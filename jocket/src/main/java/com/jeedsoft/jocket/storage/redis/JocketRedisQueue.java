package com.jeedsoft.jocket.storage.redis;

import org.json.JSONObject;

import com.jeedsoft.jocket.event.JocketAbstractQueue;
import com.jeedsoft.jocket.event.JocketEvent;

public class JocketRedisQueue extends JocketAbstractQueue
{
	@Override
	public void start()
	{
		JocketRedisSubscriber.start();
	}

	@Override
	public synchronized void stop()
	{
		subscribers.clear();
		JocketRedisSubscriber.stop();
	}
	
	@Override
	public void publish(String sessionId, JocketEvent event)
	{
		String key = getKey(sessionId);
		JocketRedisExecutor.rpush(key, event.toJsonString());
		JSONObject publishMessage = new JSONObject().put("sessionId", sessionId);
		JocketRedisExecutor.publish(JocketRedisSubscriber.channel, publishMessage.toString());
	}

	@Override
	public void unsubscribe(String sessionId, boolean isPermenant)
	{
		synchronized(subscribers) {
			subscribers.remove(sessionId);
		}
		if (isPermenant) {
			String key = getKey(sessionId);
			JocketRedisExecutor.del(key);
		}
	}

	@Override
	public int getQueueCount()
	{
		String pattern = getKeyPattern();
		return JocketRedisExecutor.keys(pattern).size(); //TODO optimize for performance
	}

	@Override
	protected JocketEvent pollEvent(String sessionId)
	{
		String key = getKey(sessionId);
		String text = JocketRedisExecutor.lpop(key);
		return text == null ? null : JocketEvent.parse(text);
	}

	private String getKey(String sessionId)
	{
		return JocketRedisKey.PREFIX_SESSION + ":" + sessionId + ":" + JocketRedisKey.POSTFIX_QUEUE;
	}
	
	private String getKeyPattern()
	{
		return JocketRedisKey.PREFIX_SESSION + ":*:" + JocketRedisKey.POSTFIX_QUEUE;
	}
}
