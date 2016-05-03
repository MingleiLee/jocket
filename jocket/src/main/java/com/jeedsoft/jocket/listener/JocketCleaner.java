package com.jeedsoft.jocket.listener;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.connection.JocketConnectionManager;
import com.jeedsoft.jocket.connection.JocketStub;
import com.jeedsoft.jocket.connection.JocketStubManager;
import com.jeedsoft.jocket.connection.impl.JocketPollingConnection;
import com.jeedsoft.jocket.endpoint.JocketCloseReason;
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
			if (!JocketStubManager.applySchedule()) {
				return;
			}
			List<JocketStub> corruptedStubs = JocketStubManager.checkCorruption();
			for (JocketStub stub: corruptedStubs) {
				JocketQueueManager.unsubscribe(stub.getId(), true);
				int code = JocketCloseReason.CLOSED_ABNORMALLY;
				JocketCloseReason reason = new JocketCloseReason(code, "no new polling");
				JocketEndpointRunner.doClose(new JocketPollingConnection(stub), reason);
			}
			if (logger.isDebugEnabled()) {
				if (!corruptedStubs.isEmpty()) {
					logger.debug("[Jocket] Removed {} corrupted connections.", corruptedStubs.size());
				}
				Object[] args = {
					JocketStubManager.size(),
					JocketQueueManager.getSubscriberCount(),
					JocketQueueManager.getQueueCount(),
					JocketConnectionManager.size()
				};
				logger.debug("[Jocket] Statistics: stub={}, subscriber={}, queue={}, local connection={}", args);
			}
		}
	}
}
