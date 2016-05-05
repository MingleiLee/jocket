package com.jeedsoft.jocket.event;

import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.connection.JocketSessionManager;
import com.jeedsoft.jocket.storage.local.JocketLocalQueue;

public class JocketQueueManager
{
	private static JocketQueue queue = new JocketLocalQueue();

	public static JocketQueue getQueue()
	{
		return queue;
	}

	public static void setQueue(JocketQueue queue)
	{
		JocketQueueManager.queue = queue;
	}

	public static void start()
	{
		queue.start();
	}

	public static void stop()
	{
		queue.stop();
	}

	public static void publish(String sessionId, JocketEvent event)
	{
		JocketSession session = JocketSessionManager.get(sessionId);
		if (session != null) {
			if (event.getType() == JocketEvent.TYPE_NORMAL) {
				session.setLastMessageTime(System.currentTimeMillis());
			}
			queue.publish(sessionId, event);
		}
	}

	public static void subscribe(JocketSubscriber subscriber, String sessionId)
	{
		queue.subscribe(subscriber, sessionId);
	}

	public static void unsubscribe(String sessionId, boolean clearEvents)
	{
		queue.unsubscribe(sessionId, clearEvents);
	}

	public static int getQueueCount()
	{
		return queue.getQueueCount();
	}
	
	public static int getSubscriberCount()
	{
		return queue.getSubscriberCount();
	}
}
