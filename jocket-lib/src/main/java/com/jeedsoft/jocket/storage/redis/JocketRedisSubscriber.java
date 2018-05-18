package com.jeedsoft.jocket.storage.redis;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.connection.JocketConnectionManager;
import com.jeedsoft.jocket.message.JocketConsumer;
import com.jeedsoft.redis.RedisChannel;
import com.jeedsoft.redis.RedisChannel.Callback;
import com.jeedsoft.redis.RedisChannelManager;

public class JocketRedisSubscriber
{
	private static final Logger logger = LoggerFactory.getLogger(JocketRedisSubscriber.class);
	
	private static String clusterId = null;
	        
	private static RedisChannel channel = null;
    
	public synchronized static void start()
	{
	    if (channel == null) {
	        String name = "Jocket";
	        if (clusterId != null) {
	            name += "-" + clusterId;
	        }
	        channel = RedisChannelManager.create(name);
	        channel.setDataSource(JocketRedisManager.getDataSource());
            channel.setCallback(new MessageCallback());
	        channel.start();
	    }
	}

	public synchronized static void stop()
	{
	    if (channel != null) {
	        channel.stop();
	    }
	}

	public static void broadcastUpgrade(String sessionId)
	{
	    JSONObject json = new JSONObject();
        json.put("sessionId", sessionId);
        json.put("isUpgrade", true);
        channel.publish(json);
	}

    public static void publish(JSONObject message)
    {
        channel.publish(message);
    }

    static void setClusterId(String clusterId)
    {
        JocketRedisSubscriber.clusterId = clusterId;
    }
    
	private static class MessageCallback implements Callback
	{
	    @Override
        public void onMessage(JSONObject message)
	    {
            try {
                logger.trace("[Jocket] Message received from Redis: {}", message);
                JSONObject json = new JSONObject(message);
                if (!json.has("sessionId")) {
                    return;
                }
                String sessionId = json.getString("sessionId");
                boolean isUpgrade = json.optBoolean("isUpgrade");
                if (isUpgrade) {
                    JocketConnectionManager.upgrade(sessionId);
                }
                else {
                    JocketConsumer.notify(sessionId);
                }
            }
            catch (Throwable e) {
                logger.error("[Jocket] Failed to handle message:", e);
            }
        }
    }
}
