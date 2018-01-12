package com.jeedsoft.jocket.storage.redis;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.JocketService;
import com.jeedsoft.jocket.message.JocketAbstractQueue;
import com.jeedsoft.jocket.message.JocketPacket;

public class JocketRedisQueue extends JocketAbstractQueue
{
	private static final Logger logger = LoggerFactory.getLogger(JocketRedisQueue.class);

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
		JocketRedisExecutor.ltrim(key, 0, JocketService.getQueueCapacity() - 2);
		long size = JocketRedisExecutor.lpush(key, packet.toJson().toString());
		logger.trace("[Jocket] Message queue: sid={}, size={}", sessionId, size);
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
		String text = JocketRedisExecutor.rpop(key);
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
