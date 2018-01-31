package com.jeedsoft.jocket.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static void publish(String sessionId, JocketPacket packet)
    {
        queue.publish(sessionId, packet);
    }

    public static JocketPacket poll(String sessionId)
    {
        return queue.poll(sessionId);
    }

    public static JocketPacket peek(String sessionId)
    {
        return queue.peek(sessionId);
    }

    public static void clear(String sessionId)
    {
        queue.clear(sessionId);
    }

	public static int getQueueCount()
	{
		return queue.getQueueCount();
	}
}
