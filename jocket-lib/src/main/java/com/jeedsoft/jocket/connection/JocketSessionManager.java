package com.jeedsoft.jocket.connection;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.endpoint.JocketEndpointRunner;
import com.jeedsoft.jocket.message.JocketPacket;
import com.jeedsoft.jocket.message.JocketQueueManager;
import com.jeedsoft.jocket.storage.local.JocketLocalSessionStore;
import com.jeedsoft.jocket.util.JocketClock;

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
		logger.debug("[Jocket] Session store class: {}", store.getClass().getName());
	}

	public static void add(JocketSession session)
	{
		store.add(session);
	}

	public static JocketSession remove(String id)
	{
        JocketConnectionManager.remove(id);
	    JocketQueueManager.clear(id);
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

	public static void close(String id, JocketCloseReason reason, boolean post)
	{
		JocketSession session = post ? get(id) : remove(id);
		if (session != null) {
			session.setStatus(JocketSession.STATUS_CLOSED);
			session.setCloseTime(JocketClock.now());
			if (post) {
				session.setCloseReason(reason);
				JocketQueueManager.publish(id, new JocketPacket(JocketPacket.TYPE_CLOSE, reason));
			}
			JocketEndpointRunner.doClose(session, reason);
		}
	}
}
