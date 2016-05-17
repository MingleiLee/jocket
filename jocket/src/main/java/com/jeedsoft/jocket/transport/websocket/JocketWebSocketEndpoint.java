package com.jeedsoft.jocket.transport.websocket;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

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
import com.jeedsoft.jocket.util.JocketStringUtil;
import com.jeedsoft.jocket.util.JocketWebSocketUtil;

public class JocketWebSocketEndpoint extends Endpoint
{
	private static final Logger logger = LoggerFactory.getLogger(JocketWebSocketEndpoint.class);
	
	private static String applicationContextPath;
	
	@Override
	public void onOpen(Session wsSession, EndpointConfig endpointConfig)
	{
		try {
			//get session and check status
			String sessionId = JocketWebSocketUtil.getParameter(wsSession, "jocket_sid");
			if (sessionId == null) {
				String message = "Jocket should be prepared first";
				JocketWebSocketUtil.close(wsSession, JocketCloseCode.NEED_INIT, message);
				return;
				/* TODO
				String path = wsSession.getRequestURI().toString();
				if (applicationContextPath != null && path.startsWith(applicationContextPath)) {
					path = path.substring(applicationContextPath.length());
				}
				JocketEndpointConfig config = JocketDeployer.getConfig(path);
				Map<String, String> parameters = config.getPathParameterMap(path);
				for (Map.Entry<String, List<String>> entry: wsSession.getRequestParameterMap().entrySet()) {
					parameters.put(entry.getKey(), entry.getValue().get(0));
				}
				JocketSession session = new JocketSession();
				session.setRequestPath(path);
				session.setEndpointClassName(config.getEndpointClassName());
				session.setParameters(parameters);
				session.setStatus(JocketSession.STATUS_PREPARED);
				sessionId = JocketSessionManager.add(session);
				*/
			}
			JocketSession session = JocketSessionManager.get(sessionId);
			if (session == null) {
				throw new JocketException("Jocket session not found: id=" + sessionId);
			}
			String status = session.getStatus();
			if (!JocketSession.STATUS_PREPARED.equals(status)) {
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
			logger.error("[Jocket] Failed to open WebSocket connection: path=" + getPath(wsSession), e);
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
			Object[] args = {sessionId, getPath(wsSession)};
			logger.debug("[Jocket] Jocket closed: transport=websocket, sid={}, path={}", args);
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
	
	private String getPath(Session wsSession)
	{
		String path = wsSession.getRequestURI().toString();
		if (applicationContextPath != null && path.startsWith(applicationContextPath)) {
			path = path.substring(applicationContextPath.length());
		}
		return path;
	}
	
	/**
	 * Attention: This method is invoked through reflection in JocketDeployer
	 */
	public static void setApplicationContextPath(String contextPath)
	{
		applicationContextPath = JocketStringUtil.isEmpty(contextPath) ? null : contextPath;
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
