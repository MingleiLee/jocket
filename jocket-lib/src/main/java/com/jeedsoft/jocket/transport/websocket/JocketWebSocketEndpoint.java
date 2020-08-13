package com.jeedsoft.jocket.transport.websocket;

import com.jeedsoft.jocket.JocketService;
import com.jeedsoft.jocket.connection.*;
import com.jeedsoft.jocket.endpoint.JocketEndpointRunner;
import com.jeedsoft.jocket.message.JocketPacket;
import com.jeedsoft.jocket.message.JocketQueueManager;
import com.jeedsoft.jocket.util.*;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/jocket-ws")
public class JocketWebSocketEndpoint
{
	private static final Logger logger = LoggerFactory.getLogger(JocketWebSocketEndpoint.class);
	
	private static final String path = "/jocket-ws";
	
	@OnOpen
	public void onOpen(Session wsSession, EndpointConfig endpointConfig)
	{
		JocketThreadUtil.updateWebSocketThreadName(path + "/open");
		String sessionId = null;
		JocketSession session = null;
		try {
			sessionId = JocketWebSocketUtil.getParameter(wsSession, "s");
			if (sessionId == null) {
				throw new JocketCloseException(JocketCloseCode.VIOLATED_POLICY, "No Jocket session ID parameter");
			}
			session = JocketSessionManager.get(sessionId);
			if (session == null) {
				throw new JocketCloseException(JocketCloseCode.SESSION_NOT_FOUND, "Jocket session not found");
			}
			String status = session.getStatus();
			if (!JocketSession.STATUS_OPEN.equals(status) && !JocketSession.STATUS_HANDSHAKING.equals(status)) {
				throw new JocketCloseException(JocketCloseCode.INVALID_STATUS, "Invalid status: " + status);
			}
			if (JocketConnectionManager.getProbing(sessionId) != null) {
				throw new JocketCloseException(JocketCloseCode.VIOLATED_POLICY, "Jocket is already probing");
			}
			
			//add to connection manager
			wsSession.setMaxIdleTimeout(JocketService.getConnectionTimeout());
			JocketWebSocketConnection cn = new JocketWebSocketConnection(session, wsSession);
			setConnection(wsSession, cn);
			JocketConnectionManager.addProbing(cn);
			if (logger.isDebugEnabled()) {
				Object[] args = {sessionId, session.getRequestPath()};
				logger.debug("[Jocket] Jocket WebSocket connection opened: sid={}, path={}", args);
			}
		}
		catch (JocketCloseException e) {
			String path = session == null ? "NULL" : session.getRequestPath();
			if (e.getCode() == JocketCloseCode.SESSION_NOT_FOUND) {
				logger.debug("[Jocket] Session not found: sid=" + sessionId + ", path=" + path, e);
			}
			else {
				logger.error("[Jocket] Failed to open WebSocket connection: sid=" + sessionId + ", path=" + path, e);
			}
			JocketWebSocketUtil.close(wsSession, e.getCode(), e.getMessage());
		}
	}

	@OnClose
	public void onClose(Session wsSession, CloseReason closeReason)
	{
		JocketThreadUtil.updateWebSocketThreadName(path + "/close");
		JocketWebSocketConnection cn = getConnection(wsSession);
		if (cn == null) {
			return;
		}
		String sessionId = cn.getSessionId();
		JocketSession session = null;
		synchronized (cn) {
			if (cn.isActive()) {
				cn.setActive(false);
				if (JocketConnectionManager.removeProbing(sessionId) == null) {
					JocketConnectionManager.remove(sessionId);
					session = JocketSessionManager.remove(sessionId);
				}
			}
		}
		if (session != null) {
			int code = closeReason.getCloseCode().getCode();
			String message = closeReason.getReasonPhrase();
			JocketEndpointRunner.doClose(session, new JocketCloseReason(code, message));
			if (logger.isDebugEnabled()) {
				String path = session.getRequestPath();
				logger.debug("[Jocket] Jocket closed: transport=websocket, sid={}, path={}", sessionId, path);
			}
		}
	}
	
	@OnMessage
	public void onMessage(String text, Session wsSession)
	{
		JocketThreadUtil.updateWebSocketThreadName(path + "/message");
		JocketWebSocketConnection cn = getConnection(wsSession);
		String sessionId = cn.getSessionId();
		logger.trace("[Jocket] Packet received: sid={}, packet={}", sessionId, text);

		JocketPacket packet = JocketPacket.parse(text);
        String type = packet.getType();
		if (type.equals(JocketPacket.TYPE_CONFIRM)) {
            logger.trace("[Jocket] Message confirmed: sid={}, packetId={}", sessionId, packet.getData());
            return;
		}
        else if (type.equals(JocketPacket.TYPE_LOG)) {
            JocketClientLogger.log(sessionId, new JSONArray(packet.getData()));
            return;
        }

		JocketSession session = JocketSessionManager.get(sessionId);
		if (session == null) {
			logger.error("[Jocket] Session not found: sid={}", sessionId);
			JocketWebSocketUtil.close(wsSession, JocketCloseCode.SESSION_NOT_FOUND, "session not found");
			return;
		}

		if (type.equals(JocketPacket.TYPE_MESSAGE)) {
			JocketEndpointRunner.doMessage(session, packet);
		}
		else if (type.equals(JocketPacket.TYPE_PING)) {
			try {
                if (JocketConnectionManager.isUpgraded(sessionId)) {
                    session.setLastHeartbeatTime(JocketClock.now());
                }
				cn.downstream(new JocketPacket(JocketPacket.TYPE_PONG));
			}
			catch (Throwable e) {
				logger.error("[Jocket] Failed to send PONG packet to client: sid=" + sessionId, e);
			}
		}
		else if (type.equals(JocketPacket.TYPE_UPGRADE)) {
            JocketQueueManager.publish(sessionId, new JocketPacket(JocketPacket.TYPE_UPGRADE));
		}
		else {
			logger.error("[Jocket] Invalid packet type for WebSocket connection: sid={}, type={}", sessionId, type);
		}
	}

	@OnError
	public void onError(Session wsSession, Throwable e)
	{
		logger.debug("[Jocket] WebSocket error occurs.", e);
	}

	private static JocketWebSocketConnection getConnection(Session wsSession)
	{
		return (JocketWebSocketConnection)wsSession.getUserProperties().get("jocket_cn");
	}

	private static void setConnection(Session wsSession, JocketWebSocketConnection cn)
	{
		wsSession.getUserProperties().put("jocket_cn", cn);
	}
}
