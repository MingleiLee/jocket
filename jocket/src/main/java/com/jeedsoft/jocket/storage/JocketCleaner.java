package com.jeedsoft.jocket.storage;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.connection.JocketCloseReason;
import com.jeedsoft.jocket.connection.JocketConnectionManager;
import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.connection.JocketSessionManager;
import com.jeedsoft.jocket.endpoint.JocketEndpointRunner;
import com.jeedsoft.jocket.event.JocketQueueManager;

public class JocketCleaner
{
	private static final Logger logger = LoggerFactory.getLogger(JocketCleaner.class);
	
	private static final long interval = 10_000;
	
	private static Timer timer;

	public static synchronized void start()
	{
		if (timer != null) {
			timer.cancel();
		}
		timer = new Timer();
		timer.schedule(new CleanTask(), interval, interval);
	}

	public static synchronized void stop()
	{
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}
	
	private static class CleanTask extends TimerTask
	{
		@Override
		public void run()
		{
			if (!JocketSessionManager.applySchedule()) {
				return;
			}
			List<JocketSession> brokenSessions = JocketSessionManager.checkStore();
			for (JocketSession session: brokenSessions) {
				JocketQueueManager.unsubscribe(session.getId(), true);
				int code = JocketCloseReason.CLOSED_ABNORMALLY;
				JocketCloseReason reason = new JocketCloseReason(code, "no new polling");
				JocketEndpointRunner.doClose(session, reason);
			}
			if (logger.isDebugEnabled()) {
				if (!brokenSessions.isEmpty()) {
					logger.debug("[Jocket] Removed {} corrupted sessions.", brokenSessions.size());
				}
				Object[] args = {
					JocketSessionManager.size(),
					JocketQueueManager.getSubscriberCount(),
					JocketQueueManager.getQueueCount(),
					JocketConnectionManager.size()
				};
				logger.debug("[Jocket] Statistics: session={}, queue={}, local connection={}, local subscriber={}", args);
			}
		}
	}
}
