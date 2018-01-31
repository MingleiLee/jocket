package com.jeedsoft.jocket.storage.local;

import com.jeedsoft.jocket.JocketService;
import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.connection.JocketSessionManager;
import com.jeedsoft.jocket.message.JocketAbstractQueue;
import com.jeedsoft.jocket.message.JocketConsumer;
import com.jeedsoft.jocket.message.JocketPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class JocketLocalQueue extends JocketAbstractQueue
{
    private static final Logger logger = LoggerFactory.getLogger(JocketLocalQueue.class);

    private final Map<String, PacketQueue> queues = new HashMap<>();

	@Override
	public void start()
	{
	}

	@Override
	public void stop()
	{
		synchronized (queues) {
			queues.clear();
		}
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

        PacketQueue queue;
        synchronized (queues) {
            queue = queues.get(sessionId);
            if (queue == null) {
                queue = new PacketQueue();
                queues.put(sessionId, queue);
            }
        }
        long sizes = queue.add(packet);
        long eqSize = sizes >> 32;
        long mqSize = sizes & 0xFFFFFFFFL;
        logger.trace("[Jocket] Publish to local queue: sid={}, eqSize={}, mqSize={}", sessionId, eqSize, mqSize);
        JocketConsumer.notify(sessionId);
    }

    @Override
    public JocketPacket poll(String sessionId)
    {
        PacketQueue queue;
        synchronized (queues) {
            queue = queues.get(sessionId);
        }
        return queue == null ? null : queue.poll();
    }

    @Override
    public JocketPacket peek(String sessionId)
    {
        PacketQueue queue;
        synchronized(queues) {
            queue = queues.get(sessionId);
        }
        return queue == null ? null : queue.peek();
    }

    @Override
    public void clear(String sessionId)
    {
        synchronized(queues) {
            queues.remove(sessionId);
        }
    }

    @Override
	public int getQueueCount()
	{
		synchronized (queues) {
			return queues.size();
		}
	}

    /**
     * A PacketQueue contains a message queue and a event queue. event queue takes precedence.
     */
	private static class PacketQueue
    {
        private Queue<JocketPacket> messageQueue = new LinkedList<>();

        private Queue<JocketPacket> eventQueue = new LinkedList<>();

        private synchronized long add(JocketPacket packet)
        {
            int capacity = JocketService.getQueueCapacity();
            while (eventQueue.size() >= capacity) {
                eventQueue.remove();
            }
            while (eventQueue.size() + messageQueue.size() >= capacity) {
                messageQueue.remove();
            }
            boolean isMessage = packet.getType().equals(JocketPacket.TYPE_MESSAGE);
            Queue<JocketPacket> queue = isMessage ? messageQueue : eventQueue;
            queue.add(packet);
            return ((long)eventQueue.size() << 32) | messageQueue.size();
        }

        public synchronized JocketPacket poll()
        {
            if (!eventQueue.isEmpty()) {
                return eventQueue.poll();
            }
            if (!messageQueue.isEmpty()) {
                return messageQueue.poll();
            }
            return null;
        }

        public synchronized JocketPacket peek()
        {
            JocketPacket packet = eventQueue.peek();
            return packet == null ? messageQueue.peek() : packet;
        }
    }
}
