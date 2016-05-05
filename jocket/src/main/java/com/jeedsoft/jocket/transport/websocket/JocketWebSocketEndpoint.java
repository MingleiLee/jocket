package com.jeedsoft.jocket.transport.websocket;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.connection.JocketCloseReason;
import com.jeedsoft.jocket.connection.JocketConnectionManager;
import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.connection.JocketSessionManager;
import com.jeedsoft.jocket.endpoint.JocketDeployer;
import com.jeedsoft.jocket.endpoint.JocketEndpointConfig;
import com.jeedsoft.jocket.endpoint.JocketEndpointRunner;
import com.jeedsoft.jocket.event.JocketEvent;
import com.jeedsoft.jocket.util.JocketException;
import com.jeedsoft.jocket.util.JocketStringUtil;

public class JocketWebSocketEndpoint extends Endpoint
{
	private static final Logger logger = LoggerFactory.getLogger(JocketWebSocketEndpoint.class);
	
	private static String applicationContextPath;
	
	@Override
	public void onOpen(Session wsSession, EndpointConfig endpointConfig)
	{
		String path = wsSession.getRequestURI().toString();
		if (applicationContextPath != null && path.startsWith(applicationContextPath)) {
			path = path.substring(applicationContextPath.length());
		}
		
		try {
			//get session and check status
			JocketEndpointConfig config = JocketDeployer.getConfig(path);
			String sessionId;
			List<String> list = wsSession.getRequestParameterMap().get("jocket_sid");
			if (list == null) {
				Map<String, String> parameters = config.getPathParameterMap(path);
				for (Map.Entry<String, List<String>> entry: wsSession.getRequestParameterMap().entrySet()) {
					parameters.put(entry.getKey(), entry.getValue().get(0));
				}
				JocketSession session = new JocketSession();
				session.setEndpointClassName(config.getEndpointClassName());
				session.setParameters(parameters);
				session.setStatus(JocketSession.STATUS_PREPARED);
				sessionId = JocketSessionManager.add(session);
			}
			else {
				sessionId = list.get(0);
			}
			JocketSession session = JocketSessionManager.get(sessionId);
			if (session == null) {
				throw new JocketException("Jocket session not found: id=" + sessionId);
			}
			String status = session.getStatus();
			if (JocketSession.STATUS_PREPARED.equals(status)) {
				throw new JocketException("Jocket status invalid: id=" + sessionId + ", status=" + status);
			}
			
			//add to connection manager
			JocketWebSocketConnection cn = new JocketWebSocketConnection(session, wsSession);
			wsSession.addMessageHandler(new JocketWebSocketMessageHandler(wsSession));
			setPath(wsSession, path);
			setJocketSessionId(wsSession, session.getId());
			session.setTransport(JocketSession.TRANSPORT_WEBSOCKET);
			JocketConnectionManager.add(cn);
			JocketEndpointRunner.doOpen(session);
			logger.debug("[Jocket] Jocket opened: transport=websocket, sid={}, path={}", sessionId, path);
		}
		catch (Exception e) {
			logger.error("[Jocket] Failed to create WebSocket connection: path=" + path, e);
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
			String description = closeReason.getReasonPhrase();
			JocketEndpointRunner.doClose(session, new JocketCloseReason(code, description));
		}
		if (logger.isDebugEnabled()) {
			String path = getPath(wsSession);
			logger.debug("[Jocket] Jocket closed: transport=websocket, sid={}, path={}", sessionId, path);
		}
	}

	@Override
	public void onError(Session wsSession, Throwable e)
	{
		logger.error("[Jocket] WebSocket error occurs.", e);
	}

	public static void downstream(Session wsSession, JocketEvent event) throws IOException
	{
		if (event.getType() == JocketEvent.TYPE_CLOSE) {
			wsSession.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, ""));
		}
		else {
			wsSession.getAsyncRemote().sendText(event.toJsonString());
		}
	}
	
	public static String getPath(Session wsSession)
	{
		return (String)wsSession.getUserProperties().get("jocket_path");
	}

	private static void setPath(Session wsSession, String path)
	{
		wsSession.getUserProperties().put("jocket_path", path);
	}
	
	public static String getJocketSessionId(Session wsSession)
	{
		return (String)wsSession.getUserProperties().get("jocket_sid");
	}

	private static void setJocketSessionId(Session wsSession, String sessionId)
	{
		wsSession.getUserProperties().put("jocket_sid", sessionId);
	}
	
	public static void setApplicationContextPath(String contextPath)
	{
		applicationContextPath = JocketStringUtil.isEmpty(contextPath) ? null : contextPath;
	}
}
