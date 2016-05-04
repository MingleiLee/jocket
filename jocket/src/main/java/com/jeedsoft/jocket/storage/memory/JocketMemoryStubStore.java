package com.jeedsoft.jocket.storage.memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jeedsoft.jocket.connection.JocketStub;
import com.jeedsoft.jocket.connection.JocketStubStore;

public class JocketMemoryStubStore implements JocketStubStore
{
	private Map<String, JocketStub> map = new HashMap<>();

	@Override
	public synchronized void add(JocketStub stub)
	{
		map.put(stub.getId(), stub);
	}

	@Override
	public synchronized JocketStub remove(String id)
	{
		return map.remove(id);
	}

	@Override
	public synchronized JocketStub get(String id)
	{
		return map.get(id);
	}

	@Override
	public synchronized List<JocketStub> checkStore()
	{
		List<JocketStub> brokenStubs = new ArrayList<>();
		for (JocketStub stub: map.values()) {
			if (stub.isBroken()) {
				brokenStubs.add(stub);
			}
		}
		for (JocketStub stub: brokenStubs) {
			map.remove(stub.getId());
		}
		return brokenStubs;
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
