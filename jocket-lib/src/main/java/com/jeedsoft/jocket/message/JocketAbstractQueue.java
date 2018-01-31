package com.jeedsoft.jocket.message;

import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.util.JocketClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class JocketAbstractQueue implements JocketQueue
{
    private static final Logger logger = LoggerFactory.getLogger(JocketAbstractQueue.class);

	protected boolean preparePublish(JocketSession session, JocketPacket packet)
	{
        String sessionId = session.getId();
        String status = session.getStatus();
        String packetType = packet.getType();
        boolean isAcceptable = packetType.equals(JocketPacket.TYPE_CLOSE)
                || packetType.equals(JocketPacket.TYPE_MESSAGE) && JocketSession.STATUS_OPEN.equals(status)
                || packetType.equals(JocketPacket.TYPE_UPGRADE) && JocketSession.STATUS_OPEN.equals(status)
                || packetType.equals(JocketPacket.TYPE_PONG) && !JocketSession.STATUS_CLOSED.equals(status);
        if (!isAcceptable) {
            logger.debug("[Jocket] Packet not publishable currently: sid={}, packet={}", sessionId, packet);
            return false;
        }

        logger.trace("[Jocket] Publish to queue: sid={}, packet={}", sessionId, packet);
        if (packetType.equals(JocketPacket.TYPE_MESSAGE)) {
            session.setLastMessageTime(JocketClock.now());
        }
        return true;
	}
}
