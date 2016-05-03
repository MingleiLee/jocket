package com.jeedsoft.jocket.storage.redis;

import org.json.JSONObject;

import com.jeedsoft.jocket.event.JocketAbstractQueue;
import com.jeedsoft.jocket.event.JocketEvent;

public class JocketRedisQueue extends JocketAbstractQueue
{
	@Override
	public void start()
	{
		JocketRedisListener.start();
	}

	@Override
	public synchronized void stop()
	{
		subscribers.clear();
		JocketRedisListener.stop();
	}
	
	@Override
	public void publish(String connectionId, JocketEvent event)
	{
		String key = getKey(connectionId);
		JocketRedisExecutor.rpush(key, event.toJsonString());
		JSONObject publishMessage = new JSONObject().put("cid", connectionId);
		JocketRedisExecutor.publish(JocketRedisListener.channel, publishMessage.toString());
	}

	@Override
	public void unsubscribe(String connectionId, boolean isPermenant)
	{
		synchronized(subscribers) {
			subscribers.remove(connectionId);
		}
		if (isPermenant) {
			String key = getKey(connectionId);
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
	protected JocketEvent pollEvent(String connectionId)
	{
		String key = getKey(connectionId);
		String text = JocketRedisExecutor.lpop(key);
		return text == null ? null : JocketEvent.parse(text);
	}

	private String getKey(String connectionId)
	{
		return JocketRedisKey.PREFIX_QUEUE + ":" + connectionId;
	}
	
	private String getKeyPattern()
	{
		return JocketRedisKey.PREFIX_QUEUE + ":*";
	}
}
