package com.jeedsoft.jocket.connection.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jeedsoft.jocket.connection.JocketStub;
import com.jeedsoft.jocket.connection.JocketStubStore;
import com.jeedsoft.jocket.endpoint.JocketAbstractEndpoint;

public class JocketStandaloneStubStore implements JocketStubStore
{
	private Map<String, JocketStub> map = new HashMap<>();

	@Override
	public synchronized void add(JocketStub stub)
	{
		map.put(stub.getId(), stub);
	}

	@Override
	public synchronized void remove(String id)
	{
		map.remove(id);
	}

	@Override
	public synchronized JocketStub get(String id)
	{
		return map.get(id);
	}

	@Override
	public synchronized int getStatus(String id)
	{
		JocketStub stub = map.get(id);
		return stub == null ? 0 : stub.getStatus();
	}

	@Override
	public synchronized void setStatus(String id, int status)
	{
		JocketStub stub = map.get(id);
		if (stub != null) {
			stub.setStatus(status);
		}
	}

	@Override
	public synchronized int getTransport(String id)
	{
		JocketStub stub = map.get(id);
		return stub == null ? 0 : stub.getTransport();
	}

	@Override
	public synchronized void setTransport(String id, int transport)
	{
		JocketStub stub = map.get(id);
		if (stub != null) {
			stub.setTransport(transport);
		}
	}

	public synchronized long getLastPolling(String id)
	{
		JocketStub stub = map.get(id);
		return stub == null ? 0 : stub.getLastPolling();
	}

	public synchronized void setLastPolling(String id, long lastPolling)
	{
		JocketStub stub = map.get(id);
		if (stub != null) {
			stub.setLastPolling(lastPolling);
		}
	}

	@Override
	public synchronized Class<? extends JocketAbstractEndpoint> getHandlerClass(String id)
	{
		JocketStub stub = map.get(id);
		return stub == null ? null : stub.getHandlerClass();
	}

	@Override
	public String getParameter(String id, String key)
	{
		JocketStub stub = map.get(id);
		return stub == null ? null : stub.getParameter(key);
	}

	@Override
	public synchronized Map<String, String> getParameterMap(String id)
	{
		JocketStub stub = map.get(id);
		return stub == null ? null : stub.getParameterMap();
	}

	@Override
	@SuppressWarnings("unchecked")
	public synchronized <T> T getUserProperty(String id, String key)
	{
		JocketStub stub = map.get(id);
		return stub == null ? null : (T)stub.getUserProperty(key);
	}

	@Override
	public synchronized <T> void setUserProperty(String id, String key, T value)
	{
		JocketStub stub = map.get(id);
		if (stub != null) {
			stub.setUserProperty(key, value);
		}
	}

	@Override
	public synchronized List<JocketStub> checkCorruption()
	{
		List<JocketStub> corruptedStubs = new ArrayList<>();
		long now = System.currentTimeMillis();
		for (JocketStub stub: map.values()) {
			long lastPolling = stub.getLastPolling();
			if (lastPolling > 0 && lastPolling + JocketPollingConnection.POLLING_INTERVAL + 20_000 < now) {
				corruptedStubs.add(stub);
			}
		}
		for (JocketStub stub: corruptedStubs) {
			map.remove(stub.getId());
		}
		return corruptedStubs;
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
}
