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
	private static Map<String, JocketConnection> map = new HashMap<>();
	
	public static synchronized void add(JocketConnection cn)
	{
		String sessionId = cn.getSessionId();
		map.put(sessionId, cn);
		cn.getSession().setConnected(true);
	}

	public static synchronized void remove(String sessionId)
	{
		JocketConnection cn = map.remove(sessionId);
		if (cn == null) {
			logger.debug("[Jocket] no connection found when removing. sid={}", sessionId);
			return;
		}
		cn.getSession().setConnected(false);
		JocketQueueManager.removeSubscriber(sessionId, false);
	}

	public static synchronized JocketConnection get(String sessionId)
	{
		return map.get(sessionId);
	}
	
	public static synchronized void clear()
	{
		map.clear();
	}
	
	public static synchronized int size()
	{
		return map.size();
	}
}
