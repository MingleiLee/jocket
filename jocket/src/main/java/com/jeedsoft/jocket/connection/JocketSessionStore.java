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

	List<JocketSession> getUserSessions(String userId);
	
	List<JocketSession> checkStore();

	int size();

	boolean applySchedule();
}
