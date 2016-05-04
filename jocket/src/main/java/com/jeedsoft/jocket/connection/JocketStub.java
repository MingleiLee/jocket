package com.jeedsoft.jocket.connection;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.connection.impl.JocketPollingConnection;
import com.jeedsoft.jocket.connection.impl.JocketWebSocketConnection;
import com.jeedsoft.jocket.util.JocketJsonUtil;

public class JocketStub
{
	private static final Logger logger = LoggerFactory.getLogger(JocketStub.class);

	public static final String STATUS_PREPARED		= "prepared";
	public static final String STATUS_CONNECTED		= "connected";
	public static final String STATUS_RECONNECTING	= "reconnecting";

	public static final String TRANSPORT_WEBSOCKET	= "websocket";
	public static final String TRANSPORT_POLLING	= "polling";

	public static final String KEY_ID 					= "id";
	public static final String KEY_ENDPOINT_CLASS_NAME	= "endpointClassName";
	public static final String KEY_HTTP_SESSION_ID		= "httpSessionId";
	public static final String KEY_TRANSPORT			= "transport";
	public static final String KEY_STATUS				= "status";
	public static final String KEY_START_TIME			= "startTime";
	public static final String KEY_LAST_HEARTBEAT_TIME	= "lastHeartbeatTime";
	public static final String KEY_LAST_MESSAGE_TIME	= "lastMessageTime";
	public static final String KEY_TIMEOUT_SECONDS		= "timeoutSeconds";
	public static final String KEY_PARAMETERS			= "parameters";
	
	protected String id;
	
	private String httpSessionId;

	private String endpointClassName;
	
	private String transport;

	private String status;

	private long startTime;
	
	private long lastHeartbeatTime;

	private long lastMessageTime;

	private int timeoutSeconds;

	private Map<String, String> parameters = new HashMap<>();

	private Map<String, Object> attributes = new HashMap<>();

	public JocketStub()
	{
	}

	public JocketStub(Map<String, String> baseData, Map<String, Object> attributes)
	{
		this.id = baseData.get(KEY_ID);
		this.endpointClassName = baseData.get(KEY_ENDPOINT_CLASS_NAME);
		this.httpSessionId = baseData.get(KEY_HTTP_SESSION_ID);
		this.transport = baseData.get(KEY_TRANSPORT);
		this.status = baseData.get(KEY_STATUS);
		this.startTime = Long.parseLong(baseData.get(KEY_START_TIME));
		this.lastHeartbeatTime = Long.parseLong(baseData.get(KEY_LAST_HEARTBEAT_TIME));
		this.lastMessageTime = Long.parseLong(baseData.get(KEY_LAST_MESSAGE_TIME));
		this.timeoutSeconds = Integer.parseInt(baseData.get(KEY_TIMEOUT_SECONDS));
		this.parameters = JocketJsonUtil.toStringMap(new JSONObject(baseData.get(KEY_PARAMETERS)));
		if (attributes != null) {
			this.attributes = attributes;
		}
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public String getEndpointClassName()
	{
		return endpointClassName;
	}

	public void setEndpointClassName(String endpointClassName)
	{
		this.endpointClassName = endpointClassName;
	}

	public String getHttpSessionId()
	{
		return httpSessionId;
	}

	public void setHttpSessionId(String httpSessionId)
	{
		this.httpSessionId = httpSessionId;
	}

	public String getTransport()
	{
		return transport;
	}

	public void setTransport(String transport)
	{
		this.transport = transport;
	}

	public String getStatus()
	{
		return status;
	}

	public void setStatus(String status)
	{
		this.status = status;
	}

	public long getStartTime()
	{
		return startTime;
	}

	public void setStartTime(long startTime)
	{
		this.startTime = startTime;
	}

	public long getLastHeartbeatTime()
	{
		return lastHeartbeatTime;
	}

	public void setLastHeartbeatTime(long lastHeartbeatTime)
	{
		this.lastHeartbeatTime = lastHeartbeatTime;
	}
	
	public long getLastMessageTime()
	{
		return lastMessageTime;
	}

	public void setLastMessageTime(long lastMessageTime)
	{
		this.lastMessageTime = lastMessageTime;
	}

	public int getTimeoutSeconds()
	{
		return timeoutSeconds;
	}

	public void setTimeoutSeconds(int timeoutSeconds)
	{
		this.timeoutSeconds = timeoutSeconds;
	}

	public String getParameter(String key)
	{
		return parameters.get(key);
	}

	public Map<String, String> getParameters()
	{
		return parameters;
	}

	public void setParameters(Map<String, String> parameters)
	{
		this.parameters = parameters;
	}

	@SuppressWarnings("unchecked")
	public <T> T getAttribute(String key)
	{
		return (T)attributes.get(key);
	}

	public <T> void setAttribute(String key, T value)
	{
		attributes.put(key, value);
	}

	protected void setAttributes(Map<String, Object> attributes)
	{
		this.attributes = attributes;
	}

	public Map<String, String> toMap()
	{
		Map<String, String> map = new HashMap<>();
		put(map, KEY_ID, id);
		put(map, KEY_ENDPOINT_CLASS_NAME, endpointClassName);
		put(map, KEY_HTTP_SESSION_ID, httpSessionId);
		put(map, KEY_TRANSPORT, transport);
		put(map, KEY_STATUS, status + "");
		put(map, KEY_START_TIME, startTime + "");
		put(map, KEY_LAST_HEARTBEAT_TIME, lastHeartbeatTime + "");
		put(map, KEY_LAST_MESSAGE_TIME, lastMessageTime + "");
		put(map, KEY_TIMEOUT_SECONDS, timeoutSeconds + "");
		put(map, KEY_PARAMETERS, new JSONObject(parameters).toString());
		if (logger.isDebugEnabled()) {
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
			if (startTime != 0) {
				put(map, KEY_START_TIME + "$", df.format(startTime));
			}
			if (lastHeartbeatTime != 0) {
				put(map, KEY_LAST_HEARTBEAT_TIME + "$", df.format(lastHeartbeatTime));
			}
			if (lastMessageTime != 0) {
				put(map, KEY_LAST_MESSAGE_TIME + "$", df.format(lastMessageTime));
			}
		}
		System.out.println(map);
		return map;
	}
	
	public boolean isBroken()
	{
		long brokenMillis; 
		if (TRANSPORT_WEBSOCKET.equals(getTransport())) {
			brokenMillis = JocketWebSocketConnection.HEARTBEAT_INTERVAL + 20_000;
		}
		else {
			brokenMillis = JocketPollingConnection.POLLING_INTERVAL + 20_000;
		}
		long heartbeatTime = Math.max(getLastHeartbeatTime(), getStartTime());
		return heartbeatTime + brokenMillis < System.currentTimeMillis();
	}

	private static void put(Map<String, String> map, String key, String value)
	{
		if (value != null) {
			map.put(key, value);
		}
	}
}
