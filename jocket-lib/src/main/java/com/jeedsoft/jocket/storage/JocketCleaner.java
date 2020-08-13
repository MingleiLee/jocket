package com.jeedsoft.jocket.storage;

import com.jeedsoft.jocket.connection.*;
import com.jeedsoft.jocket.endpoint.JocketEndpointRunner;
import com.jeedsoft.jocket.message.JocketQueueManager;
import com.jeedsoft.jocket.util.JocketClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class JocketCleaner
{
	private static final Logger logger = LoggerFactory.getLogger(JocketCleaner.class);
	
	private static final long interval = 10_000;
	
	private static Timer timer;
	
	private static long lastStatMillis = 0;
	
	public static synchronized void start()
	{
		if (timer != null) {
			timer.cancel();
		}
		timer = new Timer();
		timer.schedule(new CleanTask(), interval);
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
			try {
				Thread.currentThread().setName("JocketCleaner");
				logger.trace("[Jocket] Run clean task.");
				if (timer == null || !JocketSessionManager.applySchedule()) {
					return;
				}
				List<JocketSession> brokenSessions = JocketSessionManager.checkStore();
				for (JocketSession session: brokenSessions) {
                    JocketConnectionManager.remove(session.getId());
                    if (!session.isClosed()) {
                        int code = JocketCloseCode.NO_HEARTBEAT;
                        JocketCloseReason reason = new JocketCloseReason(code, "no new ping");
                        JocketEndpointRunner.doClose(session, reason);
                    }
				}
				if (logger.isDebugEnabled()) {
					if (!brokenSessions.isEmpty()) {
						logger.debug("[Jocket] Removed {} corrupted sessions.", brokenSessions.size());
					}
					long now = JocketClock.now();
					long interval = 60_000;
					if (now / interval != lastStatMillis / interval) {
						lastStatMillis = now;
						Object[] args = {
							JocketSessionManager.size(),
							JocketQueueManager.getQueueCount(),
							JocketConnectionManager.size(),
						};
						logger.debug("[Jocket] Statistics: session={}, queue={}, local connection={}", args);
					}
				}
			}
			catch (Throwable e) {
				logger.error("[Jocket] Failed to run clean task.", e);
			}
			finally {
				synchronized (JocketCleaner.class) {
					if (timer != null) {
						timer.schedule(new CleanTask(), interval);
					}
				}
			}
		}
	}
}
