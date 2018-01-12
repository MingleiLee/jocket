package com.jeedsoft.jocket.storage.local;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.connection.JocketSessionStore;
import com.jeedsoft.jocket.util.JocketStringUtil;

public class JocketLocalSessionStore implements JocketSessionStore
{
	private Map<String, JocketSession> sessionMap = new HashMap<>();

	private Map<String, Set<String>> userSessionMap = new HashMap<>();

	@Override
	public synchronized void add(JocketSession session)
	{
		sessionMap.put(session.getId(), session);
	}

	@Override
	public synchronized JocketSession remove(String id)
	{
		JocketSession session = sessionMap.remove(id);
		if (session != null) {
			String userId = session.getUserId();
			if (userId != null) {
				updateUserId(id, userId, null);
			}
		}
		return session;
	}

	@Override
	public synchronized JocketSession get(String id)
	{
		return sessionMap.get(id);
	}

	@Override
	public synchronized boolean contains(String id)
	{
		return sessionMap.containsKey(id);
	}

	@Override
	public synchronized List<String> getAllSessionIds()
	{
		return new ArrayList<>(sessionMap.keySet());
	}

	@Override
	public synchronized int size()
	{
		return sessionMap.size();
	}

	@Override
	public synchronized void updateUserId(String id, String oldUserId, String newUserId)
	{
		if (!JocketStringUtil.isEmpty(oldUserId)) {
			Set<String> sessionIds = userSessionMap.get(oldUserId);
			if (sessionIds != null) {
				sessionIds.remove(id);
				if (sessionIds.isEmpty()) {
					userSessionMap.remove(oldUserId);
				}
			}
		}
		if (!JocketStringUtil.isEmpty(newUserId)) {
			Set<String> sessionIds = userSessionMap.get(newUserId);
			if (sessionIds == null) {
				sessionIds = new HashSet<>();
				userSessionMap.put(newUserId, sessionIds);
			}
			sessionIds.add(id);
		}
	}

	@Override
	public synchronized List<JocketSession> getUserSessions(String userId)
	{
		List<JocketSession> sessions = new ArrayList<>();
		Set<String> sessionIds = userSessionMap.get(userId);
		if (sessionIds != null) {
			for (String sessionId: sessionIds) {
				JocketSession session = sessionMap.get(sessionId);
				if (session != null && session.isOpen()) {
					sessions.add(session);
				}
			}
		}
		return sessions;
	}

	@Override
	public synchronized List<JocketSession> checkStore()
	{
		List<JocketSession> brokenSessions = new ArrayList<>();
		for (JocketSession session: sessionMap.values()) {
			if (session.isBroken()) {
				brokenSessions.add(session);
			}
		}
		for (JocketSession session: brokenSessions) {
			sessionMap.remove(session.getId());
		}
		return brokenSessions;
	}

	@Override
	public boolean applySchedule()
	{
		return true;
	}
}
