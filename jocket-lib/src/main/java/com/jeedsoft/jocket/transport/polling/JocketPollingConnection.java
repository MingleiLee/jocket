package com.jeedsoft.jocket.transport.polling;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;

import com.jeedsoft.jocket.JocketService;
import com.jeedsoft.jocket.connection.*;
import com.jeedsoft.jocket.storage.redis.JocketRedisSubscriber;
import com.jeedsoft.jocket.util.JocketIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.message.JocketPacket;
import com.jeedsoft.jocket.util.JocketIoUtil;

public class JocketPollingConnection extends JocketConnection
{
	private static final Logger logger = LoggerFactory.getLogger(JocketPollingConnection.class);
	
	private AsyncContext context;
	
	public JocketPollingConnection(JocketSession session)
	{
		super(session);
	}

	public JocketPollingConnection(JocketSession session, AsyncContext context)
	{
		super(session);
		this.context = context;
	}

	public AsyncContext getPollingContext()
	{
		return context;
	}

	public void setPollingContext(AsyncContext context)
	{
		this.context = context;
	}

	@Override
	public synchronized boolean downstream(JocketPacket packet) throws IOException
	{
		if (!isActive()) {
			return false;
		}
		try {
            String sessionId = getSessionId();
            if (packet.getType().equals(JocketPacket.TYPE_MESSAGE)) {
                packet.setId(JocketIdGenerator.generate());
            }
            logger.debug("[Jocket] Send packet to client: transport=polling, sid={}, packet={}", sessionId, packet);
	        JocketConnectionManager.remove(this); // Connection must be removed before write response
	        HttpServletResponse response = (HttpServletResponse)context.getResponse();
	        JocketIoUtil.writeJson(response, packet.toJson());
	        context.complete();

	        if (JocketPacket.TYPE_UPGRADE.equals(packet.getType())) {
                if (!JocketConnectionManager.upgrade(sessionId) && JocketService.isCluster()) {
                    JocketRedisSubscriber.broadcastUpgrade(sessionId);
                }
            }
            return true;
		}
		finally {
			setActive(false);
		}
	}

	@Override
	public void close(JocketCloseReason reason) throws IOException
	{
		downstream(new JocketPacket(JocketPacket.TYPE_CLOSE, null, reason));
	}
}
