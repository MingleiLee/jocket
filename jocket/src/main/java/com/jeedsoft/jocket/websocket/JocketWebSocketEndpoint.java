package com.jeedsoft.jocket.websocket;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.OnMessage;
import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.connection.JocketConnection;
import com.jeedsoft.jocket.connection.JocketConnectionManager;
import com.jeedsoft.jocket.connection.JocketStub;
import com.jeedsoft.jocket.connection.JocketStubManager;
import com.jeedsoft.jocket.connection.impl.JocketWebSocketConnection;
import com.jeedsoft.jocket.endpoint.JocketCloseReason;
import com.jeedsoft.jocket.endpoint.JocketEndpointConfig;
import com.jeedsoft.jocket.endpoint.JocketDeployer;
import com.jeedsoft.jocket.endpoint.JocketEndpointRunner;
import com.jeedsoft.jocket.event.JocketEvent;
import com.jeedsoft.jocket.exception.JocketException;
import com.jeedsoft.jocket.util.StringUtil;

public class JocketWebSocketEndpoint extends Endpoint
{
	private static final Logger logger = LoggerFactory.getLogger(JocketWebSocketEndpoint.class);
	
	private static String applicationContextPath;
	
	@Override
	public void onOpen(Session session, EndpointConfig endpointConfig)
	{
		String path = session.getRequestURI().toString();
		if (applicationContextPath != null && path.startsWith(applicationContextPath)) {
			path = path.substring(applicationContextPath.length());
		}
		
		try {
			//get stub and check status
			JocketEndpointConfig config = JocketDeployer.getConfig(path);
			String cid;
			List<String> list = session.getRequestParameterMap().get("jocket_cid");
			if (list == null) {
				Map<String, String> parameterMap = config.getPathParameterMap(path);
				for (Map.Entry<String, List<String>> entry: session.getRequestParameterMap().entrySet()) {
					parameterMap.put(entry.getKey(), entry.getValue().get(0));
				}
				JocketStub stub = new JocketStub();
				stub.setHandlerClass(config.getHandlerClass());
				stub.setStatus(JocketStub.STATUS_PREPARED);
				stub.setParameterMap(parameterMap);
				cid = JocketStubManager.add(stub);
			}
			else {
				cid = list.get(0);
				JocketStub stub = JocketStubManager.get(cid);
				if (stub == null) {
					throw new JocketException("Jocket connection id not found: " + cid);
				}
				int status = stub.getStatus();
				if (status != JocketStub.STATUS_PREPARED) {
					throw new JocketException("Jocket status invalid: cid=" + cid + ", status=" + status);
				}
			}
			
			//add to connection manager
			JocketWebSocketConnection cn = new JocketWebSocketConnection(cid, session);
			session.addMessageHandler(new JocketWebSocketMessageHandler(session));
			setConfig(session, config);
			setConnection(session, cn);
			JocketConnectionManager.add(cn);
			JocketEndpointRunner.doOpen(cn);
			logger.debug("[Jocket] Jocket opened: transport=websocket, cid={}, path={}", cid, path);
		}
		catch (Exception e) {
			logger.error("[Jocket] Failed to create WebSocket connection: path=" + path, e);
		}
	}

	@Override
	public void onClose(Session session, CloseReason closeReason)
	{
		JocketEndpointConfig config = getConfig(session); 
		JocketConnection cn = getConnection(session);
		String cid = cn.getId();
		JocketConnectionManager.remove(cid);
		JocketStub stub = JocketStubManager.get(cn.getId());
		if (stub != null) {
			cn.setStub(stub);
			JocketStubManager.remove(cid);
			int code = closeReason.getCloseCode().getCode();
			String description = closeReason.getReasonPhrase();
			JocketEndpointRunner.doClose(cn, new JocketCloseReason(code, description));
		}
		logger.debug("[Jocket] Jocket closed: transport=websocket, cid={}, path={}", cid, config.getPath());
	}
	
	@OnMessage
	public void onMessage(String message, Session session)
	{
		JocketConnection cn = getConnection(session); 
		JocketEvent event = JocketEvent.parse(message);
		JocketEndpointRunner.doMessage(cn, event);
		if (logger.isDebugEnabled()) {
			JocketEndpointConfig config = getConfig(session); 
			Object[] args = {cn.getId(), config.getPath(), event};
			logger.debug("[Jocket] Message received: transport=websocket, cid={}, path={}, event={}", args);
		}
	}

	@Override
	public void onError(Session session, Throwable e)
	{
		logger.error("[Jocket] WebSocket error occurs.", e);
	}

	public static void downstream(Session session, JocketEvent event) throws IOException
	{
		if (event.getType() == JocketEvent.TYPE_CLOSE) {
			session.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, ""));
		}
		else {
			session.getAsyncRemote().sendText(event.toJsonString());
		}
	}
	
	public static JocketEndpointConfig getConfig(Session session)
	{
		return (JocketEndpointConfig)session.getUserProperties().get("jocket_config");
	}

	public static void setConfig(Session session, JocketEndpointConfig config)
	{
		session.getUserProperties().put("jocket_config", config);
	}
	
	public static JocketConnection getConnection(Session session)
	{
		return (JocketConnection)session.getUserProperties().get("jocket_connection");
	}

	public static void setConnection(Session session, JocketConnection cn)
	{
		session.getUserProperties().put("jocket_connection", cn);
	}
	
	public static void setApplicationContextPath(String contextPath)
	{
		applicationContextPath = StringUtil.isEmpty(contextPath) ? null : contextPath;
	}
}
