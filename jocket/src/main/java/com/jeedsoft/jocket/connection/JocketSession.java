package com.jeedsoft.jocket.connection;

import java.util.HashMap;
import java.util.Map;

import com.jeedsoft.jocket.endpoint.JocketDeployer;
import com.jeedsoft.jocket.endpoint.JocketEndpoint;
import com.jeedsoft.jocket.message.JocketPacket;
import com.jeedsoft.jocket.message.JocketQueueManager;
import com.jeedsoft.jocket.util.JocketConstant;

public class JocketSession
{
	public static final String STATUS_PREPARED		= "prepared";
	public static final String STATUS_OPEN			= "open";
	public static final String STATUS_CLOSED		= "closed";

	public static final String TRANSPORT_WEBSOCKET	= "websocket";
	public static final String TRANSPORT_POLLING	= "polling";
	
	protected String id;

	private String requestPath;

	private String httpSessionId;
	
	private String endpointClassName;
	
	private String transport;

	private String status;
	
	private boolean connected;

	private boolean waitingHeartbeat;
	
	private long startTime;

	private long closeTime;

	private long lastHeartbeatTime;

	private long lastMessageTime;

	private int timeoutSeconds;

	private JocketCloseReason closeReason;

	private Map<String, String> parameters = new HashMap<>();

	private Map<String, Object> attributes = new HashMap<>();

	public JocketSession()
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

	public String getRequestPath()
	{
		return requestPath;
	}

	public void setRequestPath(String requestPath)
	{
		this.requestPath = requestPath;
	}

	public Class<? extends JocketEndpoint> getEndpointClass()
	{
		return JocketDeployer.getEndpointClass(endpointClassName);
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

	public boolean isOpen()
	{
		return STATUS_OPEN.equals(getStatus());
	}

	public void setStatus(String status)
	{
		this.status = status;
		if (STATUS_CLOSED.equals(status)) {
			this.closeTime = System.currentTimeMillis();
		}
	}

	public boolean isConnected()
	{
		return connected;
	}

	public void setConnected(boolean connected)
	{
		this.connected = connected;
	}

	public boolean isWaitingHeartbeat()
	{
		return waitingHeartbeat;
	}

	public void setWaitingHeartbeat(boolean waitingHeartbeat)
	{
		this.waitingHeartbeat = waitingHeartbeat;
	}

	public long getStartTime()
	{
		return startTime;
	}

	public void setStartTime(long startTime)
	{
		this.startTime = startTime;
	}

	public long getCloseTime()
	{
		return closeTime;
	}

	public void setCloseTime(long closeTime)
	{
		this.closeTime = closeTime;
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

	public JocketCloseReason getCloseReason()
	{
		return closeReason;
	}

	public void setCloseReason(JocketCloseReason closeReason)
	{
		this.closeReason = closeReason;
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

	public void setAttributes(Map<String, Object> attributes)
	{
		this.attributes.clear();
		if (attributes != null) {
			this.attributes.putAll(attributes);
		}
	}

	public void send(Object data)
	{
		send(data, null);
	}

	public void send(Object data, String name)
	{
		JocketPacket packet = new JocketPacket(JocketPacket.TYPE_MESSAGE, name, data);
		JocketQueueManager.publishMessage(id, packet);
	}
	
	public boolean isBroken()
	{
		//TODO check closed sessions
		long brokenMillis; 
		if (TRANSPORT_WEBSOCKET.equals(getTransport())) {
			brokenMillis = JocketConstant.HEARTBEAT_INTERVAL + 20_000;
		}
		else {
			brokenMillis = JocketConstant.POLLING_INTERVAL + 20_000;
		}
		long heartbeatTime = Math.max(getLastHeartbeatTime(), getStartTime());
		return heartbeatTime + brokenMillis < System.currentTimeMillis();
	}
}
