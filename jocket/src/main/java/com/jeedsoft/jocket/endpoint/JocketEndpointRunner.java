package com.jeedsoft.jocket.endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.connection.JocketConnection;
import com.jeedsoft.jocket.event.JocketEvent;
import com.jeedsoft.jocket.util.ReflectUtil;

public class JocketEndpointRunner
{
	private static final Logger logger = LoggerFactory.getLogger(JocketEndpointRunner.class);
	
	public static void doOpen(JocketConnection cn)
	{
		new Thread(new OpenRunner(cn)).start();
	}

	public static void doClose(JocketConnection cn, JocketCloseReason reason)
	{
		new Thread(new CloseRunner(cn, reason)).start();
	}
	
	public static void doMessage(JocketConnection cn, JocketEvent event)
	{
		new Thread(new MessageRunner(cn, event)).start();
	}

	private static class OpenRunner implements Runnable
	{
		private JocketConnection cn;
		
		public OpenRunner(JocketConnection cn)
		{
			this.cn = cn;
		}

		public void run()
		{
			Class<? extends JocketAbstractEndpoint> cls = cn.getHandlerClass();
			if (logger.isTraceEnabled()) {
				Object[] args = {cn.getId(), cls.getName()};
				logger.trace("[Jocket] Invoking endpoint: cid={}, method={}.onOpen", args);
			}
			try {
				JocketAbstractEndpoint endpoint = ReflectUtil.newInstance(cls);
				endpoint.onOpen(cn);
			}
			catch (Throwable e) {
				logger.error("[Jocket] Failed to invoke " + cls.getName() + ".onOpen", e);
			}
		}
	}

	private static class CloseRunner implements Runnable
	{
		private JocketConnection cn;
		
		private JocketCloseReason reason;
		
		public CloseRunner(JocketConnection cn, JocketCloseReason reason)
		{
			this.cn = cn;
			this.reason = reason;
		}

		public void run()
		{
			Class<? extends JocketAbstractEndpoint> cls = cn.getHandlerClass();
			if (logger.isTraceEnabled()) {
				Object[] args = {cn.getId(), cls.getName(), reason};
				logger.trace("[Jocket] Invoking endpoint: cid={}, method={}.onClose, reason={}", args);
			}
			try {
				JocketAbstractEndpoint endpoint = ReflectUtil.newInstance(cls);
				endpoint.onClose(cn, reason);
			}
			catch (Throwable e) {
				logger.error("[Jocket] Failed to invoke " + cls.getName() + ".onClose", e);
			}
		}
	}

	private static class MessageRunner implements Runnable
	{
		private JocketConnection cn;
		
		private JocketEvent event;
		
		public MessageRunner(JocketConnection cn, JocketEvent event)
		{
			this.cn = cn;
			this.event = event;
		}

		public void run()
		{
			Class<? extends JocketAbstractEndpoint> cls = cn.getHandlerClass();
			if (logger.isTraceEnabled()) {
				Object[] args = {cn.getId(), cls.getName(), event};
				logger.trace("[Jocket] Invoking endpoint: cid={}, method={}.onMessage, event={}", args);
			}
			try {
				JocketAbstractEndpoint endpoint = ReflectUtil.newInstance(cls);
				endpoint.onMessage(cn, event.getName(), event.getData());
			}
			catch (Throwable e) {
				logger.error("[Jocket] Failed to invoke " + cls.getName() + ".onMessage", e);
			}
		}
	}
}
