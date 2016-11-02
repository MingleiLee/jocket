package com.jeedsoft.jocket.transport.polling;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.connection.JocketCloseCode;
import com.jeedsoft.jocket.connection.JocketCloseReason;
import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.connection.JocketSessionManager;
import com.jeedsoft.jocket.endpoint.JocketEndpointRunner;
import com.jeedsoft.jocket.message.JocketPacket;
import com.jeedsoft.jocket.message.JocketQueueManager;
import com.jeedsoft.jocket.util.JocketIoUtil;
import com.jeedsoft.jocket.util.JocketRequestUtil;

@WebServlet(urlPatterns="/send.jocket", name="JocketSendServlet")
public class JocketSendServlet extends HttpServlet
{
	private static final Logger logger = LoggerFactory.getLogger(JocketSendServlet.class);

	private static final long serialVersionUID = 1L;
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		process(request, response);
	}
	
	public static void process(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String sessionId = request.getParameter("s");
		String text = JocketIoUtil.readText(request);
		if (logger.isTraceEnabled()) {
			String params = JocketRequestUtil.getQueryStringWithoutSessionId(request);
			logger.trace("[Jocket] Packet received: sid={}, packet={}, params=[{}]", sessionId, text, params);
		}
		JocketSession session = JocketSessionManager.get(sessionId);
		if (session == null) {
			logger.error("[Jocket] Session not found: sid={}", sessionId);
			JocketIoUtil.writeText(response, "{}");
			return;
		}
		JocketPacket packet = JocketPacket.parse(text);
		String type = packet.getType();
		if (JocketPacket.TYPE_MESSAGE.equals(type)) {
			JocketEndpointRunner.doMessage(session, packet);
		}
		else if (JocketPacket.TYPE_PING.equals(type)) {
			session.setHeartbeating(true);
	        session.setLastHeartbeatTime(System.currentTimeMillis());
			JocketQueueManager.publishEvent(sessionId, new JocketPacket(JocketPacket.TYPE_PING));
		}
		else if (JocketPacket.TYPE_CLOSE.equals(type)) {
			JocketCloseReason reason = JocketCloseReason.parse(packet.getData());
			if (reason == null) {
				reason = new JocketCloseReason(JocketCloseCode.NORMAL, "Jocket session closed by user");
			}
			JocketSessionManager.close(sessionId, reason, false);
		}
		else {
			logger.error("[Jocket] Invalid packet type for polling connection: sid={}, type={}", sessionId, type);
		}
		JocketIoUtil.writeText(response, "{}");
	}
}
