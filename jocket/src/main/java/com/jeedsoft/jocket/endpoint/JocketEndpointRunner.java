package com.jeedsoft.jocket.endpoint;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.connection.JocketCloseReason;
import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.message.JocketPacket;
import com.jeedsoft.jocket.util.JocketClock;
import com.jeedsoft.jocket.util.JocketReflection;

public class JocketEndpointRunner
{
	private static final Logger logger = LoggerFactory.getLogger(JocketEndpointRunner.class);
	
	public static void doOpen(JocketSession session, HttpSession httpSession)
	{
		new Thread(new OpenRunner(session, httpSession)).start();
	}

	public static void doClose(JocketSession session, JocketCloseReason reason)
	{
		new Thread(new CloseRunner(session, reason)).start();
	}
	
	public static void doMessage(JocketSession session, JocketPacket message)
	{
		new Thread(new MessageRunner(session, message)).start();
	}

	private static class OpenRunner implements Runnable
	{
		private JocketSession session;
		
		private HttpSession httpSession;
		
		public OpenRunner(JocketSession session, HttpSession httpSession)
		{
			this.session = session;
			this.httpSession = httpSession;
		}

		public void run()
		{
			Class<? extends JocketEndpoint> cls = session.getEndpointClass();
			if (logger.isTraceEnabled()) {
				Object[] args = {session.getId(), cls.getName()};
				logger.trace("[Jocket] Invoking endpoint: sid={}, method={}.onOpen", args);
			}
			try {
				JocketEndpoint endpoint = JocketReflection.newInstance(cls);
				endpoint.onOpen(session, httpSession);
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
			Class<? extends JocketEndpoint> cls = session.getEndpointClass();
			if (logger.isTraceEnabled()) {
				Object[] args = {session.getId(), cls.getName(), reason};
				logger.trace("[Jocket] Invoking endpoint: sid={}, method={}.onClose, reason={}", args);
			}
			try {
				JocketEndpoint endpoint = JocketReflection.newInstance(cls);
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
		
		private JocketPacket message;
		
		public MessageRunner(JocketSession session, JocketPacket message)
		{
			this.session = session;
			this.message = message;
		}

		public void run()
		{
			Class<? extends JocketEndpoint> cls = session.getEndpointClass();
			if (logger.isTraceEnabled()) {
				Object[] args = {session.getId(), cls.getName(), message};
				logger.trace("[Jocket] Invoking endpoint: sid={}, method={}.onMessage, message={}", args);
			}
			try {
				session.setLastMessageTime(JocketClock.now());
				JocketEndpoint endpoint = JocketReflection.newInstance(cls);
				endpoint.onMessage(session, message.getName(), message.getData());
			}
			catch (Throwable e) {
				logger.error("[Jocket] Failed to invoke " + cls.getName() + ".onMessage", e);
			}
		}
	}
}
