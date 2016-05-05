package com.jeedsoft.jocket;

import com.jeedsoft.jocket.connection.JocketCloseReason;
import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.connection.JocketSessionManager;
import com.jeedsoft.jocket.event.JocketEvent;
import com.jeedsoft.jocket.event.JocketQueueManager;

public class Jocket
{
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
	
	public static long getLastMessageTime(String sessionId)
	{
		JocketSession session = JocketSessionManager.get(sessionId);
		if (session == null) {
			return 0;
		}
		return session.getLastMessageTime();
	}
	
	public static <T> T getAttribute(String sessionId, String key)
	{
		JocketSession session = JocketSessionManager.get(sessionId);
		if (session == null) {
			return null;
		}
		return session.getAttribute(key);
	}
}
