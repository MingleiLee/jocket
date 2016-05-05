package com.jeedsoft.jocket.storage.redis;

import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.connection.JocketSessionManager;
import com.jeedsoft.jocket.util.JocketJsonUtil;

public class JocketRedisSession extends JocketSession
{
	private static final Logger logger = LoggerFactory.getLogger(JocketRedisSession.class);

	private JocketRedisSessionStore store = (JocketRedisSessionStore)JocketSessionManager.getStore();

	@Override
	public String getTransport()
	{
		return store.getBaseData(id, KEY_TRANSPORT);
	}

	@Override
	public void setTransport(String transport)
	{
		store.setBaseData(id, KEY_TRANSPORT, transport);
	}

	@Override
	public String getStatus()
	{
		return store.getBaseData(id, KEY_STATUS);
	}

	@Override
	public void setStatus(String status)
	{
		logger.trace("[Jocket] set session status: sid={}, status={}", id, status);
		store.setBaseData(id, KEY_STATUS, status);
	}

	@Override
	public long getLastHeartbeatTime()
	{
		return store.getBaseDataAsLong(id, KEY_LAST_HEARTBEAT_TIME);
	}

	@Override
	public void setLastHeartbeatTime(long lastHeartbeatTime)
	{
		store.setBaseData(id, KEY_LAST_HEARTBEAT_TIME, lastHeartbeatTime + "");
	}
	
	@Override
	public long getLastMessageTime()
	{
		return store.getBaseDataAsLong(id, KEY_LAST_MESSAGE_TIME);
	}

	@Override
	public void setLastMessageTime(long lastMessageTime)
	{
		store.setBaseData(id, KEY_LAST_MESSAGE_TIME, lastMessageTime + "");
	}

	@Override
	public int getTimeoutSeconds()
	{
		return store.getBaseDataAsInt(id, KEY_TIMEOUT_SECONDS);
	}

	@Override
	public void setTimeoutSeconds(int timeoutSeconds)
	{
		store.setBaseData(id, KEY_TIMEOUT_SECONDS, timeoutSeconds + "");
	}

	@Override
	public <T> T getAttribute(String key)
	{
		return store.getAttribute(id, key);
	}

	@Override
	public <T> void setAttribute(String key, T value)
	{
		store.setAttribute(id, key, value);
	}
	
	public static JocketRedisSession fromMap(Map<String, String> map)
	{
		JocketRedisSession session = new JocketRedisSession();
		session.setId(map.get(KEY_ID));
		session.setEndpointClassName(map.get(KEY_ENDPOINT_CLASS_NAME));
		session.setHttpSessionId(map.get(KEY_HTTP_SESSION_ID));
		session.setParameters(JocketJsonUtil.toStringMap(new JSONObject(map.get(KEY_PARAMETERS))));
		session.setStartTime(Long.parseLong(map.get(KEY_START_TIME)));
		return session;
	}
}
