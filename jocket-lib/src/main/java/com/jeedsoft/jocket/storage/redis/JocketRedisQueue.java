package com.jeedsoft.jocket.storage.redis;

import com.jeedsoft.jocket.JocketService;
import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.connection.JocketSessionManager;
import com.jeedsoft.jocket.message.JocketAbstractQueue;
import com.jeedsoft.jocket.message.JocketConsumer;
import com.jeedsoft.jocket.message.JocketPacket;
import com.jeedsoft.jocket.util.JocketStringUtil;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
		JocketRedisSubscriber.stop();
	}

	@Override
	public void publish(String sessionId, JocketPacket packet)
	{
		JocketSession session = JocketSessionManager.get(sessionId);
        if (session == null) {
            logger.debug("[Jocket] Session not found: sid={}, packet={}", sessionId, packet);
            return;
        }
        if (!preparePublish(session, packet)) {
            return;
        }

        // Ensure the total queue (event + message) size not exceeds the capacity
        String eqKey = getEventQueueKey(sessionId);
        String mqKey = getMessageQueueKey(sessionId);
        long eqSize = JocketRedisExecutor.llen(eqKey);
        long mqSize = JocketRedisExecutor.llen(mqKey);
        int capacity = JocketService.getQueueCapacity();
        if (eqSize >= capacity) {
            eqSize = capacity - 1;
            JocketRedisExecutor.ltrim(eqKey, 0, eqSize - 1); // length = end - start + 1
        }
        if (eqSize + mqSize >= capacity) {
            mqSize = capacity - eqSize - 1;
            if (mqSize <= 0) {
                JocketRedisExecutor.del(mqKey);
            }
            else {
                JocketRedisExecutor.ltrim(mqKey, 0, mqSize - 1);
            }
        }
        JocketRedisExecutor.ltrim(mqKey, 0, capacity - eqSize - 1);

        // Push to redis
        boolean isMessage = packet.getType().equals(JocketPacket.TYPE_MESSAGE);
        if (isMessage) {
            ++mqSize;
        }
        else {
            ++eqSize;
        }
        JocketRedisExecutor.lpush(isMessage ? mqKey : eqKey, packet.toJson().toString());
        logger.trace("[Jocket] Publish to redis queue: sid={}, eqSize={}, mqSize={}", sessionId, eqSize, mqSize);
        JSONObject data = new JSONObject().put("sessionId", sessionId);
        JocketRedisExecutor.publish(JocketRedisSubscriber.channel, data.toString());
		JocketConsumer.notify(sessionId);
	}

	@Override
	public JocketPacket poll(String sessionId)
    {
        String eqKey = getEventQueueKey(sessionId);
        String mqKey = getMessageQueueKey(sessionId);
        String packetText = JocketRedisExecutor.rpop(eqKey);
        if (JocketStringUtil.isEmpty(packetText)) {
            packetText = JocketRedisExecutor.rpop(mqKey);
        }
        return JocketStringUtil.isEmpty(packetText) ? null : JocketPacket.parse(packetText);
	}

	@Override
	public JocketPacket peek(String sessionId)
    {
        String eqKey = getEventQueueKey(sessionId);
        String mqKey = getMessageQueueKey(sessionId);
        List<String> list = JocketRedisExecutor.lrange(eqKey, -1, -1);
        if (list.isEmpty()) {
            list = JocketRedisExecutor.lrange(mqKey, -1, -1);
        }
        return list.isEmpty() ? null : JocketPacket.parse(list.get(0));
	}

    @Override
    public void clear(String sessionId)
    {
        JocketRedisExecutor.del(getEventQueueKey(sessionId));
        JocketRedisExecutor.del(getMessageQueueKey(sessionId));
    }

	@Override
	public int getQueueCount()
	{
        //TODO Optimize for performance: use lua script to return the count directly
		Set<String> keys = JocketRedisExecutor.keys(getQueueKeyPattern());
		Set<String> sessionIds = new HashSet<>();
		int startIndex = JocketRedisKey.PREFIX_SESSION.length() + 1;
		for (String key: keys) {
		    int endIndex = key.indexOf(':', startIndex);
		    sessionIds.add(key.substring(startIndex, endIndex));
        }
		return sessionIds.size();
	}

	private String getEventQueueKey(String sessionId)
	{
		return JocketRedisKey.PREFIX_SESSION + ":" + sessionId + ":" + JocketRedisKey.POSTFIX_QUEUE_EVENT;
	}

    private String getMessageQueueKey(String sessionId)
    {
        return JocketRedisKey.PREFIX_SESSION + ":" + sessionId + ":" + JocketRedisKey.POSTFIX_QUEUE_MESSAGE;
    }

    private String getQueueKeyPattern()
	{
		return JocketRedisKey.PREFIX_SESSION + ":*:" + JocketRedisKey.POSTFIX_QUEUE + ":*";
	}
}
