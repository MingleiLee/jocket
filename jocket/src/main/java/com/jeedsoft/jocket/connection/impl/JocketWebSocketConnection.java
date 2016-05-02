package com.jeedsoft.jocket.connection.impl;

import java.io.IOException;

import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.connection.JocketConnection;
import com.jeedsoft.jocket.event.JocketEvent;
import com.jeedsoft.jocket.websocket.JocketWebSocketEndpoint;

public class JocketWebSocketConnection extends JocketConnection
{
	private static final Logger logger = LoggerFactory.getLogger(JocketWebSocketConnection.class);
	
	private Session session;

	public JocketWebSocketConnection(String id, Session session)
	{
		setId(id);
		this.session = session;
	}

	public Session getWebSocketSession()
	{
		return session;
	}

	public void setWebSocketSession(Session session)
	{
		this.session = session;
	}

	@Override
	public void onEvent(String connectionId, JocketEvent event)
	{
		try {
			JocketWebSocketEndpoint.downstream(session, event);
		}
		catch (IOException e) {
			logger.error("[Jocket] Failed to send message: cid={}, event={}", connectionId, event);
		}
	}
}
