package com.jeedsoft.jocket.websocket;

import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.connection.JocketConnection;
import com.jeedsoft.jocket.endpoint.JocketConfig;
import com.jeedsoft.jocket.endpoint.JocketEndpointRunner;
import com.jeedsoft.jocket.event.JocketEvent;

class JocketWebSocketMessageHandler implements MessageHandler.Whole<String>
{
	private static final Logger logger = LoggerFactory.getLogger(JocketWebSocketMessageHandler.class);
	
	private Session session;
	
	public JocketWebSocketMessageHandler(Session session)
	{
		this.session = session;
	}
	
	@Override
    public void onMessage(String text)
	{
		JocketConfig config = JocketWebSocketEndpoint.getConfig(session); 
		JocketConnection cn = JocketWebSocketEndpoint.getConnection(session); 
		JocketEvent event = JocketEvent.parse(text);
		JocketEndpointRunner.doMessage(cn, event);
		if (logger.isDebugEnabled()) {
			Object[] args = {cn.getId(), config.getPath(), event};
			logger.debug("[Jocket] Message received: transport=websocket, cid={}, path={}, event={}", args);
		}
   }
}
