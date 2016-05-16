package com.jeedsoft.jocket.storage.redis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.connection.JocketSessionStore;

public class JocketRedisSessionStore implements JocketSessionStore
{
	private long scheduleSerial = 0;

	@Override
	public void add(JocketSession session)
	{
		String key = getBaseKey(session.getId());
		JocketRedisExecutor.hmset(key, JocketRedisSession.toRedisHash(session));
	}

	@Override
	public JocketSession remove(String id)
	{
		String baseKey = getBaseKey(id);
		String attrKey = getAttrKey(id);
		Map<String, String> baseData = JocketRedisExecutor.hgetAll(baseKey);
		Map<String, String> attrData = JocketRedisExecutor.hgetAll(attrKey);
		JocketSession session = null;
		if (!baseData.isEmpty()) {
			Map<String, Object> attributes = new HashMap<>();
			for (String k: attrData.keySet()) {
				Object v = JocketRedisObjectNotation.toObject(attrData.get(k));
				attributes.put(k, v);
			}
			session = JocketRedisSession.toLocalSession(baseData, attributes);
		}
		if (!baseData.isEmpty() || !attrData.isEmpty()) {
			JocketRedisExecutor.del(baseKey, attrKey);
		}
		return session;
	}

	@Override
	public JocketSession get(String id)
	{
		String key = getBaseKey(id);
		Map<String, String> map = JocketRedisExecutor.hgetAll(key);
		return map.isEmpty() ? null : JocketRedisSession.fromMap(map);
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
	public synchronized List<JocketSession> checkStore()
	{
		List<JocketSession> brokenSessions = new ArrayList<>();
		String pattern = getBaseKeyPattern();
		for (String key: JocketRedisExecutor.keys(pattern)) {
			Map<String, String> map = JocketRedisExecutor.hgetAll(key);
			if (!map.isEmpty()) {
				JocketSession session = JocketRedisSession.fromMap(map);
				if (session.isBroken()) {
					JocketSession localSession = remove(session.getId());
					if (localSession != null) {
						brokenSessions.add(localSession);
					}
				}
			}
		}
		return brokenSessions;
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
		return JocketRedisExecutor.exists(key); //TODO exclude closed sessions?
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
		return JocketRedisKey.PREFIX_SESSION + ":" + id + ":" + JocketRedisKey.POSTFIX_BASE;
	}

	private String getAttrKey(String id)
	{
		return JocketRedisKey.PREFIX_SESSION + ":" + id + ":" + JocketRedisKey.POSTFIX_ATTR;
	}
	
	private String getBaseKeyPattern()
	{
		return JocketRedisKey.PREFIX_SESSION + ":*:" + JocketRedisKey.POSTFIX_BASE;
	}
}
