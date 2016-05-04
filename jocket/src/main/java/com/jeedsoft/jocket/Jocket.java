package com.jeedsoft.jocket;

import com.jeedsoft.jocket.connection.JocketStub;
import com.jeedsoft.jocket.connection.JocketStubManager;
import com.jeedsoft.jocket.endpoint.JocketCloseReason;
import com.jeedsoft.jocket.event.JocketEvent;
import com.jeedsoft.jocket.event.JocketQueueManager;

public class Jocket
{
	public static void emit(String connectionId, String name, Object data)
	{
		JocketEvent event = new JocketEvent(JocketEvent.TYPE_NORMAL, name, data);
		JocketQueueManager.publish(connectionId, event);
	}

	public static void close(String connectionId, int code, String description)
	{
		JocketCloseReason reason = new JocketCloseReason(code, description);
		JocketEvent event = new JocketEvent(JocketEvent.TYPE_CLOSE, null, reason.toJsonString());
		JocketQueueManager.publish(connectionId, event);
	}
	
	public static long getLastMessageTime(String connectionId)
	{
		JocketStub stub = JocketStubManager.get(connectionId);
		if (stub == null) {
			return 0;
		}
		return stub.getLastMessageTime();
	}
	
	public static <T> T getAttribute(String connectionId, String key)
	{
		JocketStub stub = JocketStubManager.get(connectionId);
		if (stub == null) {
			return null;
		}
		return stub.getAttribute(key);
	}
}
