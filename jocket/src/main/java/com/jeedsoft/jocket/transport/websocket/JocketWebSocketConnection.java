package com.jeedsoft.jocket.transport.websocket;

import java.io.IOException;

import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.connection.JocketConnection;
import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.event.JocketEvent;

public class JocketWebSocketConnection extends JocketConnection
{
	private static final Logger logger = LoggerFactory.getLogger(JocketWebSocketConnection.class);

	private Session wsSession;

	public JocketWebSocketConnection(JocketSession session, Session wsSession)
	{
		super(session);
		this.wsSession = wsSession;
	}

	public Session getWebSocketSession()
	{
		return wsSession;
	}

	public void setWebSocketSession(Session wsSession)
	{
		this.wsSession = wsSession;
	}

	@Override
	public void onEvent(String sessionId, JocketEvent event)
	{
		try {
			JocketWebSocketEndpoint.downstream(wsSession, event);
		}
		catch (IOException e) {
			logger.error("[Jocket] Failed to send message: sid={}, event={}", sessionId, event);
		}
	}

	@Override
	public boolean isAutoNext()
	{
		return true;
	}
}
