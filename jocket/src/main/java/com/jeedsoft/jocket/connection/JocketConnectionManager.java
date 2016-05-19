package com.jeedsoft.jocket.connection;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.message.JocketQueueManager;

public class JocketConnectionManager
{
	private static final Logger logger = LoggerFactory.getLogger(JocketConnectionManager.class);

	//connections on *this* server
	private static Map<String, JocketConnection> connections = new HashMap<>();

	//probing connections on *this* server
	private static Map<String, JocketConnection> probingConnections = new HashMap<>();

	public static synchronized void add(JocketConnection cn)
	{
		String sessionId = cn.getSessionId();
		connections.put(sessionId, cn);
		cn.getSession().setConnected(true);
	}

	public static synchronized void addProbing(JocketConnection cn)
	{
		String sessionId = cn.getSessionId();
		probingConnections.put(sessionId, cn);
	}

	public static synchronized void remove(String sessionId)
	{
		JocketConnection cn = connections.remove(sessionId);
		if (cn == null) {
			logger.debug("[Jocket] no connection found when removing. sid={}", sessionId);
			return;
		}
		cn.getSession().setConnected(false);
		JocketQueueManager.removeSubscriber(sessionId, false);
	}

	public static synchronized void removeProbing(String sessionId)
	{
		JocketConnection cn = probingConnections.remove(sessionId);
		if (cn == null) {
			logger.debug("[Jocket] no probing connection found when removing. sid={}", sessionId);
			return;
		}
	}

	public static synchronized JocketConnection get(String sessionId)
	{
		return connections.get(sessionId);
	}

	public static synchronized JocketConnection getProbing(String sessionId)
	{
		return probingConnections.get(sessionId);
	}

	public static synchronized void clear()
	{
		connections.clear();
		probingConnections.clear();
	}
	
	public static synchronized int size()
	{
		return connections.size();
	}
}
