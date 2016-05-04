package com.jeedsoft.jocket.storage.redis;

import java.util.Map;

import org.json.JSONObject;

import com.jeedsoft.jocket.connection.JocketStub;
import com.jeedsoft.jocket.connection.JocketStubManager;
import com.jeedsoft.jocket.util.JocketJsonUtil;

public class JocketRedisStub extends JocketStub
{
	private JocketRedisStubStore store = (JocketRedisStubStore)JocketStubManager.getStore();
	
	public JocketRedisStub()
	{
	}

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
	
	public static JocketRedisStub fromMap(Map<String, String> map)
	{
		JocketRedisStub stub = new JocketRedisStub();
		stub.setId(map.get(KEY_ID));
		stub.setEndpointClassName(map.get(KEY_ENDPOINT_CLASS_NAME));
		stub.setHttpSessionId(map.get(KEY_HTTP_SESSION_ID));
		stub.setParameters(JocketJsonUtil.toStringMap(new JSONObject(map.get(KEY_PARAMETERS))));
		stub.setStartTime(Long.parseLong(map.get(KEY_START_TIME)));
		return stub;
	}
}
