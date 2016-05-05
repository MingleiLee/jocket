package com.jeedsoft.jocket.connection;

import java.util.List;

public interface JocketSessionStore
{
	void add(JocketSession session);

	JocketSession remove(String id);

	JocketSession get(String id);

	List<JocketSession> checkStore();

	int size();

	boolean contains(String id);

	boolean applySchedule();
}
