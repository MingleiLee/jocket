package com.jeedsoft.jocket.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.connection.JocketConnection;
import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.connection.JocketSessionManager;
import com.jeedsoft.jocket.storage.local.JocketLocalQueue;

public class JocketQueueManager
{
	private static final Logger logger = LoggerFactory.getLogger(JocketQueueManager.class);

	private static JocketQueue queue = new JocketLocalQueue();

	public static JocketQueue getQueue()
	{
		return queue;
	}

	public static void setQueue(JocketQueue queue)
	{
		JocketQueueManager.queue = queue;
		logger.debug("[Jocket] Queue class: {}", queue.getClass().getName());
	}

	public static void start()
	{
		queue.start();
	}

	public static void stop()
	{
		queue.stop();
	}

	public static void addSubscriber(JocketConnection cn)
	{
		queue.addSubscriber(cn);
	}

	public static void removeSubscriber(String sessionId, boolean clearQueue)
	{
		queue.removeSubscriber(sessionId, clearQueue);
	}

	public static void publishMessage(String sessionId, JocketPacket message)
	{
		JocketSession session = JocketSessionManager.get(sessionId);
		publishMessage(session, message);
	}

	public static void publishMessage(JocketSession session, JocketPacket message)
	{
		if (session != null && session.isOpen()) {
			if (JocketPacket.TYPE_MESSAGE.equals(message.getType())) {
				session.setLastMessageTime(System.currentTimeMillis());
			}
			queue.publishMessage(session.getId(), message);
		}
	}

	public static void publishEvent(String sessionId, JocketPacket packet)
	{
		queue.publishEvent(sessionId, packet);
	}

	public static int getSubscriberCount()
	{
		return queue.getSubscriberCount();
	}

	public static int getQueueCount()
	{
		return queue.getQueueCount();
	}
}
