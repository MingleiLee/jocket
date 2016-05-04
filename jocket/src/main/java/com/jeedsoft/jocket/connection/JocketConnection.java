package com.jeedsoft.jocket.connection;

import java.util.Map;

import com.jeedsoft.jocket.endpoint.JocketAbstractEndpoint;
import com.jeedsoft.jocket.event.JocketEvent;
import com.jeedsoft.jocket.event.JocketQueueManager;
import com.jeedsoft.jocket.event.JocketSubscriber;
import com.jeedsoft.jocket.util.ReflectUtil;

public abstract class JocketConnection implements JocketSubscriber
{
	private JocketStub stub;

	public JocketConnection(JocketStub stub)
	{
		this.stub = stub;
	}

	public JocketStub getStub()
	{
		return stub;
	}

	public void setStub(JocketStub stub)
	{
		this.stub = stub;
	}

	public String getId()
	{
		return stub.getId();
	}

	public Class<? extends JocketAbstractEndpoint> getEndpointClass()
	{
		return ReflectUtil.getClass(stub.getEndpointClassName());
	}

	public String getTransport()
	{
		return stub.getTransport();
	}

	public String getStatus()
	{
		return stub.getStatus();
	}
	
	public long getLastHeartbeatTime()
	{
		return stub.getLastHeartbeatTime();
	}
	
	public long getLastMessageTime()
	{
		return stub.getLastMessageTime();
	}

	public int getTimeoutSeconds()
	{
		return stub.getTimeoutSeconds();
	}

	public void setTimeoutSeconds(int timeoutSeconds)
	{
		stub.setTimeoutSeconds(timeoutSeconds);
	}

	public String getParameter(String key)
	{
		return stub.getParameter(key);
	}

	public Map<String, String> getParameters()
	{
		return stub.getParameters();
	}

	public <T> T getAttribute(String key)
	{
		return stub.getAttribute(key);
	}

	public <T> void setAttribute(String key, T value)
	{
		stub.setAttribute(key, value);
	}

	public void emit(String eventName, Object data)
	{
		JocketEvent event = new JocketEvent(JocketEvent.TYPE_NORMAL, eventName, data);
		JocketQueueManager.publish(stub.getId(), event);
	}
}
