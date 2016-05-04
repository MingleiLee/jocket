package com.jeedsoft.jocket.event;

import com.jeedsoft.jocket.connection.JocketStub;
import com.jeedsoft.jocket.connection.JocketStubManager;
import com.jeedsoft.jocket.storage.memory.JocketMemoryQueue;

public class JocketQueueManager
{
	private static JocketQueue queue = new JocketMemoryQueue();

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

	public static void publish(String connectionId, JocketEvent event)
	{
		JocketStub stub = JocketStubManager.get(connectionId);
		if (stub != null) {
			if (event.getType() == JocketEvent.TYPE_NORMAL) {
				stub.setLastMessageTime(System.currentTimeMillis());
			}
			queue.publish(connectionId, event);
		}
	}

	public static void subscribe(JocketSubscriber subscriber, String connectionId)
	{
		queue.subscribe(subscriber, connectionId);
	}

	public static void unsubscribe(String connectionId, boolean clearEvents)
	{
		queue.unsubscribe(connectionId, clearEvents);
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
