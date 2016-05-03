package com.jeedsoft.jocket.storage.redis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.jeedsoft.jocket.connection.JocketStub;
import com.jeedsoft.jocket.connection.JocketStubStore;
import com.jeedsoft.jocket.connection.impl.JocketPollingConnection;
import com.jeedsoft.jocket.endpoint.JocketAbstractEndpoint;
import com.jeedsoft.jocket.util.JocketJsonUtil;
import com.jeedsoft.jocket.util.ReflectUtil;

public class JocketRedisStubStore implements JocketStubStore
{
	private long scheduleSerial = 0;

	@Override
	public void add(JocketStub stub)
	{
		String key = getMainKey(stub.getId());
		JocketRedisExecutor.hmset(key, stub.toMap());
	}

	@Override
	public void remove(String id)
	{
		String[] keys = {getMainKey(id), getPropKey(id)};
		JocketRedisExecutor.del(keys);
	}

	@Override
	public JocketStub get(String id)
	{
		String key = getMainKey(id);
		Map<String, String> map = JocketRedisExecutor.hgetAll(key);
		return JocketStub.fromMap(map);
	}

	@Override
	public int getStatus(String id)
	{
		String key = getMainKey(id);
		String value = JocketRedisExecutor.hget(key, JocketStub.KEY_STATUS);
		return value == null ? JocketStub.STATUS_INVALID : Integer.parseInt(value);
	}

	@Override
	public void setStatus(String id, int status)
	{
		String key = getMainKey(id);
		JocketRedisExecutor.hsetOnKeyExist(key, JocketStub.KEY_STATUS, status + "");
	}

	public long getLastPolling(String id)
	{
		String key = getMainKey(id);
		String value = JocketRedisExecutor.hget(key, JocketStub.KEY_LAST_POLLING);
		return value == null ? 0 : Long.parseLong(value);
	}

	public void setLastPolling(String id, long lastPolling)
	{
		String key = getMainKey(id);
		JocketRedisExecutor.hsetOnKeyExist(key, JocketStub.KEY_LAST_POLLING, lastPolling + "");
	}

	@Override
	public Class<? extends JocketAbstractEndpoint> getHandlerClass(String id)
	{
		String key = getMainKey(id);
		String value = JocketRedisExecutor.hget(key, JocketStub.KEY_HANDLER_CLASS);
		if (value == null) {
			return null;
		}
		return ReflectUtil.getClass(value);
	}

	@Override
	public String getParameter(String id, String key)
	{
		return getParameterMap(id).get(key);
	}

	@Override
	public Map<String, String> getParameterMap(String id)
	{
		String key = getMainKey(id);
		String value = JocketRedisExecutor.hget(key, JocketStub.KEY_PARAMETERS);
		return value == null ? null : JocketJsonUtil.toStringMap(new JSONObject(value));
	}

	@Override
	public synchronized <T> T getUserProperty(String id, String field)
	{
		String key = getPropKey(id);
		String value = JocketRedisExecutor.hget(key, field);
		if (value == null) {
			return null;
		}
		return JocketRedisObjectNotation.toObject(value);
	}

	private Map<String, Object> getUserProperties(String id)
	{
		String key = getPropKey(id);
		Map<String, String> map = JocketRedisExecutor.hgetAll(key);
		Map<String, Object> properties = new HashMap<>();
		for (String k : map.keySet()) {
			Object v = JocketRedisObjectNotation.toObject(map.get(k));
			properties.put(k, v);
		}
		return properties;
	}

	@Override
	public synchronized <T> void setUserProperty(String id, String field, T value)
	{
		String key = getPropKey(id);
		String text = JocketRedisObjectNotation.toString(value);
		JocketRedisExecutor.hsetOnKeyExist(key, field, text);
	}

	@Override
	public boolean applySchedule()
	{
		String key = JocketRedisKey.TIMER;
		String field = JocketRedisKey.FIELD_CLEANER;
		JocketCasResult<Long> cas = JocketRedisExecutor.hcheckAndIncr(key, field, scheduleSerial);
		scheduleSerial = cas.getValue();
		return cas.isSuccess();
	}

	@Override
	public synchronized List<JocketStub> checkCorruption()
	{
		List<JocketStub> corruptedStubs = new ArrayList<>();
		long now = System.currentTimeMillis();
		String pattern = getMainKeyPattern();
		for (String key: JocketRedisExecutor.keys(pattern)) {
			int end = key.lastIndexOf(':');
			int start = key.lastIndexOf(':', end - 1);
			String id = key.substring(start + 1, end);
			long lastPolling = getLastPolling(id);
			if (lastPolling > 0 && lastPolling + JocketPollingConnection.POLLING_INTERVAL + 20_000 < now) {
				JocketStub stub = get(id);
				stub.setUserProperties(getUserProperties(id));
				corruptedStubs.add(stub);
			}
		}
		for (JocketStub stub: corruptedStubs) {
			remove(stub.getId());
		}
		return corruptedStubs;
	}

	@Override
	public synchronized int size()
	{
		String pattern = getMainKeyPattern();
		return JocketRedisExecutor.keys(pattern).size();
	}

	@Override
	public synchronized boolean contains(String id)
	{
		String key = getMainKey(id);
		return JocketRedisExecutor.exists(key); //TODO optimize for performance
	}

	private String getMainKey(String id)
	{
		return JocketRedisKey.PREFIX_STUB + ":" + id + ":" + JocketRedisKey.POSTFIX_MAIN;
	}

	private String getPropKey(String id)
	{
		return JocketRedisKey.PREFIX_STUB + ":" + id + ":" + JocketRedisKey.POSTFIX_PROP;
	}
	
	private String getMainKeyPattern()
	{
		return JocketRedisKey.PREFIX_STUB + ":*:" + JocketRedisKey.POSTFIX_MAIN;
	}
}
