package com.jeedsoft.jocket.connection;

import java.util.HashMap;
import java.util.Map;

import com.jeedsoft.jocket.message.JocketConsumer;
import com.jeedsoft.jocket.util.JocketClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.transport.websocket.JocketWebSocketConnection;

public class JocketConnectionManager
{
	private static final Logger logger = LoggerFactory.getLogger(JocketConnectionManager.class);

	// connections on *this* server
	private static final Map<String, JocketConnection> connections = new HashMap<>();

	// probing connections on *this* server
	private static final Map<String, JocketWebSocketConnection> probingConnections = new HashMap<>();

	public static void add(JocketConnection cn)
	{
        if (logger.isTraceEnabled()) {
            logger.trace("[Jocket] Add connection: sid={}, cn={}", cn.getSessionId(), cn.getClass().getName());
        }
        String sessionId = cn.getSessionId();
        synchronized (connections) {
            connections.put(sessionId, cn);
        }
        JocketConsumer.notify(sessionId);
	}

	public static void addProbing(JocketWebSocketConnection cn)
	{
        String sessionId = cn.getSessionId();
        synchronized (probingConnections) {
            probingConnections.put(sessionId, cn);
        }
	}

	public static JocketConnection remove(String sessionId)
	{
		JocketConnection cn;
		synchronized (connections) {
            cn = connections.remove(sessionId);
        }
		if (cn == null) {
			logger.trace("[Jocket] No connection found when removing. sid={}", sessionId);
		}
		else {
            logger.trace("[Jocket] Remove connection: sid={}, cn={}", sessionId, cn.getClass().getName());
		}
		return cn;
	}

	public static boolean remove(JocketConnection cn)
	{
		String sessionId = cn.getSessionId();
		boolean match;
        synchronized (connections) {
            match = connections.get(sessionId) == cn;
            if (match) {
                connections.remove(sessionId);
            }
        }
		if (!match) {
            logger.debug("[Jocket] No connection found when removing. sid={}", sessionId);
        }
		return match;
	}

	public static JocketWebSocketConnection removeProbing(String sessionId)
	{
        JocketWebSocketConnection cn;
        synchronized (probingConnections) {
            cn = probingConnections.remove(sessionId);
        }
		if (cn == null) {
			logger.debug("[Jocket] No probing connection found when removing. sid={}", sessionId);
		}
		return cn;
	}

	public static boolean removeWebSocket(String sessionId)
	{
        JocketWebSocketConnection cn;
        synchronized (probingConnections) {
            cn = probingConnections.remove(sessionId);
        }
        if (cn == null) {
            synchronized (connections) {
                if (connections.get(sessionId) instanceof JocketWebSocketConnection) {
                    connections.remove(sessionId);
                }
            }
        }
		return cn != null; // is probing
	}

	public static JocketConnection get(String sessionId)
	{
        synchronized (connections) {
            return connections.get(sessionId);
        }
	}

	public static JocketConnection getProbing(String sessionId)
	{
        synchronized (probingConnections) {
            return probingConnections.get(sessionId);
        }
	}

	public static boolean upgrade(String sessionId)
	{
        JocketConnection cn;
        synchronized (probingConnections) {
            cn = probingConnections.remove(sessionId);
        }
        if (cn != null) {
            synchronized (connections) {
                connections.put(sessionId, cn);
            }
            JocketSession session = JocketSessionManager.get(sessionId);
            if (session != null) {
                session.setLastHeartbeatTime(JocketClock.now());
            }
            logger.debug("[Jocket] Upgrade the transport to WebSocket. sid={}", sessionId);
        }
        return cn != null;
	}

    public static boolean isUpgraded(String sessionId)
    {
        synchronized (connections) {
            return connections.get(sessionId) instanceof JocketWebSocketConnection;
        }
    }

	public static void clear()
	{
        synchronized (connections) {
            connections.clear();
        }
        synchronized (probingConnections) {
            probingConnections.clear();
        }
	}
	
	public static int size()
	{
        synchronized (connections) {
            return connections.size();
        }
	}
}
