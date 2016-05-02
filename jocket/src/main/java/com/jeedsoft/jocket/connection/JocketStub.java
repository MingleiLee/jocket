package com.jeedsoft.jocket.connection;

import java.util.HashMap;
import java.util.Map;

import com.jeedsoft.jocket.endpoint.JocketAbstractEndpoint;

public class JocketStub
{
	public static final int STATUS_PREPARED		= 1;
	public static final int STATUS_CONNECTED	= 2;
	public static final int STATUS_RECONNECTING = 3;

	public static final int TRANSPORT_WEBSOCKET	= 1;
	public static final int TRANSPORT_POLLING	= 2;

	private String id;
	
	private String httpSessionId;
	
	private Class<? extends JocketAbstractEndpoint> handlerClass;

	private int status;
	
	private int transport;
	
	private long lastPolling;
	
	private Map<String, String> parameterMap = new HashMap<>();

	private Map<String, Object> userProperties = new HashMap<>();

	public JocketStub()
	{
	}
	
	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public String getHttpSessionId()
	{
		return httpSessionId;
	}

	public void setHttpSessionId(String httpSessionId)
	{
		this.httpSessionId = httpSessionId;
	}

	public Class<? extends JocketAbstractEndpoint> getHandlerClass()
	{
		return handlerClass;
	}

	public void setHandlerClass(Class<? extends JocketAbstractEndpoint> handlerClass)
	{
		this.handlerClass = handlerClass;
	}

	public int getStatus()
	{
		return status;
	}

	public void setStatus(int status)
	{
		this.status = status;
	}

	public int getTransport()
	{
		return transport;
	}

	public void setTransport(int transport)
	{
		this.transport = transport;
	}

	public long getLastPolling()
	{
		return lastPolling;
	}

	public void setLastPolling(long lastPolling)
	{
		this.lastPolling = lastPolling;
	}

	public String getParameter(String key)
	{
		return parameterMap.get(key);
	}

	public Map<String, String> getParameterMap()
	{
		return parameterMap;
	}

	public void setParameterMap(Map<String, String> parameterMap)
	{
		this.parameterMap = parameterMap;
	}

	@SuppressWarnings("unchecked")
	public <T> T getUserProperty(String key)
	{
		return (T)userProperties.get(key);
	}

	public <T> void setUserProperty(String key, T value)
	{
		userProperties.put(key, value);
	}
}
