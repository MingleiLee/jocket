package com.jeedsoft.jocket.connection;

import java.util.List;
import java.util.UUID;

import com.jeedsoft.jocket.event.JocketQueueManager;
import com.jeedsoft.jocket.storage.local.JocketLocalSessionStore;

public class JocketSessionManager
{
	private static JocketSessionStore store = new JocketLocalSessionStore();

	public static JocketSessionStore getStore()
	{
		return store;
	}

	public static void setStore(JocketSessionStore store)
	{
		JocketSessionManager.store = store;
	}

	public static String add(JocketSession session)
	{
		session.setId(UUID.randomUUID().toString());
		session.setStartTime(System.currentTimeMillis());
		store.add(session);
		return session.getId();
	}

	public static JocketSession remove(String id)
	{
		JocketQueueManager.unsubscribe(id, true);
		return store.remove(id);
	}

	public static JocketSession get(String id)
	{
		return store.get(id);
	}
	
	public static List<JocketSession> checkStore()
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
