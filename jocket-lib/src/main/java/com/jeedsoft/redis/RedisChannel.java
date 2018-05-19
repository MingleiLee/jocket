package com.jeedsoft.redis;

import java.util.UUID;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

public class RedisChannel
{
    private static final Logger logger = LoggerFactory.getLogger(RedisChannel.class);

    private static final String KEY_SENDER = "sender";
    private static final String KEY_DATA = "data";
    private static final String KEY_DISCARD_OWN_MESSAGE = "discardOwnMessage";
    private static final String KEY_HANDSHAKE = "handshake";
    
    private final String id = UUID.randomUUID().toString();

    private String name;

    private RedisDataSource ds;
    
    private RedisChannelCallback callback;
    
    private PubSub pubSub;

    private boolean started = false;

    private boolean connected = false;
    
    private int serial = 0; // When reconnect, serial++
    
    private long lastActiveTime = 0; // The last time when message received

    private boolean isHandshaking = false;

    RedisChannel(String name)
    {
        this.name = name;
    }

    public void setDataSource(RedisDataSource ds)
    {
        this.ds = ds;
    }

    public void setCallback(RedisChannelCallback callback)
    {
        this.callback = callback;
    }
    
    public synchronized void start()
    {
        cleanup();
        started = true;
        connect();
    }
    
    public synchronized void stop()
    {
        cleanup();
        started = false;
        setConnected(false, null);
    }

    public void publish(JSONObject message)
    {
        publish(message, false);
    }

    public void publish(JSONObject message, boolean discardOwnMessage)
    {
        JSONObject packet = new JSONObject();
        packet.put(KEY_SENDER, id);
        packet.put(KEY_DATA, message);
        packet.put(KEY_DISCARD_OWN_MESSAGE, discardOwnMessage);
        publishPacket(packet);
    }
    
    private void publishPacket(JSONObject packet)
    {
        logger.trace("Publish to Redis: channel={}, packet={}", name, packet);
        try (Jedis jedis = ds.getJedis()) {
            jedis.publish(name, packet.toString());
        }
    }
    
    private synchronized void cleanup()
    {
        isHandshaking = false;
        if (pubSub != null) {
            try {
                pubSub.close();
            }
            catch (Throwable e) {
                logger.error("Failed to close Redis PubSub", e);
            }
            finally {
                pubSub = null;
            }
        }
    }
    
    private synchronized void setConnected(boolean connected, Throwable e)
    {
        if (connected != this.connected) {
            this.connected = connected;
            if (connected) {
                callback.onConnect();
            }
            else {
                callback.onDisconnect(e);
            }
        }
    }
    
    private synchronized void connect()
    {
        logger.debug("Redis subscribe: channel={}", name);
        cleanup();
        ++serial;
        pubSub = new PubSub();
        new Thread() {
            public void run() {
                try (Jedis jedis = ds.getJedis()) {
                    setName("RedisSubscriber-" + name + "-" + serial);
                    logger.debug("Redis subscribe thread start. channel={}", name);
                    jedis.subscribe(pubSub, name);
                    logger.debug("Redis subscribe thread stop. channel={}", name);
                    cleanup();
                    setConnected(false, null);
                }
                catch (Throwable e) {
                    logger.error("Redis subscribe thread error", e);
                    cleanup();
                    setConnected(false, e);
                }
            }
        }.start();
    }

    synchronized void monitor()
    {
        try {
            String msg = "Run monitor: channel={}, started={}, pubSub={}, isHandshaking={}, lastActiveTime={}";
            logger.trace(msg, name, started, pubSub, isHandshaking, lastActiveTime);
            if (!started) {
                return;
            }
            else if (pubSub == null) {
                connect();
            }
            else {
                long now = System.currentTimeMillis();
                final long maxIdleTime = 5000;
                if (!isHandshaking && now - lastActiveTime >= maxIdleTime) {
                    logger.trace("Publish handshake message to Redis server.");
                    isHandshaking = true;
                    publishPacket(new JSONObject().put(KEY_HANDSHAKE, "1"));
                }
                else if (isHandshaking) {
                    logger.debug("Redis subscriber handshake timeout, reconnect.");
                    isHandshaking = false;
                    connect();
                }
            }
        }
        catch (Throwable e) {
            logger.error("Failed to check Redis subscriber status", e);
        }
    }

    private class PubSub extends JedisPubSub
    {
        public void close()
        {
            if (isSubscribed()) {
                try {
                    unsubscribe();
                }
                catch (Throwable e) {
                    logger.error("Failed to close subscriber", e);
                }
            }
        }

        @Override
        public void onMessage(String channel, String message)
        {
            try {
                logger.trace("Received from Redis: channel={}, packet={}", channel, message);
                lastActiveTime = System.currentTimeMillis();
                isHandshaking = false;
                JSONObject packet = new JSONObject(message);
                if (!packet.has(KEY_HANDSHAKE)) {
                    String sender = packet.getString(KEY_SENDER);
                    boolean discardOwnMessage = packet.getBoolean(KEY_DISCARD_OWN_MESSAGE);
                    if (!discardOwnMessage || !sender.equals(id)) {
                        JSONObject data = packet.getJSONObject(KEY_DATA);
                        callback.onMessage(data);
                    }
                }
            }
            catch (Throwable e) {
                logger.error("Failed to handle message:", e);
            }
        }

        @Override
        public void onSubscribe(String channel, int subscribedChannels)
        {
            logger.debug("Redis subscribed. channel={}, subscribedChannels={}", channel, subscribedChannels);
            lastActiveTime = System.currentTimeMillis();
            isHandshaking = false;
            setConnected(true, null);
        }

        @Override
        public void onUnsubscribe(String channel, int subscribedChannels)
        {
            logger.debug("Redis unsubscribed. channel={}, subscribedChannels={}", channel, subscribedChannels);
            setConnected(false, null);
        }
    }
}
