package com.jeedsoft.jocket.connection;

import java.util.Map;

import com.jeedsoft.jocket.endpoint.JocketAbstractEndpoint;
import com.jeedsoft.jocket.event.JocketEvent;
import com.jeedsoft.jocket.event.JocketQueueManager;
import com.jeedsoft.jocket.event.JocketSubscriber;

public abstract class JocketConnection implements JocketSubscriber
{
	private String id;

	private JocketStub stub;

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public void setStub(JocketStub stub)
	{
		this.stub = stub;
	}

	public Class<? extends JocketAbstractEndpoint> getHandlerClass()
	{
		if (stub == null) {
			return JocketStubManager.getHandlerClass(id);
		}
		else {
			return stub.getHandlerClass();
		}
	}

	public String getParameter(String key)
	{
		return stub == null ? JocketStubManager.getParameter(id, key) : stub.getParameter(key);
	}

	public Map<String, String> getParameterMap()
	{
		return stub == null ? JocketStubManager.getParameterMap(id) : stub.getParameterMap();
	}

	public <T> T getUserProperty(String key)
	{
		if (stub == null) {
			return JocketStubManager.getUserProperty(id, key);
		}
		else {
			return stub.getUserProperty(key);
		}
	}

	public <T> void setUserProperty(String key, T value)
	{
		JocketStubManager.setUserProperty(id, key, value);
	}

	public void emit(String eventName, Object data)
	{
		JocketEvent event = new JocketEvent(JocketEvent.TYPE_NORMAL, eventName, data);
		JocketQueueManager.publish(getId(), event);
	}
}
