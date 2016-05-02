package com.jeedsoft.jocket.event.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import com.jeedsoft.jocket.event.JocketEvent;
import com.jeedsoft.jocket.event.JocketQueue;
import com.jeedsoft.jocket.event.JocketSubscriber;

public class JocketStandaloneQueue implements JocketQueue
{
	private static final int MAX_QUEUE_SIZE = 1000;
	
	private final Map<String, Queue<JocketEvent>> queues = new HashMap<>();

	private final Map<String, JocketSubscriber> subscribers = new HashMap<>();
	
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
		consume(connectionId);
	}

	@Override
	public void subscribe(JocketSubscriber subscriber, String connectionId)
	{
		synchronized(subscribers) {
			subscribers.put(connectionId, subscriber);
		}
		consume(connectionId);
	}

	@Override
	public void unsubscribe(JocketSubscriber subscriber, String connectionId)
	{
		synchronized(subscribers) {
			if (subscribers.get(connectionId) == subscriber) {
				subscribers.remove(connectionId);
			}
		}
	}

	@Override
	public void unsubscribe(String connectionId)
	{
		synchronized(subscribers) {
			subscribers.remove(connectionId);
		}
		synchronized(queues) {
			queues.remove(connectionId);
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
	public int getSubscriberCount()
	{
		synchronized(subscribers) {
			return subscribers.size();
		}
	}

	private void consume(String connectionId)
	{
		new Thread(new Consumer(this, connectionId)).start();
	}
	
	private JocketSubscriber getSubscriber(String connectionId)
	{
		synchronized(subscribers) {
			return subscribers.get(connectionId);
		}
	}
	
	private Queue<JocketEvent> getQueue(String connectionId)
	{
		synchronized(queues) {
			return queues.get(connectionId);
		}
	}
	
	private JocketEvent pollEvent(Queue<JocketEvent> queue)
	{
		synchronized(queue) {
			return queue.poll();
		}
	}
	
	private static class Consumer implements Runnable
	{
		private JocketStandaloneQueue impl;
		
		private String connectionId;
		
		public Consumer(JocketStandaloneQueue impl, String connectionId)
		{
			this.impl = impl;
			this.connectionId = connectionId;
		}

		@Override
		public void run()
		{
			JocketSubscriber subscriber = impl.getSubscriber(connectionId);
			if (subscriber != null) {
				synchronized(subscriber) {
					Queue<JocketEvent> queue = impl.getQueue(connectionId);
					JocketEvent event = queue == null ? null : impl.pollEvent(queue);
					if (event != null) {
						subscriber.onEvent(connectionId, event);
					}
				}
			}
		}
	}
}
