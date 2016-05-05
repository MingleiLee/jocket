package com.jeedsoft.jocket.endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.connection.JocketCloseReason;
import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.event.JocketEvent;
import com.jeedsoft.jocket.util.JocketReflectUtil;

public class JocketEndpointRunner
{
	private static final Logger logger = LoggerFactory.getLogger(JocketEndpointRunner.class);
	
	public static void doOpen(JocketSession session)
	{
		new Thread(new OpenRunner(session)).start();
	}

	public static void doClose(JocketSession session, JocketCloseReason reason)
	{
		new Thread(new CloseRunner(session, reason)).start();
	}
	
	public static void doMessage(JocketSession session, JocketEvent event)
	{
		new Thread(new MessageRunner(session, event)).start();
	}

	private static class OpenRunner implements Runnable
	{
		private JocketSession session;
		
		public OpenRunner(JocketSession session)
		{
			this.session = session;
		}

		public void run()
		{
			Class<? extends JocketAbstractEndpoint> cls = session.getEndpointClass();
			if (logger.isTraceEnabled()) {
				Object[] args = {session.getId(), cls.getName()};
				logger.trace("[Jocket] Invoking endpoint: sid={}, method={}.onOpen", args);
			}
			try {
				JocketAbstractEndpoint endpoint = JocketReflectUtil.newInstance(cls);
				endpoint.onOpen(session);
			}
			catch (Throwable e) {
				logger.error("[Jocket] Failed to invoke " + cls.getName() + ".onOpen", e);
			}
		}
	}

	private static class CloseRunner implements Runnable
	{
		private JocketSession session;
		
		private JocketCloseReason reason;
		
		public CloseRunner(JocketSession session, JocketCloseReason reason)
		{
			this.session = session;
			this.reason = reason;
		}

		public void run()
		{
			Class<? extends JocketAbstractEndpoint> cls = session.getEndpointClass();
			if (logger.isTraceEnabled()) {
				Object[] args = {session.getId(), cls.getName(), reason};
				logger.trace("[Jocket] Invoking endpoint: sid={}, method={}.onClose, reason={}", args);
			}
			try {
				JocketAbstractEndpoint endpoint = JocketReflectUtil.newInstance(cls);
				endpoint.onClose(session, reason);
			}
			catch (Throwable e) {
				logger.error("[Jocket] Failed to invoke " + cls.getName() + ".onClose", e);
			}
		}
	}

	private static class MessageRunner implements Runnable
	{
		private JocketSession session;
		
		private JocketEvent event;
		
		public MessageRunner(JocketSession session, JocketEvent event)
		{
			this.session = session;
			this.event = event;
		}

		public void run()
		{
			Class<? extends JocketAbstractEndpoint> cls = session.getEndpointClass();
			if (logger.isTraceEnabled()) {
				Object[] args = {session.getId(), cls.getName(), event};
				logger.trace("[Jocket] Invoking endpoint: sid={}, method={}.onMessage, event={}", args);
			}
			try {
				session.setLastMessageTime(System.currentTimeMillis());
				JocketAbstractEndpoint endpoint = JocketReflectUtil.newInstance(cls);
				endpoint.onMessage(session, event.getName(), event.getData());
			}
			catch (Throwable e) {
				logger.error("[Jocket] Failed to invoke " + cls.getName() + ".onMessage", e);
			}
		}
	}
}
