package com.jeedsoft.jocket.connection;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.event.JocketQueueManager;

public class JocketConnectionManager
{
	private static final Logger logger = LoggerFactory.getLogger(JocketConnectionManager.class);

	//connections on this server
	private static Map<String, JocketConnection> map = new HashMap<>();
	
	public static synchronized void add(JocketConnection connection)
	{
		String sessionId = connection.getSessionId();
		map.put(sessionId, connection);
		connection.getSession().setStatus(JocketSession.STATUS_CONNECTED);
		JocketQueueManager.subscribe(connection, sessionId);
	}

	public static synchronized void remove(String sessionId)
	{
		JocketConnection connection = map.remove(sessionId);
		if (connection == null) {
			logger.debug("[Jocket] no connection found when removing. sid={}", sessionId);
			return;
		}
		connection.getSession().setStatus(JocketSession.STATUS_RECONNECTING);
		JocketQueueManager.unsubscribe(sessionId, false);
	}

	public static synchronized JocketConnection get(String sessionId)
	{
		return map.get(sessionId);
	}
	
	public static synchronized void clear()
	{
		map.clear();
	}
	
	public static int size()
	{
		return map.size();
	}
}
