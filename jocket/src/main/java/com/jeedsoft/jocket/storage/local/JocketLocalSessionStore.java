package com.jeedsoft.jocket.storage.local;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.connection.JocketSessionStore;

public class JocketLocalSessionStore implements JocketSessionStore
{
	private Map<String, JocketSession> map = new HashMap<>();

	@Override
	public synchronized void add(JocketSession session)
	{
		map.put(session.getId(), session);
	}

	@Override
	public synchronized JocketSession remove(String id)
	{
		return map.remove(id);
	}

	@Override
	public synchronized JocketSession get(String id)
	{
		return map.get(id);
	}

	@Override
	public synchronized List<JocketSession> checkStore()
	{
		List<JocketSession> brokenSessions = new ArrayList<>();
		for (JocketSession session: map.values()) {
			if (session.isBroken()) {
				brokenSessions.add(session);
			}
		}
		for (JocketSession session: brokenSessions) {
			map.remove(session.getId());
		}
		return brokenSessions;
	}

	@Override
	public synchronized int size()
	{
		return map.size();
	}

	@Override
	public synchronized boolean contains(String id)
	{
		return map.containsKey(id);
	}

	@Override
	public boolean applySchedule()
	{
		return true;
	}
}
