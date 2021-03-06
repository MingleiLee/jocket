package com.jeedsoft.jocket.storage.redis;

import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.connection.JocketSessionStore;
import com.jeedsoft.jocket.util.JocketStringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class JocketRedisSessionStore implements JocketSessionStore
{
	private static final Logger logger = LoggerFactory.getLogger(JocketRedisSessionStore.class);
	
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
		if (session != null && !JocketStringUtil.isEmpty(session.getUserId())) {
			String key = getUserKey(session.getUserId());
			JocketRedisExecutor.srem(key, id);
		}
		if (session != null && !JocketStringUtil.isEmpty(session.getOnlineUserId())) {
			String key = getOnlineUserKey(session.getOnlineUserId());
			JocketRedisExecutor.del(key);
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
	public List<String> getAllSessionIds()
	{
		List<String> list = new ArrayList<>();
		for (String key: JocketRedisExecutor.keys(getBaseKeyPattern())) {
			list.add(extractId(key));
		}
		return list;
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
		JocketSession session = get(id);
		return session != null && !session.isBroken();
	}

	@Override
	public void updateUserId(String id, String oldUserId, String newUserId)
	{
		if (!JocketStringUtil.isEmpty(oldUserId)) {
			String key = getUserKey(oldUserId);
			JocketRedisExecutor.srem(key, id);
		}
		if (!JocketStringUtil.isEmpty(newUserId)) {
			String key = getUserKey(newUserId);
			JocketRedisExecutor.sadd(key, id);
		}
	}

	@Override
	public void updateOnlineUserId(String id, String oldOnlineUserId, String newOnlineUserId)
	{
		if (!JocketStringUtil.isEmpty(oldOnlineUserId)) {
			String key = getOnlineUserKey(oldOnlineUserId);
			JocketRedisExecutor.del(key);
		}
		if (!JocketStringUtil.isEmpty(newOnlineUserId)) {
			String key = getOnlineUserKey(newOnlineUserId);
			JocketRedisExecutor.set(key, id);
		}
	}

	@Override
	public List<JocketSession> getUserSessions(String userId)
	{
		List<JocketSession> sessions = new ArrayList<>();
		String key = getUserKey(userId);
		Set<String> sessionIds = JocketRedisExecutor.smembers(key);
		if (sessionIds != null) {
			for (String sessionId: sessionIds) {
				JocketSession session = get(sessionId);
				if (session.isOpen()) {
					sessions.add(session);
				}
			}
		}
		return sessions;
	}

	@Override
	public JocketSession getOnlineUserSession(String onlineUserId)
	{
		String key = getOnlineUserKey(onlineUserId);
		String sessionId = JocketRedisExecutor.get(key);
		if (!JocketStringUtil.isEmpty(sessionId)) {
			JocketSession session = get(sessionId);
			if (session.isOpen()) {
				return session;
			}
		}
		return null;
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

	public boolean hasAttribute(String id, String field)
	{
		String key = getAttrKey(id);
		return JocketRedisExecutor.hexists(key, field);
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
		logger.trace("[Jocket] Checking store...");
		List<JocketSession> brokenSessions = new ArrayList<>();
		Set<String> sessionIds = new HashSet<>();
		for (String key: JocketRedisExecutor.keys(getBaseKeyPattern())) {
			Map<String, String> baseData = JocketRedisExecutor.hgetAll(key);
			if (!baseData.isEmpty()) {
				JocketSession session = JocketRedisSession.fromMap(baseData);
				if (session.isBroken()) {
					JocketSession localSession = remove(session.getId());
					if (localSession != null) {
						brokenSessions.add(localSession);
					}
				}
				else {
					sessionIds.add(session.getId());
				}
			}
		}
		logger.trace("[Jocket] {} alive sessions, {} broken sessions.", sessionIds.size(), brokenSessions.size());
		for (String key: JocketRedisExecutor.keys(getSessionKeyPattern())) {
			String sessionId = extractId(key);
			if (!sessionIds.contains(sessionId)) {
				logger.trace("[Jocket] Remove trash data: key={}", key);
				JocketRedisExecutor.del(key);
			}
		}
		for (String key: JocketRedisExecutor.keys(getUserKeyPattern())) {
			Set<String> set = JocketRedisExecutor.smembers(key);
			if (set != null) {
				for (String sessionId: set) {
					if (!sessionIds.contains(sessionId)) {
						logger.trace("[Jocket] Remove from user session set: ukey={}, sid={}", key, sessionId);
						JocketRedisExecutor.srem(key, sessionId);
					}
				}
			}
		}
		for (String key: JocketRedisExecutor.keys(getOnlineUserKeyPattern())) {
			String sessionId = JocketRedisExecutor.get(key);
			if (!sessionIds.contains(sessionId)) {
				logger.trace("[Jocket] Remove online user entry: oukey={}, sid={}", key, sessionId);
				JocketRedisExecutor.del(key);
			}
		}
		return brokenSessions;
	}

	private String getBaseKey(String id)
	{
		return JocketRedisKey.PREFIX_SESSION + ":" + id + ":" + JocketRedisKey.POSTFIX_BASE;
	}

	private String getAttrKey(String id)
	{
		return JocketRedisKey.PREFIX_SESSION + ":" + id + ":" + JocketRedisKey.POSTFIX_ATTR;
	}

	private String getUserKey(String userId)
	{
		return JocketRedisKey.PREFIX_USER + ":" + userId;
	}

	private String getOnlineUserKey(String onlineUserId)
	{
		return JocketRedisKey.PREFIX_ONLINE_USER + ":" + onlineUserId;
	}

	private String getSessionKeyPattern()
	{
		return JocketRedisKey.PREFIX_SESSION + ":*";
	}

	private String getBaseKeyPattern()
	{
		return JocketRedisKey.PREFIX_SESSION + ":*:" + JocketRedisKey.POSTFIX_BASE;
	}

	private String getUserKeyPattern()
	{
		return JocketRedisKey.PREFIX_USER + ":*";
	}

	private String getOnlineUserKeyPattern()
	{
		return JocketRedisKey.PREFIX_ONLINE_USER + ":*";
	}

	private String extractId(String key)
	{
		int start = JocketRedisKey.PREFIX_SESSION.length() + 1;
		int end = key.indexOf(':', start);
		return key.substring(start, end == -1 ? key.length() : end);
	}
}
