package com.jeedsoft.jocket.connection;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.jeedsoft.jocket.connection.impl.JocketStandaloneStubStore;
import com.jeedsoft.jocket.endpoint.JocketAbstractEndpoint;
import com.jeedsoft.jocket.event.JocketQueueManager;

public class JocketStubManager
{
	private static JocketStubStore store = new JocketStandaloneStubStore();

	public static void setStore(JocketStubStore store)
	{
		JocketStubManager.store = store;
	}

	public static String add(JocketStub stub)
	{
		String id = UUID.randomUUID().toString();
		stub.setId(id);
		store.add(stub);
		return id;
	}

	public static void remove(String id)
	{
		store.remove(id);
		JocketQueueManager.unsubscribe(id);
	}

	public static JocketStub get(String id)
	{
		return store.get(id);
	}

	public static void getStatus(String id)
	{
		store.getStatus(id);
	}

	public static void setStatus(String id, int status)
	{
		store.setStatus(id, status);
	}

	public static int getTransport(String id)
	{
		return store.getTransport(id);
	}

	public static void setTransport(String id, int transport)
	{
		store.setTransport(id, transport);
	}

	public static long getLastPolling(String id)
	{
		return store.getLastPolling(id);
	}

	public static void setLastPolling(String id, long lastPolling)
	{
		store.setLastPolling(id, lastPolling);
	}

	public static String getParameter(String id, String key)
	{
		return store.getParameter(id, key);
	}

	public static Map<String, String> getParameterMap(String id)
	{
		return store.getParameterMap(id);
	}

	public static <T> T getUserProperty(String id, String key)
	{
		return store.getUserProperty(id, key);
	}

	public static <T> void setUserProperty(String id, String key, T value)
	{
		store.setUserProperty(id, key, value);
	}

	public static Class<? extends JocketAbstractEndpoint> getHandlerClass(String id)
	{
		return store.getHandlerClass(id);
	}
	
	public static List<JocketStub> checkCorruption()
	{
		return store.checkCorruption();
	}
	
	public static int size()
	{
		return store.size();
	}

	public static boolean contains(String id)
	{
		return store.contains(id);
	}
}
