package com.jeedsoft.jocket.storage.local;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import com.jeedsoft.jocket.event.JocketAbstractQueue;
import com.jeedsoft.jocket.event.JocketEvent;

public class JocketLocalQueue extends JocketAbstractQueue
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
	public void publish(String sessionId, JocketEvent event)
	{
		Queue<JocketEvent> queue;
		synchronized(queues) {
			queue = queues.get(sessionId);
			if (queue == null) {
				queue = new LinkedList<>();
				queues.put(sessionId, queue);
			}
		}
		synchronized(queue) {
			while (queue.size() >= MAX_QUEUE_SIZE) {
				queue.remove();
			}
			queue.add(event);
		}
		notifySubscriber(sessionId);
	}

	@Override
	public void unsubscribe(String sessionId, boolean isPermenant)
	{
		synchronized(subscribers) {
			subscribers.remove(sessionId);
		}
		if (isPermenant) {
			synchronized(queues) {
				queues.remove(sessionId);
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
	protected JocketEvent pollEvent(String sessionId)
	{
		Queue<JocketEvent> queue;
		synchronized(queues) {
			queue = queues.get(sessionId);
		}
		if (queue == null) {
			return null;
		}
		synchronized(queue) {
			return queue.poll();
		}
	}
}
