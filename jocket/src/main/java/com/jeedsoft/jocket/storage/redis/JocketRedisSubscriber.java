package com.jeedsoft.jocket.storage.redis;

import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.JocketService;
import com.jeedsoft.jocket.event.JocketQueueManager;

import redis.clients.jedis.JedisPubSub;

public class JocketRedisSubscriber
{
	private static final Logger logger = LoggerFactory.getLogger(JocketRedisSubscriber.class);
	
    public static final String channel = "JocketEventNotification";
	
    private static final long retryInterval = 5000;

    private static Subscriber subscriber = null;

	public synchronized static void start()
	{
		stop();
		subscriber = new Subscriber();
		new SubscriptionThread().start();
	}

	public synchronized static void stop()
	{
		try {
			if (subscriber != null) {
				subscriber.unsubscribe();
				subscriber = null;
			}
		}
		catch (Throwable e) {
			logger.error("[Jocket] Failed to stop current subscriber", e);
		}
	}

	private static class SubscriptionThread extends Thread
	{
	    public SubscriptionThread()
	    {
	    	super("JocketRedisSubscriber");
	    }
	    
	    @Override
	    public void run()
	    {
	        try {
	        	logger.debug("[Jocket] Redis subscription thread start. channel={}", channel);
		        JocketRedisExecutor.subscribe(subscriber, channel);
	        	logger.debug("[Jocket] Redis subscription thread stop. channel={}", channel);
	        }
	        catch (Throwable e) {
	        	logger.error("[Jocket] Redis subscription thread error", e);
	        	if (JocketService.isRunning()) {
	        		new Timer().schedule(new TimerTask() {
						public void run() {
			        		start();
						}
					}, retryInterval);
	        	}
	        }
	    }
	}

	private static class Subscriber extends JedisPubSub
	{
		@Override
	    public void onMessage(String channel, String message)
	    {
			logger.trace("[Jocket] Message received from Redis: {}", message);
			JSONObject json = new JSONObject(message);
			String sessionId = json.getString("sessionId");
			JocketRedisQueue queue = (JocketRedisQueue)JocketQueueManager.getQueue();
			queue.notifySubscriber(sessionId);
	    }
	}
}
