package com.jeedsoft.jocket.message;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.connection.JocketConnection;
import com.jeedsoft.jocket.connection.JocketConnectionManager;

public abstract class JocketAbstractQueue implements JocketQueue
{
	private static final Logger logger = LoggerFactory.getLogger(JocketAbstractQueue.class);
	
	protected final Map<String, JocketConnection> subscribers = new HashMap<>();
	
	protected abstract JocketPacket pollMessage(String sessionId);

	@Override
	public void addSubscriber(JocketConnection cn)
	{
		String sessionId = cn.getSessionId();
		synchronized(subscribers) {
			subscribers.put(sessionId, cn);
		}
		notifyNewMessage(sessionId);
	}

	protected JocketConnection getSubscriber(String sessionId)
	{
		synchronized(subscribers) {
			return subscribers.get(sessionId);
		}
	}

	@Override
	public int getSubscriberCount()
	{
		synchronized(subscribers) {
			return subscribers.size();
		}
	}

	public void notifyNewMessage(String sessionId)
	{
		JocketConnection cn = getSubscriber(sessionId);
		if (cn != null) {
			new Thread(new MessageConsumer(this, sessionId, cn)).start();
		}
	}

	public void notifyNewEvent(String sessionId, JocketPacket event)
	{
		JocketConnection cn = JocketConnectionManager.get(sessionId);
		if (cn != null) {
			new Thread(new EventConsumer(cn, event)).start();
		}
	}

	protected static class MessageConsumer implements Runnable
	{
		private JocketAbstractQueue queue;
		
		private String sessionId;
		
		private JocketConnection cn;
		
		public MessageConsumer(JocketAbstractQueue queue, String sessionId, JocketConnection cn)
		{
			this.queue = queue;
			this.sessionId = sessionId;
			this.cn = cn;
		}

		@Override
		public void run()
		{
			synchronized(cn) {
				if (cn.isActive()) {
					JocketPacket event = queue.pollMessage(sessionId);
					if (event != null) {
						try {
							cn.downstream(event);
						}
						catch (Throwable e) {
							logger.error("[Jocket] Failed to send message: sid={}, event={}", cn.getSessionId(), event);
						}
						if (cn.isLongTime()) {
							queue.notifyNewMessage(sessionId);
						}
					}
				}
			}
		}
	}

	protected static class EventConsumer implements Runnable
	{
		private JocketConnection cn;
		
		private JocketPacket event;

		public EventConsumer(JocketConnection cn, JocketPacket event)
		{
			this.cn = cn;
			this.event = event;
		}

		@Override
		public void run()
		{
			synchronized(cn) {
				if (cn.isActive()) {
					cn.onEvent(event);
				}
			}
		}
	}
}
