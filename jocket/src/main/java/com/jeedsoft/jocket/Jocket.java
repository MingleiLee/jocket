package com.jeedsoft.jocket;

import com.jeedsoft.jocket.connection.JocketCloseReason;
import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.connection.JocketSessionManager;
import com.jeedsoft.jocket.message.JocketPacket;
import com.jeedsoft.jocket.message.JocketQueueManager;

public class Jocket
{
	public static void send(String sessionId, Object data)
	{
		send(sessionId, data, null);
	}

	public static void send(String sessionId, Object data, String name)
	{
		JocketPacket packet = new JocketPacket(JocketPacket.TYPE_MESSAGE, name, data);
		JocketQueueManager.publishMessage(sessionId, packet);
	}

	public static void close(String sessionId, int code, String message)
	{
		JocketCloseReason reason = new JocketCloseReason(code, message);
		JocketSessionManager.close(sessionId, reason, true);
	}
	
	public static JocketSession getSession(String sessionId)
	{
		return JocketSessionManager.get(sessionId);
	}
}
