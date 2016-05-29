package com.jeedsoft.jocket.connection;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.message.JocketQueueManager;
import com.jeedsoft.jocket.transport.polling.JocketPollingConnection;
import com.jeedsoft.jocket.transport.websocket.JocketWebSocketConnection;

public class JocketConnectionManager
{
	private static final Logger logger = LoggerFactory.getLogger(JocketConnectionManager.class);

	//connections on *this* server
	private static Map<String, JocketConnection> connections = new HashMap<>();

	//probing connections on *this* server
	private static Map<String, JocketWebSocketConnection> probingConnections = new HashMap<>();

	public static synchronized void add(JocketConnection cn)
	{
		String sessionId = cn.getSessionId();
		connections.put(sessionId, cn);
		cn.getSession().setConnected(true);
	}

	public static synchronized void addProbing(JocketWebSocketConnection cn)
	{
		String sessionId = cn.getSessionId();
		probingConnections.put(sessionId, cn);
	}

	public static synchronized JocketConnection remove(String sessionId)
	{
		JocketConnection cn = connections.remove(sessionId);
		if (cn == null) {
			logger.debug("[Jocket] No connection found when removing. sid={}", sessionId);
		}
		else {
			cn.getSession().setConnected(false);
			JocketQueueManager.removeSubscriber(sessionId, false);
		}
		return cn;
	}

	public static synchronized JocketWebSocketConnection removeProbing(String sessionId)
	{
		JocketWebSocketConnection cn = probingConnections.remove(sessionId);
		if (cn == null) {
			logger.debug("[Jocket] No probing connection found when removing. sid={}", sessionId);
		}
		return cn;
	}

	public static synchronized boolean removeWebSocket(String sessionId)
	{
		JocketConnection cn = probingConnections.remove(sessionId);
		if (cn == null && connections.get(sessionId) instanceof JocketWebSocketConnection) {
			connections.remove(sessionId);
		}
		return cn != null; //is probing
	}

	public static synchronized JocketConnection get(String sessionId)
	{
		return connections.get(sessionId);
	}

	public static synchronized JocketConnection getProbing(String sessionId)
	{
		return probingConnections.get(sessionId);
	}

	public static synchronized JocketPollingConnection upgrade(String sessionId)
	{
		JocketConnection newcn = probingConnections.remove(sessionId);
		return (JocketPollingConnection)connections.put(sessionId, newcn);
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
