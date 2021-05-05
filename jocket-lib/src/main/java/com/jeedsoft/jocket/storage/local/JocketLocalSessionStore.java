package com.jeedsoft.jocket.storage.local;

import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.connection.JocketSessionStore;
import com.jeedsoft.jocket.util.JocketStringUtil;

import java.util.*;

public class JocketLocalSessionStore implements JocketSessionStore
{
	private final Map<String, JocketSession> sessionMap = new HashMap<>();

	private final Map<String, Set<String>> userSessionMap = new HashMap<>();

	private final Map<String, String> onlineUserSessionMap = new HashMap<>();

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
			String onlineUserId = session.getOnlineUserId();
			if (onlineUserId != null) {
				updateOnlineUserId(id, onlineUserId, null);
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
        JocketSession session = get(id);
        return session != null && !session.isBroken();
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
			userSessionMap.computeIfAbsent(newUserId, k -> new HashSet<>()).add(id);
		}
	}

	@Override
	public void updateOnlineUserId(String id, String oldOnlineUserId, String newOnlineUserId)
	{
		if (!JocketStringUtil.isEmpty(oldOnlineUserId)) {
			onlineUserSessionMap.remove(oldOnlineUserId);
		}
		if (!JocketStringUtil.isEmpty(newOnlineUserId)) {
			onlineUserSessionMap.put(newOnlineUserId, id);
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
	public synchronized JocketSession getOnlineUserSession(String onlineUserId)
	{
		JocketSession session = sessionMap.get(onlineUserSessionMap.get(onlineUserId));
		if (session != null && session.isOpen() && onlineUserId.equals(session.getOnlineUserId())) {
			return session;
		}
		return null;
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
		for (Set<String> sessionIds: userSessionMap.values()) {
			sessionIds.removeIf(sessionId -> !sessionMap.containsKey(sessionId));
		}
		userSessionMap.values().removeIf(Set::isEmpty);
		onlineUserSessionMap.values().removeIf(sessionId -> !sessionMap.containsKey(sessionId));
		return brokenSessions;
	}

	@Override
	public boolean applySchedule()
	{
		return true;
	}
}
