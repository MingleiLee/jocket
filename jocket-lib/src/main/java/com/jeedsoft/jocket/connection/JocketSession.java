package com.jeedsoft.jocket.connection;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.Jocket;
import com.jeedsoft.jocket.JocketService;
import com.jeedsoft.jocket.endpoint.JocketDeployer;
import com.jeedsoft.jocket.endpoint.JocketEndpoint;
import com.jeedsoft.jocket.message.JocketPacket;
import com.jeedsoft.jocket.message.JocketQueueManager;
import com.jeedsoft.jocket.util.JocketClock;
import com.jeedsoft.jocket.util.JocketStringUtil;

public class JocketSession
{
	private static final Logger logger = LoggerFactory.getLogger(JocketSession.class);

	private static final Logger checkLogger = LoggerFactory.getLogger("com.jeedsoft-jocket.session.CheckBroken");

	public static final String STATUS_NEW			= "new";
	public static final String STATUS_HANDSHAKING	= "handshaking";
	public static final String STATUS_OPEN			= "open";
	public static final String STATUS_CLOSED		= "closed";

	protected String id;

    private String clientId;

	private String requestPath;

	private String httpSessionId;
	
	private String endpointClassName;
	
	private String userId;

	private String onlineUserId;

	private String status;
	
	private long startTime;

	private long closeTime;

	private long lastHeartbeatTime;

	private long lastMessageTime;

	private int timeoutSeconds;

	private JocketCloseReason closeReason;

	private Map<String, String> parameters = new HashMap<>();

	private final Map<String, Object> attributes = new HashMap<>();

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

	public String getClientId()
    {
        return clientId;
    }

    public void setClientId(String clientId)
    {
        this.clientId = clientId;
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
	
	public String getUserId()
	{
		return userId;
	}

	public void setUserId(String userId)
	{
		String oldUserId = this.userId;
		setLocalUserId(userId);
		JocketSessionManager.getStore().updateUserId(id, oldUserId, this.userId);
	}

	public void setLocalUserId(String userId)
	{
		this.userId = JocketStringUtil.isEmpty(userId) ? null : userId;
	}

	public String getOnlineUserId()
	{
		return onlineUserId;
	}

	public void setOnlineUserId(String onlineUserId)
	{
		String oldOnlineUserId = this.onlineUserId;
		setLocalOnlineUserId(onlineUserId);
		JocketSessionManager.getStore().updateOnlineUserId(id, oldOnlineUserId, this.onlineUserId);
	}

	public void setLocalOnlineUserId(String onlineUserId)
	{
		this.onlineUserId = JocketStringUtil.isEmpty(onlineUserId) ? null : onlineUserId;
	}

	public String getStatus()
	{
		return status;
	}

	public void setStatus(String status)
	{
		logger.trace("[Jocket] Set status: sid={}, value={}", id, status);
		this.status = status;
		if (STATUS_CLOSED.equals(status)) {
			this.closeTime = JocketClock.now();
		}
	}

	public boolean isOpen()
	{
		return STATUS_OPEN.equals(getStatus());
	}

    public boolean isClosed()
    {
        return STATUS_CLOSED.equals(getStatus());
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

	public boolean hasAttribute(String key)
	{
		return attributes.containsKey(key);
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

	public void send(String name, Object data)
	{
		JocketPacket packet = new JocketPacket(JocketPacket.TYPE_MESSAGE, name, data);
		JocketQueueManager.publish(id, packet);
	}
	
	public void close(int code, String message)
	{
		Jocket.close(id, code, message);
	}
	
	/**
	 * Check if the session is broken (due to timeout)
	 */
	public boolean isBroken()
	{
		long brokenMillis = JocketService.getPingInterval() + JocketService.getPingTimeout();
		long heartbeatTime = Math.max(getLastHeartbeatTime(), getStartTime());
		long now = JocketClock.now();
		boolean broken = now - heartbeatTime > brokenMillis;
		if (checkLogger.isTraceEnabled()) {
			Object[] args = {id, heartbeatTime, brokenMillis, broken};
			checkLogger.trace("[Jocket] Check broken: sid={}, heartbeat={}, timeout={}, broken={}", args);
		}
		return broken;
	}
}
