package com.jeedsoft.jocket.transport.websocket;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.JocketService;
import com.jeedsoft.jocket.connection.JocketCloseCode;
import com.jeedsoft.jocket.connection.JocketCloseReason;
import com.jeedsoft.jocket.connection.JocketConnectionManager;
import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.connection.JocketSessionManager;
import com.jeedsoft.jocket.endpoint.JocketEndpointRunner;
import com.jeedsoft.jocket.message.JocketPacket;
import com.jeedsoft.jocket.message.JocketQueueManager;
import com.jeedsoft.jocket.transport.polling.JocketPollingConnection;
import com.jeedsoft.jocket.util.JocketCloseException;
import com.jeedsoft.jocket.util.JocketThreadUtil;
import com.jeedsoft.jocket.util.JocketWebSocketUtil;

@ServerEndpoint("/jocket-ws")
public class JocketWebSocketEndpoint
{
	private static final Logger logger = LoggerFactory.getLogger(JocketWebSocketEndpoint.class);
	
	private static final String path = "/jocket-ws";
	
	@OnOpen
	public void onOpen(Session wsSession, EndpointConfig endpointConfig)
	{
		JocketThreadUtil.updateWebSocketThreadName(path + "/open");
		JocketSession session = null;
		try {
			String sessionId = JocketWebSocketUtil.getParameter(wsSession, "s");
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
			if (session.isUpgraded()) {
				throw new JocketCloseException(JocketCloseCode.VIOLATED_POLICY, "Jocket is already upgraded");
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
			logger.error("[Jocket] Failed to open WebSocket connection: path=" + path, e);
			JocketWebSocketUtil.close(wsSession, e.getCode(), e.getMessage());
		}
	}

	@OnClose
	public void onClose(Session wsSession, CloseReason closeReason)
	{
		JocketThreadUtil.updateWebSocketThreadName(path + "/close");
		JocketWebSocketConnection cn = getConnection(wsSession);
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
		JocketSession session = JocketSessionManager.get(sessionId);
		if (session == null) {
			logger.error("[Jocket] Session not found: sid=" + sessionId);
			JocketWebSocketUtil.close(wsSession, JocketCloseCode.SESSION_NOT_FOUND, "session not found");
			return;
		}
		JocketPacket packet = JocketPacket.parse(text);
		String type = packet.getType();
		if (JocketPacket.TYPE_MESSAGE.equals(type)) {
			JocketEndpointRunner.doMessage(session, packet);
		}
		else if (JocketPacket.TYPE_PING.equals(type)) {
			try {
		        session.setLastHeartbeatTime(System.currentTimeMillis());
				cn.downstream(new JocketPacket(JocketPacket.TYPE_PONG));
			}
			catch (Throwable e) {
				logger.error("[Jocket] Failed to send PONG packet to client: sid=" + sessionId);
			}
		}
		else if (JocketPacket.TYPE_UPGRADE.equals(packet.getType())) {
			synchronized (cn) {
				if (cn.isActive()) {
					logger.debug("[Jocket] Upgrade the transport to WebSocket: sid=" + sessionId);
					session.setStatus(JocketSession.STATUS_OPEN);
					JocketConnectionManager.removeProbing(sessionId);
					JocketPollingConnection pc = (JocketPollingConnection)JocketConnectionManager.remove(sessionId);
					JocketConnectionManager.add(cn);
					JocketQueueManager.addSubscriber(cn);
					if (pc == null) {
						JocketQueueManager.publishEvent(sessionId, new JocketPacket(JocketPacket.TYPE_UPGRADE));
					}
					else {
						pc.closeOnUpgrade();
					}
				}
			}
		}
		else {
			logger.error("[Jocket] Invalid packet type for WebSocket connection: " + type);
		}
	}

	@OnError
	public void onError(Session wsSession, Throwable e)
	{
		logger.error("[Jocket] WebSocket error occurs.", e);
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
