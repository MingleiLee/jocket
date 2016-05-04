package com.jeedsoft.jocket.storage.redis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jeedsoft.jocket.connection.JocketStub;
import com.jeedsoft.jocket.connection.JocketStubStore;

public class JocketRedisStubStore implements JocketStubStore
{
	private long scheduleSerial = 0;

	@Override
	public void add(JocketStub stub)
	{
		String key = getBaseKey(stub.getId());
		JocketRedisExecutor.hmset(key, stub.toMap());
	}

	@Override
	public JocketStub remove(String id)
	{
		String baseKey = getBaseKey(id);
		String attrKey = getAttrKey(id);
		Map<String, String> baseData = JocketRedisExecutor.hgetAll(baseKey);
		Map<String, String> attrData = JocketRedisExecutor.hgetAll(attrKey);
		JocketStub stub = null;
		if (!baseData.isEmpty()) {
			Map<String, Object> attributes = new HashMap<>();
			for (String k: attrData.keySet()) {
				Object v = JocketRedisObjectNotation.toObject(attrData.get(k));
				attributes.put(k, v);
			}
			stub = new JocketStub(baseData, attributes);
		}
		if (!baseData.isEmpty() || !attrData.isEmpty()) {
			JocketRedisExecutor.del(baseKey, attrKey);
		}
		return stub;
	}

	@Override
	public JocketStub get(String id)
	{
		String key = getBaseKey(id);
		Map<String, String> map = JocketRedisExecutor.hgetAll(key);
		return JocketRedisStub.fromMap(map);
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
	public synchronized List<JocketStub> checkStore()
	{
		List<JocketStub> brokenStubs = new ArrayList<>();
		String pattern = getBaseKeyPattern();
		for (String key: JocketRedisExecutor.keys(pattern)) {
			Map<String, String> map = JocketRedisExecutor.hgetAll(key);
			if (!map.isEmpty()) {
				JocketStub stub = JocketRedisStub.fromMap(map);
				if (stub.isBroken()) {
					JocketStub localStub = remove(stub.getId());
					if (localStub != null) {
						brokenStubs.add(localStub);
					}
				}
			}
		}
		return brokenStubs;
	}

	@Override
	public synchronized int size()
	{
		String pattern = getBaseKeyPattern();
		return JocketRedisExecutor.keys(pattern).size();
	}

	@Override
	public synchronized boolean contains(String id)
	{
		String key = getBaseKey(id);
		return JocketRedisExecutor.exists(key); //TODO optimize for performance
	}

	public String getBaseData(String id, String field)
	{
		String key = getBaseKey(id);
		return JocketRedisExecutor.hget(key, field);
	}

	public int getBaseDataAsInt(String id, String field)
	{
		String key = getBaseKey(id);
		String value = JocketRedisExecutor.hget(key, field);
		return value == null ? 0 : Integer.parseInt(value);
	}

	public long getBaseDataAsLong(String id, String field)
	{
		String key = getBaseKey(id);
		String value = JocketRedisExecutor.hget(key, field);
		return value == null ? 0 : Long.parseLong(value);
	}

	public boolean setBaseData(String id, String field, String value)
	{
		String key = getBaseKey(id);
		return JocketRedisExecutor.hsetOnKeyExist(key, field, value);
	}

	public <T> T getAttribute(String id, String field)
	{
		String key = getAttrKey(id);
		String text = JocketRedisExecutor.hget(key, field);
		if (text == null) {
			return null;
		}
		return JocketRedisObjectNotation.toObject(text);
	}

	public <T> void setAttribute(String id, String field, T value)
	{
		String key = getAttrKey(id);
		String text = JocketRedisObjectNotation.toString(value);
		JocketRedisExecutor.hset(key, field, text);
	}

	private String getBaseKey(String id)
	{
		return JocketRedisKey.PREFIX_STUB + ":" + id + ":" + JocketRedisKey.POSTFIX_BASE;
	}

	private String getAttrKey(String id)
	{
		return JocketRedisKey.PREFIX_STUB + ":" + id + ":" + JocketRedisKey.POSTFIX_ATTR;
	}
	
	private String getBaseKeyPattern()
	{
		return JocketRedisKey.PREFIX_STUB + ":*:" + JocketRedisKey.POSTFIX_BASE;
	}
}
