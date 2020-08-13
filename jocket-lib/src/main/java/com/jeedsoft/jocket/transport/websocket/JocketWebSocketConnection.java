package com.jeedsoft.jocket.transport.websocket;

import java.io.IOException;

import javax.websocket.Session;

import com.jeedsoft.jocket.util.JocketIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.connection.JocketCloseReason;
import com.jeedsoft.jocket.connection.JocketConnection;
import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.message.JocketPacket;
import com.jeedsoft.jocket.util.JocketWebSocketUtil;

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
	public synchronized boolean downstream(JocketPacket packet) throws IOException
	{
		String sessionId = getSessionId();
		if (isActive()) {
			if (packet.getType().equals(JocketPacket.TYPE_MESSAGE)) {
				packet.setId(JocketIdGenerator.generate());
			}
			logger.debug("[Jocket] Send message to client: transport=websocket, sid={}, packet={}", sessionId, packet);
			wsSession.getBasicRemote().sendText(packet.toJson().toString());
			return true;
		}
		else {
			logger.warn("[Jocket] WebSocket connection is closed: sid={}, packet={}", sessionId, packet);
			return false;
		}
	}

	@Override
	public void close(JocketCloseReason reason) throws IOException
	{
		JocketWebSocketUtil.close(wsSession, reason.getCode(), reason.getMessage());
	}
}
