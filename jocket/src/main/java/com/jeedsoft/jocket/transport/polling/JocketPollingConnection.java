package com.jeedsoft.jocket.transport.polling;

import java.io.IOException;

import javax.servlet.AsyncContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.connection.JocketConnection;
import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.event.JocketEvent;

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
	public void onEvent(String sessionId, JocketEvent event)
	{
		try {
			JocketPollingServlet.downstream(this, event);
		}
		catch (IOException e) {
			logger.error("[Jocket] Failed to send message: sid={}, event={}", sessionId, event);
		}
	}

	@Override
	public boolean isAutoNext()
	{
		return false;
	}
}
