package com.jeedsoft.jocket;

import com.jeedsoft.jocket.connection.JocketCloseReason;
import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.connection.JocketSessionManager;
import com.jeedsoft.jocket.message.JocketPacket;
import com.jeedsoft.jocket.message.JocketQueueManager;

public class Jocket
{
    public static final String VERSION = "2.0.4";
    
	public static void send(String sessionId, String name, Object data)
	{
		JocketPacket packet = new JocketPacket(JocketPacket.TYPE_MESSAGE, name, data);
		JocketQueueManager.publish(sessionId, packet);
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
