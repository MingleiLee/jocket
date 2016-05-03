package com.jeedsoft.jocket.connection;

import java.util.HashMap;
import java.util.Map;

import com.jeedsoft.jocket.event.JocketQueueManager;

public class JocketConnectionManager
{
	//connections on this server
	private static Map<String, JocketConnection> map = new HashMap<>();
	
	public static synchronized void add(JocketConnection connection)
	{
		String id = connection.getId();
		map.put(id, connection);
		JocketStubManager.setStatus(id, JocketStub.STATUS_CONNECTED);
		JocketQueueManager.subscribe(connection, id);
	}

	public static synchronized void remove(String id)
	{
		map.remove(id);
		JocketStubManager.setStatus(id, JocketStub.STATUS_RECONNECTING);
		JocketQueueManager.unsubscribe(id, false);
	}

	public static synchronized JocketConnection get(String id)
	{
		return map.get(id);
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
