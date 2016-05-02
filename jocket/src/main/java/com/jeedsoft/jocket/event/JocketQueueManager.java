package com.jeedsoft.jocket.event;

import com.jeedsoft.jocket.connection.JocketStubManager;
import com.jeedsoft.jocket.event.impl.JocketStandaloneQueue;

public class JocketQueueManager
{
	private static JocketQueue queue = new JocketStandaloneQueue();

	public static void setQueue(JocketQueue queue)
	{
		JocketQueueManager.queue = queue;
	}

	public static void publish(String connectionId, JocketEvent event)
	{
		if (JocketStubManager.contains(connectionId)) {
			queue.publish(connectionId, event);
		}
	}

	public static void subscribe(JocketSubscriber subscriber, String connectionId)
	{
		queue.subscribe(subscriber, connectionId);
	}

	public static void unsubscribe(JocketSubscriber subscriber, String connectionId)
	{
		queue.unsubscribe(subscriber, connectionId);
	}

	public static void unsubscribe(String connectionId)
	{
		queue.unsubscribe(connectionId);
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
