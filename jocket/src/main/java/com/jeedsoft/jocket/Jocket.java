package com.jeedsoft.jocket;

import com.jeedsoft.jocket.connection.JocketCloseReason;
import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.connection.JocketSessionManager;
import com.jeedsoft.jocket.event.JocketEvent;
import com.jeedsoft.jocket.event.JocketQueueManager;

public class Jocket
{
	public static void send(String sessionId, Object data)
	{
		emit(sessionId, "message", data);
	}

	public static void emit(String sessionId, String name, Object data)
	{
		JocketEvent event = new JocketEvent(JocketEvent.TYPE_NORMAL, name, data);
		JocketQueueManager.publish(sessionId, event);
	}

	public static void close(String sessionId, int code, String description)
	{
		JocketCloseReason reason = new JocketCloseReason(code, description);
		JocketEvent event = new JocketEvent(JocketEvent.TYPE_CLOSE, null, reason.toJsonString());
		JocketQueueManager.publish(sessionId, event);
	}
	
	public static JocketSession getSession(String sessionId)
	{
		return JocketSessionManager.get(sessionId);
	}
}
