package com.jeedsoft.jocket.transport.polling;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.connection.JocketCloseReason;
import com.jeedsoft.jocket.connection.JocketConnection;
import com.jeedsoft.jocket.connection.JocketConnectionManager;
import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.message.JocketPacket;
import com.jeedsoft.jocket.util.JocketIoUtil;

public class JocketPollingConnection extends JocketConnection
{
	private static final Logger logger = LoggerFactory.getLogger(JocketPollingConnection.class);
	
	private AsyncContext context;
	
	public JocketPollingConnection(JocketSession session)
	{
		super(session);
	}

	public JocketPollingConnection(JocketSession session, AsyncContext context)
	{
		super(session);
		this.context = context;
	}

	public AsyncContext getPollingContext()
	{
		return context;
	}

	public void setPollingContext(AsyncContext context)
	{
		this.context = context;
	}

	@Override
	public synchronized void downstream(JocketPacket packet) throws IOException
	{
		if (!isActive()) {
			return;
		}
		String sessionId = getSessionId();
		String type = packet.getType();
		if (JocketPacket.TYPE_NOOP.equals(type)) {
			logger.trace("[Jocket] Polling timeout: sid={}", sessionId);
		}
		else {
			logger.debug("[Jocket] Send message to client: transport=polling, sid={}, packet={}", sessionId, packet);
		}
		try {
	        JocketConnectionManager.remove(this); //connection must be removed before write response
	        HttpServletResponse response = (HttpServletResponse)context.getResponse();
	        JocketIoUtil.writeJson(response, packet.toJson());
	        context.complete();
		}
		finally {
			setActive(false);
		}
	}

	public void closeOnUpgrade()
	{
		try {
			downstream(new JocketPacket(JocketPacket.TYPE_NOOP));
		}
		catch (Throwable e) {
			logger.error("[Jocket] Failed to close polling connection on upgrade. sid={}", getSessionId());
		}
	}

	@Override
	public void close(JocketCloseReason reason) throws IOException
	{
		downstream(new JocketPacket(JocketPacket.TYPE_CLOSE, null, reason));
	}
}
