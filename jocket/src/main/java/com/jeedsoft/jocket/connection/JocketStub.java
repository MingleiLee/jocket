package com.jeedsoft.jocket.connection;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.jeedsoft.jocket.endpoint.JocketAbstractEndpoint;
import com.jeedsoft.jocket.util.JocketJsonUtil;
import com.jeedsoft.jocket.util.ReflectUtil;

public class JocketStub
{
	public static final int STATUS_INVALID		= 0;
	public static final int STATUS_PREPARED		= 1;
	public static final int STATUS_CONNECTED	= 2;
	public static final int STATUS_RECONNECTING = 3;

	public static final int TRANSPORT_WEBSOCKET	= 1;
	public static final int TRANSPORT_POLLING	= 2;

	public static final String KEY_ID 				= "id";
	public static final String KEY_HTTP_SESSION_ID	= "httpSessionId";
	public static final String KEY_HANDLER_CLASS	= "handlerClass";
	public static final String KEY_STATUS			= "status";
	public static final String KEY_LAST_POLLING 	= "lastPolling";
	public static final String KEY_PARAMETERS		= "parameters";

	private String id;
	
	private String httpSessionId;
	
	private Class<? extends JocketAbstractEndpoint> handlerClass;

	private int status;
	
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

	public void setUserProperties(Map<String, Object> userProperties)
	{
		this.userProperties = userProperties;
	}

	public Map<String, String> toMap()
	{
		Map<String, String> map = new HashMap<>();
		map.put(KEY_ID, id);
		map.put(KEY_HTTP_SESSION_ID, httpSessionId);
		map.put(KEY_HANDLER_CLASS, handlerClass.getName());
		map.put(KEY_STATUS, status + "");
		map.put(KEY_LAST_POLLING, lastPolling + "");
		map.put(KEY_PARAMETERS, new JSONObject(parameterMap).toString());
		return map;
	}
	
	public static JocketStub fromMap(Map<String, String> map)
	{
		JocketStub stub = new JocketStub();
		stub.id = map.get(KEY_ID);
		stub.httpSessionId = map.get(KEY_HTTP_SESSION_ID);
		stub.handlerClass = ReflectUtil.getClass(map.get(KEY_HANDLER_CLASS));
		stub.status = Integer.parseInt(map.get(KEY_STATUS));
		stub.lastPolling = Long.parseLong(map.get(KEY_LAST_POLLING));
		stub.parameterMap = JocketJsonUtil.toStringMap(new JSONObject(map.get(KEY_PARAMETERS)));
		return stub;
	}
}
