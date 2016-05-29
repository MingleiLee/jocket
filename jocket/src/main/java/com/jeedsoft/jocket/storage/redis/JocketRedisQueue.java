package com.jeedsoft.jocket.storage.redis;

import org.json.JSONObject;

import com.jeedsoft.jocket.message.JocketAbstractQueue;
import com.jeedsoft.jocket.message.JocketPacket;

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
	public void removeSubscriber(String sessionId, boolean clearQueue)
	{
		removeSubscriber(sessionId);
		if (clearQueue) {
			String key = getKey(sessionId);
			JocketRedisExecutor.del(key);
		}
	}

	@Override
	public void publishMessage(String sessionId, JocketPacket packet)
	{
		String key = getKey(sessionId);
		JocketRedisExecutor.rpush(key, packet.toJson().toString());
		JSONObject data = new JSONObject().put("sessionId", sessionId);
		JocketRedisExecutor.publish(JocketRedisSubscriber.channel, data.toString());
	}

	@Override
	public void publishEvent(String sessionId, JocketPacket packet)
	{
		JSONObject data = new JSONObject();
		data.put("sessionId", sessionId);
		data.put("event", packet.toString());
		JocketRedisExecutor.publish(JocketRedisSubscriber.channel, data.toString());
	}

	@Override
	public int getQueueCount()
	{
		String pattern = getKeyPattern();
		return JocketRedisExecutor.keys(pattern).size(); //TODO optimize for performance
	}

	@Override
	protected JocketPacket pollMessage(String sessionId)
	{
		String key = getKey(sessionId);
		String text = JocketRedisExecutor.lpop(key);
		return text == null ? null : JocketPacket.parse(text);
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
