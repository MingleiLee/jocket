package com.jeedsoft.jocket.transport.websocket;

import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.connection.JocketSessionManager;
import com.jeedsoft.jocket.endpoint.JocketEndpointRunner;
import com.jeedsoft.jocket.event.JocketEvent;

class JocketWebSocketMessageHandler implements MessageHandler.Whole<String>
{
	private static final Logger logger = LoggerFactory.getLogger(JocketWebSocketMessageHandler.class);
	
	private Session wsSession;
	
	public JocketWebSocketMessageHandler(Session wsSession)
	{
		this.wsSession = wsSession;
	}
	
	@Override
    public void onMessage(String text)
	{
		//TODO heartbeat
		String sessionId = JocketWebSocketEndpoint.getJocketSessionId(wsSession);
		JocketSession session = JocketSessionManager.get(sessionId);
		if (session == null) {
			logger.error("[Jocket] session not found: id={}", sessionId);
			return;
		}
		JocketEvent event = JocketEvent.parse(text);
		JocketEndpointRunner.doMessage(session, event);
		if (logger.isDebugEnabled()) {
			Object[] args = {sessionId, JocketWebSocketEndpoint.getPath(wsSession), event};
			logger.debug("[Jocket] Message received: transport=websocket, sid={}, path={}, event={}", args);
		}
   }
}
