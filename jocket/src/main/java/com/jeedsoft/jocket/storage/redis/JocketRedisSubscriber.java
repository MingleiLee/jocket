package com.jeedsoft.jocket.storage.redis;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.message.JocketPacket;
import com.jeedsoft.jocket.message.JocketQueueManager;

import redis.clients.jedis.JedisPubSub;

public class JocketRedisSubscriber
{
	private static final Logger logger = LoggerFactory.getLogger(JocketRedisSubscriber.class);
	
    public static final String channel = "JocketEventNotification";

    private static Subscriber currentSubscriber = null;

    private static long reconnectCount = 0;

    private static boolean started = false;
 
    private static Timer reconnectTimer = null;
    
	public synchronized static void start()
	{
		stop();
		currentSubscriber = new Subscriber();
		started = true;
	}

	public synchronized static void stop()
	{
		started = false;
		if (currentSubscriber != null) {
			currentSubscriber.close(false);
			currentSubscriber = null;
		}
		if (reconnectTimer != null) {
			reconnectTimer.cancel();
			reconnectTimer = null;
		}
	}
	
	private synchronized static void onSubscriberClose(Subscriber subscriber)
	{
		if (!started || subscriber != currentSubscriber) {
			return;
		}
		currentSubscriber = null;
	    final long maxReconnectDelay = 10_000;
		long delay = Math.min(reconnectCount * 1000, maxReconnectDelay);
		++reconnectCount;
		logger.debug("[Jocket] Redis subscribe thread restart: count={}", reconnectCount);
		reconnectTimer = new Timer();
		reconnectTimer.schedule(new TimerTask() {
			public void run() {
				synchronized (JocketRedisSubscriber.class) { //synchronized is required here
					if (started && currentSubscriber == null) {
						currentSubscriber = new Subscriber();
					}
				}
			}
		}, delay);
	}

	private static class Subscriber extends JedisPubSub
	{
		private static final AtomicLong count = new AtomicLong();
		
		private static final long checkInterval = 1000;

		private long serial;
		
		private Timer checkTimer;
		  
	    private long lastMessageTime = 0;
	    
	    private long handshakeTime = 0;

	    private boolean closed = false;

		public Subscriber()
		{
			serial = count.incrementAndGet();
			new SubscribeThread(this).start();
		}

		public synchronized void close(boolean fireEvent)
		{
			if (closed) {
				return;
			}
			closed = true;
			if (checkTimer != null) {
				checkTimer.cancel();
				checkTimer = null;
			}
			if (isSubscribed()) {
				try {
					unsubscribe();
				}
				catch (Throwable e) {
					logger.error("[Jocket] Failed to close subscriber " + serial, e);
				}
			}
			if (fireEvent) {
				new CloseEventThread(this).start();
			}
		}

		@Override
	    public void onMessage(String channel, String message)
	    {
			try {
				logger.trace("[Jocket] Message received from Redis: {}", message);
				lastMessageTime = System.currentTimeMillis();
				handshakeTime = 0;
				JSONObject json = new JSONObject(message);
				if (!json.has("sessionId")) {
					return;
				}
				String sessionId = json.getString("sessionId");
				JocketPacket event = JocketPacket.parse(json.optString("event", null));
				JocketRedisQueue queue = (JocketRedisQueue)JocketQueueManager.getQueue();
				if (event == null) {
					queue.notifyNewMessage(sessionId);
				}
				else {
					queue.notifyNewEvent(sessionId, event);
				}
			}
			catch (Throwable e) {
				logger.error("[Jocket] Failed to handle message:", e);
			}
	    }

		@Override
		public void onSubscribe(String channel, int subscribedChannels)
		{
			logger.debug("[Jocket] Redis subscriber start. channel={}, subscribed={}", channel, subscribedChannels);
			lastMessageTime = System.currentTimeMillis();
			handshakeTime = 0;
			reconnectCount = 0;
			checkTimer = new Timer();
			checkTimer.schedule(new CheckTask(), checkInterval);
		}

		@Override
		public void onUnsubscribe(String channel, int subscribedChannels)
		{
			logger.debug("[Jocket] Redis subscriber stop. channel={}, subscribed={}", channel, subscribedChannels);
			close(true);
		}
		
		private class CheckTask extends TimerTask
		{
			public void run()
			{
				synchronized (Subscriber.this) {
					if (closed) {
						return;
					}
					final long silentTimeout = 5000;
					final long handshakeTimeout = 5000;
					try {
						long now = System.currentTimeMillis();
						if (handshakeTime == 0 && now - lastMessageTime >= silentTimeout) {
							handshakeTime = now;
							logger.debug("[Jocket] Publish handshake message to Redis server.");
							JocketRedisExecutor.publish(channel, new JSONObject().put("type", "handshake").toString());
						}
						else if (handshakeTime > 0 && now - handshakeTime >= handshakeTimeout) {
							handshakeTime = 0;
							logger.debug("[Jocket] Redis subscriber handshake timeout.");
							close(true);
						}
					}
					catch (Throwable e) {
						logger.error("[Jocket] Failed to check Redis subscriber status", e);
					}
					finally {
						checkTimer.schedule(new CheckTask(), checkInterval);
					}
				}
			}
		}
	}

	private static class SubscribeThread extends Thread
	{
		private Subscriber subscriber;
		
	    public SubscribeThread(Subscriber subscriber)
	    {
	    	super("JocketRedisSubscriber-" + subscriber.serial);
	    	this.subscriber = subscriber;
	    }
	    
	    @Override
	    public void run()
	    {
	        try {
	        	logger.debug("[Jocket] Redis subscribe thread start. channel={}", channel);
		        JocketRedisExecutor.subscribe(subscriber, channel);
	        	logger.debug("[Jocket] Redis subscribe thread stop. channel={}", channel);
	        	subscriber.close(true);
	        }
	        catch (Throwable e) {
	        	logger.error("[Jocket] Redis subscribe thread error", e);
	        	subscriber.close(true);
	        }
	    }
	}

	private static class CloseEventThread extends Thread
	{
		private Subscriber subscriber;
		
	    public CloseEventThread(Subscriber subscriber)
	    {
	    	super("JocketRedisSubscriberClose-" + subscriber.serial);
	    	this.subscriber = subscriber;
	    }
	    
	    @Override
	    public void run()
	    {
	    	onSubscriberClose(subscriber);
	    }
	}
}
