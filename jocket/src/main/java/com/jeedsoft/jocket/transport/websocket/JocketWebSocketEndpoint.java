package com.jeedsoft.jocket.transport.websocket;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.connection.JocketCloseCode;
import com.jeedsoft.jocket.connection.JocketCloseReason;
import com.jeedsoft.jocket.connection.JocketConnectionManager;
import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.connection.JocketSessionManager;
import com.jeedsoft.jocket.endpoint.JocketEndpointRunner;
import com.jeedsoft.jocket.transport.JocketUpstreamHandler;
import com.jeedsoft.jocket.util.JocketException;
import com.jeedsoft.jocket.util.JocketWebSocketUtil;

@ServerEndpoint("/jocket-ws")
public class JocketWebSocketEndpoint extends Endpoint
{
	private static final Logger logger = LoggerFactory.getLogger(JocketWebSocketEndpoint.class);
	
	@Override
	public void onOpen(Session wsSession, EndpointConfig endpointConfig)
	{
		JocketSession session = null;
		try {
			//get session and check status
			String sessionId = JocketWebSocketUtil.getParameter(wsSession, "s");
			if (sessionId == null) {
				String message = "The Jocket session ID parameter is missing";
				JocketWebSocketUtil.close(wsSession, JocketCloseCode.NO_SESSION_PARAM, message);
				return;
			}
			session = JocketSessionManager.get(sessionId);
			if (session == null) {
				throw new JocketException("Jocket session not found: id=" + sessionId);
			}
			String status = session.getStatus();
			if (!JocketSession.STATUS_NEW.equals(status)) {
				throw new JocketException("Jocket status invalid: id=" + sessionId + ", status=" + status);
			}
			
			//add to connection manager
			JocketWebSocketConnection cn = new JocketWebSocketConnection(session, wsSession);
			wsSession.addMessageHandler(new JocketWebSocketMessageHandler(wsSession));
			setJocketSessionId(wsSession, session.getId());
			session.setTransport(JocketSession.TRANSPORT_WEBSOCKET);
			JocketConnectionManager.add(cn);
			if (logger.isDebugEnabled()) {
				Object[] args = {sessionId, session.getRequestPath()};
				logger.debug("[Jocket] Jocket WebSocket connection opened: sid={}, path={}", args);
			}
		}
		catch (Exception e) {
			String path = session == null ? "NULL" : session.getRequestPath();
			logger.error("[Jocket] Failed to open WebSocket connection: path=" + path, e);
			JocketWebSocketUtil.close(wsSession, JocketCloseCode.CLOSED_ABNORMALLY, e.getMessage());
		}
	}

	@Override
	public void onClose(Session wsSession, CloseReason closeReason)
	{
		String sessionId = getJocketSessionId(wsSession);
		JocketConnectionManager.remove(sessionId);
		JocketSession session = JocketSessionManager.remove(sessionId);
		if (session != null) {
			int code = closeReason.getCloseCode().getCode();
			String message = closeReason.getReasonPhrase();
			JocketEndpointRunner.doClose(session, new JocketCloseReason(code, message));
		}
		if (logger.isDebugEnabled()) {
			String path = session == null ? "NULL" : session.getRequestPath();
			logger.debug("[Jocket] Jocket closed: transport=websocket, sid={}, path={}", sessionId, path);
		}
	}

	@Override
	public void onError(Session wsSession, Throwable e)
	{
		logger.error("[Jocket] WebSocket error occurs.", e);
	}
	
	public static String getJocketSessionId(Session wsSession)
	{
		return (String)wsSession.getUserProperties().get("jocket_sid");
	}

	private static void setJocketSessionId(Session wsSession, String sessionId)
	{
		wsSession.getUserProperties().put("jocket_sid", sessionId);
	}

	private static class JocketWebSocketMessageHandler implements MessageHandler.Whole<String>
	{
		private Session wsSession;
		
		public JocketWebSocketMessageHandler(Session wsSession)
		{
			this.wsSession = wsSession;
		}
		
		@Override
	    public void onMessage(String text)
		{
			String sessionId = JocketWebSocketEndpoint.getJocketSessionId(wsSession);
			JocketUpstreamHandler.handle(sessionId, text);
		}
	}
}
