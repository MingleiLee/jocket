package com.jeedsoft.jocket.connection;

import java.util.List;

public interface JocketStubStore
{
	void add(JocketStub stub);

	JocketStub remove(String id);

	JocketStub get(String id);

	List<JocketStub> checkStore();

	int size();

	boolean contains(String id);

	boolean applySchedule();
}
