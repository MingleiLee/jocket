package com.jeedsoft.jocket.message;

import com.jeedsoft.jocket.connection.JocketConnection;
import com.jeedsoft.jocket.connection.JocketConnectionManager;
import com.jeedsoft.jocket.transport.websocket.JocketWebSocketConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JocketConsumer
{
	private static final Logger logger = LoggerFactory.getLogger(JocketConsumer.class);

	public static void notify(String sessionId)
	{
        JocketConnection cn = JocketConnectionManager.get(sessionId);
        logger.trace("[Jocket] Notify: sid={}, cn={}", sessionId, cn);
        if (cn != null) {
            synchronized (cn) {
                if (cn.isActive()) {
                    JocketPacket packet = JocketQueueManager.peek(sessionId);
                    logger.trace("[Jocket] Notify: sid={}, packet={}", sessionId, packet);
                    if (packet != null) {
                        new Thread(new Runner(sessionId)).start();
                    }
                }
            }
        }
	}

    private static class Runner implements Runnable
    {
        private String sessionId;

        public Runner(String sessionId)
        {
            this.sessionId = sessionId;
        }

        @Override
        public void run()
        {
            JocketConnection cn = JocketConnectionManager.get(sessionId);
            logger.trace("[Jocket] Consume: sid={}, cn={}", sessionId, cn);
            if (cn != null) {
                synchronized (cn) {
                    if (cn.isActive()) {
                        JocketPacket packet = JocketQueueManager.poll(sessionId);
                        logger.trace("[Jocket] Consume: sid={}, packet={}", sessionId, packet);
                        if (packet != null) {
                            process(cn, packet);
                        }
                    }
                }
            }
        }

        private void process(JocketConnection cn, JocketPacket packet)
        {
            String packetType = packet.getType();
            if (packetType.equals(JocketPacket.TYPE_MESSAGE)) {
                try {
                    cn.downstream(packet);
                }
                catch (Throwable e) {
                    logger.error("[Jocket] Failed to send message: sid=" + sessionId + ", packet=" + packet, e);
                }
            }
            else {
                cn.onEvent(packet);
            }
            if (cn instanceof JocketWebSocketConnection) {
                JocketConsumer.notify(sessionId);
            }
        }
    }
}
