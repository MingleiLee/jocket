package com.jeedsoft.jocket.message;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.connection.JocketConnection;
import com.jeedsoft.jocket.connection.JocketConnectionManager;
import com.jeedsoft.jocket.transport.websocket.JocketWebSocketConnection;

public abstract class JocketAbstractQueue implements JocketQueue
{
	private static final Logger logger = LoggerFactory.getLogger(JocketAbstractQueue.class);
	
	protected final Map<String, JocketConnection> subscribers = new HashMap<>();
	
	protected abstract JocketPacket pollMessage(String sessionId);

	@Override
	public void addSubscriber(JocketConnection cn)
	{
		if (logger.isTraceEnabled()) {
			logger.trace("[Jocket] Add subscriber: sid={}, cn={}", cn.getSessionId(), cn.getClass().getName());
		}
		String sessionId = cn.getSessionId();
		synchronized(subscribers) {
			subscribers.put(sessionId, cn);
		}
		notifyNewMessage(sessionId);
	}

	protected void removeSubscriber(String sessionId)
	{
		synchronized(subscribers) {
			JocketConnection cn = subscribers.remove(sessionId);
			if (logger.isTraceEnabled()) {
				String className = cn == null ? "NULL" : cn.getClass().getName();
				logger.trace("[Jocket] Remove subscriber: sid={}, cn={}", sessionId, className);
			}
		}
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
		else {
			logger.debug("[Jocket] No connection found currently: sid={}", sessionId);
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
					JocketPacket packet = queue.pollMessage(sessionId);
					if (packet != null) {
						try {
							cn.downstream(packet);
						}
						catch (Throwable e) {
							logger.error("[Jocket] Failed to send message: sid=" + sessionId + ", packet=" + packet, e);
						}
						if (cn instanceof JocketWebSocketConnection) {
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
				else {
					logger.debug("[Jocket] Connection is not active. sid={}", cn.getSession());
				}
			}
		}
	}
}
