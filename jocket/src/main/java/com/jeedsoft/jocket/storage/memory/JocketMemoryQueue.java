package com.jeedsoft.jocket.storage.memory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import com.jeedsoft.jocket.event.JocketAbstractQueue;
import com.jeedsoft.jocket.event.JocketEvent;

public class JocketMemoryQueue extends JocketAbstractQueue
{
	private static final int MAX_QUEUE_SIZE = 1000;
	
	private final Map<String, Queue<JocketEvent>> queues = new HashMap<>();

	@Override
	public void start()
	{
	}

	@Override
	public void stop()
	{
		synchronized(queues) {
			queues.clear();
		}
		synchronized(subscribers) {
			subscribers.clear();
		}
	}

	@Override
	public void publish(String connectionId, JocketEvent event)
	{
		Queue<JocketEvent> queue;
		synchronized(queues) {
			queue = queues.get(connectionId);
			if (queue == null) {
				queue = new LinkedList<>();
				queues.put(connectionId, queue);
			}
		}
		synchronized(queue) {
			while (queue.size() >= MAX_QUEUE_SIZE) {
				queue.remove();
			}
			queue.add(event);
		}
		notifySubscriber(connectionId);
	}

	@Override
	public void unsubscribe(String connectionId, boolean isPermenant)
	{
		synchronized(subscribers) {
			subscribers.remove(connectionId);
		}
		if (isPermenant) {
			synchronized(queues) {
				queues.remove(connectionId);
			}
		}
	}

	@Override
	public int getQueueCount()
	{
		synchronized(queues) {
			return queues.size();
		}
	}

	@Override
	protected JocketEvent pollEvent(String connectionId)
	{
		Queue<JocketEvent> queue;
		synchronized(queues) {
			queue = queues.get(connectionId);
		}
		if (queue == null) {
			return null;
		}
		synchronized(queue) {
			return queue.poll();
		}
	}
}
