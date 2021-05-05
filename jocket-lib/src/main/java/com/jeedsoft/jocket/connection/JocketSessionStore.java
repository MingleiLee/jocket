package com.jeedsoft.jocket.connection;

import java.util.List;

public interface JocketSessionStore
{
	void add(JocketSession session);

	JocketSession remove(String id);

	JocketSession get(String id);

	List<String> getAllSessionIds();

	boolean contains(String id);

	void updateUserId(String id, String oldUserId, String newUserId);

	void updateOnlineUserId(String id, String oldOnlineUserId, String newOnlineUserId);

	List<JocketSession> getUserSessions(String userId);

	JocketSession getOnlineUserSession(String onlineUserId);

	List<JocketSession> checkStore();

	int size();

	boolean applySchedule();
}
