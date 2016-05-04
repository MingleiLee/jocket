package com.jeedsoft.jocket.connection;

import java.util.List;
import java.util.UUID;

import com.jeedsoft.jocket.event.JocketQueueManager;
import com.jeedsoft.jocket.storage.memory.JocketMemoryStubStore;

public class JocketStubManager
{
	private static JocketStubStore store = new JocketMemoryStubStore();

	public static JocketStubStore getStore()
	{
		return store;
	}

	public static void setStore(JocketStubStore store)
	{
		JocketStubManager.store = store;
	}

	public static String add(JocketStub stub)
	{
		stub.setId(UUID.randomUUID().toString());
		stub.setStartTime(System.currentTimeMillis());
		store.add(stub);
		return stub.getId();
	}

	public static JocketStub remove(String id)
	{
		JocketQueueManager.unsubscribe(id, true);
		return store.remove(id);
	}

	public static JocketStub get(String id)
	{
		return store.get(id);
	}
	
	public static List<JocketStub> checkCorruption()
	{
		return store.checkStore();
	}
	
	public static int size()
	{
		return store.size();
	}

	public static boolean contains(String id)
	{
		return store.contains(id);
	}

	public static boolean applySchedule()
	{
		return store.applySchedule();
	}
}
