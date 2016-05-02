package com.jeedsoft.jocket.connection;

import java.util.List;
import java.util.Map;

import com.jeedsoft.jocket.endpoint.JocketAbstractEndpoint;

public interface JocketStubStore
{
	void add(JocketStub stub);

	void remove(String id);

	JocketStub get(String id);

	int getStatus(String id);

	void setStatus(String id, int status);

	int getTransport(String id);

	void setTransport(String id, int transport);

	long getLastPolling(String id);

	void setLastPolling(String id, long lastPolling);

	Class<? extends JocketAbstractEndpoint> getHandlerClass(String id);

	String getParameter(String id, String key);

	Map<String, String> getParameterMap(String id);

	<T> T getUserProperty(String id, String key);

	<T> void setUserProperty(String id, String key, T value);

	List<JocketStub> checkCorruption();

	int size();

	boolean contains(String id);
}
