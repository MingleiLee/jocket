package com.jeedsoft.jocket.storage.local;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import com.jeedsoft.jocket.JocketService;
import com.jeedsoft.jocket.message.JocketAbstractQueue;
import com.jeedsoft.jocket.message.JocketPacket;

public class JocketLocalQueue extends JocketAbstractQueue
{
	private final Map<String, Queue<JocketPacket>> queues = new HashMap<>();

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
	public void removeSubscriber(String sessionId, boolean clearQueue)
	{
		removeSubscriber(sessionId);
		if (clearQueue) {
			synchronized(queues) {
				queues.remove(sessionId);
			}
		}
	}

	@Override
	public void publishMessage(String sessionId, JocketPacket packet)
	{
		Queue<JocketPacket> queue;
		synchronized(queues) {
			queue = queues.get(sessionId);
			if (queue == null) {
				queue = new LinkedList<>();
				queues.put(sessionId, queue);
			}
		}
		synchronized(queue) {
			int capacity = JocketService.getQueueCapacity();
			while (queue.size() >= capacity) {
				queue.remove();
			}
			queue.add(packet);
		}
		notifyNewMessage(sessionId);
	}

	@Override
	public void publishEvent(String sessionId, JocketPacket packet)
	{
		notifyNewEvent(sessionId, packet);
	}

	@Override
	public int getQueueCount()
	{
		synchronized(queues) {
			return queues.size();
		}
	}

	@Override
	protected JocketPacket pollMessage(String sessionId)
	{
		Queue<JocketPacket> queue;
		synchronized(queues) {
			queue = queues.get(sessionId);
			return queue == null ? null : queue.poll();
		}
	}
}
