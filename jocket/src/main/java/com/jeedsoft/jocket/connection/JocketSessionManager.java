package com.jeedsoft.jocket.connection;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.endpoint.JocketEndpointRunner;
import com.jeedsoft.jocket.message.JocketPacket;
import com.jeedsoft.jocket.message.JocketQueueManager;
import com.jeedsoft.jocket.storage.local.JocketLocalSessionStore;

public class JocketSessionManager
{
	private static final Logger logger = LoggerFactory.getLogger(JocketSessionManager.class);

	private static JocketSessionStore store = new JocketLocalSessionStore();

	public static JocketSessionStore getStore()
	{
		return store;
	}

	public static void setStore(JocketSessionStore store)
	{
		JocketSessionManager.store = store;
	}

	public static String add(JocketSession session)
	{
		session.setId(UUID.randomUUID().toString());
		session.setStartTime(System.currentTimeMillis());
		store.add(session);
		return session.getId();
	}

	public static JocketSession remove(String id)
	{
		JocketQueueManager.removeSubscriber(id, true);
		return store.remove(id);
	}

	public static JocketSession get(String id)
	{
		return store.get(id);
	}
	
	public static List<JocketSession> checkStore()
	{
		return store.checkStore();
	}
	
	public static int size()
	{
		return store.size();
	}

	public static boolean contains(String id)
	{
		return store.contains(id);
	}

	public static boolean applySchedule()
	{
		return store.applySchedule();
	}

	public static void open(String id)
	{
		JocketSession session = store.get(id);
		if (session != null) {
			session.setStatus(JocketSession.STATUS_OPEN);
			JocketQueueManager.publishEvent(id, new JocketPacket(JocketPacket.TYPE_OPEN));
			JocketEndpointRunner.doOpen(session);
			if (logger.isDebugEnabled()) {
				Object[] args = {id, session.getTransport(), session.getRequestPath()};
				logger.debug("[Jocket] Jocket opened: sid={}, transport={}, path={}", args);
			}
		}
	}

	public static void close(String id, JocketCloseReason reason, boolean post)
	{
		JocketSession session = post ? get(id) : remove(id);
		if (session != null) {
			session.setStatus(JocketSession.STATUS_CLOSED);
			session.setCloseTime(System.currentTimeMillis());
			if (post) {
				session.setCloseReason(reason);
				JocketQueueManager.publishEvent(id, new JocketPacket(JocketPacket.TYPE_CLOSE, null, reason));
			}
			JocketEndpointRunner.doClose(session, reason);
		}
	}
}
