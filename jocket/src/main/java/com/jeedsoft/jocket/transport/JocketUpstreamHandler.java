package com.jeedsoft.jocket.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.Jocket;
import com.jeedsoft.jocket.connection.JocketCloseReason;
import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.connection.JocketSessionManager;
import com.jeedsoft.jocket.endpoint.JocketEndpointRunner;
import com.jeedsoft.jocket.message.JocketPacket;
import com.jeedsoft.jocket.message.JocketQueueManager;

public class JocketUpstreamHandler
{
	private static final Logger logger = LoggerFactory.getLogger(JocketUpstreamHandler.class);

	public static void handle(String sessionId, String text)
	{
		logger.trace("[Jocket] Handle upstream packet: sessionId={}, packet={}", sessionId, text);
		JocketSession session = JocketSessionManager.get(sessionId);
		if (session == null) {
			logger.error("[Jocket] Session not found: id=" + sessionId);
			return;
		}
		JocketPacket packet = JocketPacket.parse(text);
		String type = packet.getType();
		if (JocketPacket.TYPE_MESSAGE.equals(type)) {
			JocketEndpointRunner.doMessage(session, packet);
		}
		else if (JocketPacket.TYPE_PING.equals(type)) {
			session.setWaitingHeartbeat(true);
			JocketQueueManager.publishEvent(sessionId, new JocketPacket(JocketPacket.TYPE_PING));
		}
		else if (JocketPacket.TYPE_OPEN.equals(type)) {
			JocketSessionManager.open(sessionId);
		}
		else if (JocketPacket.TYPE_CLOSE.equals(type)) {
			Jocket.close(sessionId, JocketCloseReason.NORMAL, "Jocket session closed by user");
		}
		else {
			logger.error("[Jocket] Invalid packet type: " + type);
		}
	}
}
