package com.jeedsoft.jocket.storage.redis;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.connection.JocketCloseReason;
import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.connection.JocketSessionManager;
import com.jeedsoft.jocket.util.JocketJsonUtil;

public class JocketRedisSession extends JocketSession
{
	private static final Logger logger = LoggerFactory.getLogger(JocketRedisSession.class);

	private JocketRedisSessionStore store = (JocketRedisSessionStore)JocketSessionManager.getStore();

	private static final String KEY_ID 					= "id";
	private static final String KEY_REQUEST_PATH		= "requestPath";
	private static final String KEY_ENDPOINT_CLASS		= "endpointClass";
	private static final String KEY_HTTP_SESSION_ID		= "httpSessionId";
	private static final String KEY_TRANSPORT			= "transport";
	private static final String KEY_STATUS				= "status";
	private static final String KEY_CONNECTED			= "connected";
	private static final String KEY_WAITING_HEARTBEAT	= "waitingHeartbeat";
	private static final String KEY_START_TIME			= "startTime";
	private static final String KEY_CLOSE_TIME			= "closeTime";
	private static final String KEY_LAST_HEARTBEAT_TIME	= "lastHeartbeatTime";
	private static final String KEY_LAST_MESSAGE_TIME	= "lastMessageTime";
	private static final String KEY_TIMEOUT_SECONDS		= "timeoutSeconds";
	private static final String KEY_PARAMETERS			= "parameters";
	private static final String KEY_CLOSE_REASON		= "closeReason";

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
		if (STATUS_CLOSED.equals(status)) {
			store.setBaseData(id, KEY_CLOSE_TIME, System.currentTimeMillis() + "");
		}
	}

	@Override
	public boolean isConnected()
	{
		return Boolean.parseBoolean(store.getBaseData(id, KEY_CONNECTED));
	}

	@Override
	public void setConnected(boolean connected)
	{
		store.setBaseData(id, KEY_CONNECTED, Boolean.toString(connected));
	}

	@Override
	public boolean isWaitingHeartbeat()
	{
		return Boolean.parseBoolean(store.getBaseData(id, KEY_WAITING_HEARTBEAT));
	}

	@Override
	public void setWaitingHeartbeat(boolean waitingHeartbeat)
	{
		store.setBaseData(id, KEY_WAITING_HEARTBEAT, Boolean.toString(waitingHeartbeat));
	}

	@Override
	public long getCloseTime()
	{
		return store.getBaseDataAsLong(id, KEY_CLOSE_TIME);
	}

	@Override
	public void setCloseTime(long closeTime)
	{
		store.setBaseData(id, KEY_CLOSE_TIME, closeTime + "");
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
	public JocketCloseReason getCloseReason()
	{
		return JocketCloseReason.parse(store.getBaseData(id, KEY_CLOSE_REASON));
	}

	@Override
	public void setCloseReason(JocketCloseReason closeReason)
	{
		store.setBaseData(id, KEY_CLOSE_REASON, closeReason.toString());
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
		session.setRequestPath(map.get(KEY_REQUEST_PATH));
		session.setEndpointClassName(map.get(KEY_ENDPOINT_CLASS));
		session.setHttpSessionId(map.get(KEY_HTTP_SESSION_ID));
		session.setParameters(JocketJsonUtil.toStringMap(new JSONObject(map.get(KEY_PARAMETERS))));
		session.setStartTime(Long.parseLong(map.get(KEY_START_TIME)));
		return session;
	}
	
	public static JocketSession toLocalSession(Map<String, String> baseData, Map<String, Object> attributes)
	{
		JocketSession session = new JocketSession();
		session.setId(baseData.get(KEY_ID));
		session.setRequestPath(baseData.get(KEY_REQUEST_PATH));
		session.setEndpointClassName(baseData.get(KEY_ENDPOINT_CLASS));
		session.setHttpSessionId(baseData.get(KEY_HTTP_SESSION_ID));
		session.setTransport(baseData.get(KEY_TRANSPORT));
		session.setStatus(baseData.get(KEY_STATUS));
		session.setConnected("true".equals(baseData.get(KEY_CONNECTED)));
		session.setWaitingHeartbeat("true".equals(baseData.get(KEY_WAITING_HEARTBEAT)));
		session.setStartTime(Long.parseLong(baseData.get(KEY_START_TIME)));
		session.setCloseTime(Long.parseLong(baseData.get(KEY_CLOSE_TIME)));
		session.setLastHeartbeatTime(Long.parseLong(baseData.get(KEY_LAST_HEARTBEAT_TIME)));
		session.setLastMessageTime(Long.parseLong(baseData.get(KEY_LAST_MESSAGE_TIME)));
		session.setTimeoutSeconds(Integer.parseInt(baseData.get(KEY_TIMEOUT_SECONDS)));
		session.setParameters(JocketJsonUtil.toStringMap(new JSONObject(baseData.get(KEY_PARAMETERS))));
		session.setCloseReason(JocketCloseReason.parse(baseData.get(KEY_CLOSE_REASON)));
		session.setAttributes(attributes);
		return session;
	}

	public static Map<String, String> toRedisHash(JocketSession session)
	{
		Map<String, String> map = new HashMap<>();
		DateFormat df = logger.isDebugEnabled() ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS") : null;
		put(map, KEY_ID, session.getId());
		put(map, KEY_REQUEST_PATH, session.getRequestPath());
		put(map, KEY_ENDPOINT_CLASS, session.getEndpointClassName());
		put(map, KEY_HTTP_SESSION_ID, session.getHttpSessionId());
		put(map, KEY_TRANSPORT, session.getTransport());
		put(map, KEY_STATUS, session.getStatus());
		put(map, KEY_CONNECTED, session.isConnected());
		put(map, KEY_WAITING_HEARTBEAT, session.isWaitingHeartbeat());
		put(map, KEY_START_TIME, session.getStartTime(), df);
		put(map, KEY_CLOSE_TIME, session.getCloseTime(), df);
		put(map, KEY_LAST_HEARTBEAT_TIME, session.getLastHeartbeatTime(), df);
		put(map, KEY_LAST_MESSAGE_TIME, session.getLastMessageTime(), df);
		put(map, KEY_TIMEOUT_SECONDS, session.getTimeoutSeconds(), null);
		put(map, KEY_PARAMETERS, new JSONObject(session.getParameters()));
		put(map, KEY_CLOSE_REASON, session.getCloseReason());
		return map;
	}

	private static void put(Map<String, String> map, String key, String value)
	{
		if (value != null) {
			map.put(key, value);
		}
	}

	private static void put(Map<String, String> map, String key, long value, DateFormat df)
	{
		if (value != 0) {
			map.put(key, value + "");
			if (df != null) {
				map.put(key + "$", df.format(value));
			}
		}
	}

	private static void put(Map<String, String> map, String key, boolean value)
	{
		if (value) {
			map.put(key, Boolean.toString(value));
		}
	}

	private static void put(Map<String, String> map, String key, Object value)
	{
		if (value != null) {
			map.put(key, value.toString());
		}
	}
}
