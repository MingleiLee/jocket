package com.jeedsoft.jocket.transport.polling;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jeedsoft.jocket.util.*;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.connection.JocketCloseCode;
import com.jeedsoft.jocket.connection.JocketCloseReason;
import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.connection.JocketSessionManager;
import com.jeedsoft.jocket.endpoint.JocketEndpointRunner;
import com.jeedsoft.jocket.message.JocketPacket;
import com.jeedsoft.jocket.message.JocketQueueManager;

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
		try {
			String text = unescape(JocketIoUtil.readText(request));
			if (logger.isTraceEnabled()) {
				String params = JocketRequestUtil.getQueryStringWithoutSessionId(request);
				logger.trace("[Jocket] Packet received: sid={}, packet={}, params=[{}]", sessionId, text, params);
			}

			JocketPacket packet = JocketPacket.parse(text);
			String type = packet.getType();
			if (type.equals(JocketPacket.TYPE_CONFIRM)) {
				logger.trace("[Jocket] Message confirmed: sid={}, packetId={}", sessionId, packet.getData());
				JocketIoUtil.writeText(response, "{}");
				return;
			}
			else if (type.equals(JocketPacket.TYPE_LOG)) {
				JocketClientLogger.log(sessionId, new JSONArray(packet.getData()));
				JocketIoUtil.writeText(response, "{}");
				return;
			}

			JocketSession session = JocketSessionManager.get(sessionId);
			if (session == null) {
				logger.error("[Jocket] Session not found: sid={}", sessionId);
				JocketIoUtil.writeText(response, "{}");
				return;
			}

			if (type.equals(JocketPacket.TYPE_MESSAGE)) {
				JocketEndpointRunner.doMessage(session, packet);
			}
			else if (type.equals(JocketPacket.TYPE_PING)) {
				session.setLastHeartbeatTime(JocketClock.now());
				JocketQueueManager.publish(sessionId, new JocketPacket(JocketPacket.TYPE_PONG));
			}
			else if (type.equals(JocketPacket.TYPE_CLOSE)) {
				JocketCloseReason reason = JocketCloseReason.parse(packet.getData());
				if (reason == null) {
					reason = new JocketCloseReason(JocketCloseCode.NORMAL, "Jocket session closed by user");
				}
				JocketSessionManager.close(sessionId, reason, false);
			}
			else if (type.equals(JocketPacket.TYPE_BROWSER_CLOSE)) {
				logger.debug("[Jocket] User is trying to close or reload the browser: sid={}", sessionId);
			}
			else {
				logger.error("[Jocket] Invalid packet type for polling connection: sid={}, type={}", sessionId, type);
			}
			JocketIoUtil.writeText(response, "{}");
		}
		catch (Exception e) {
			logger.error("[Jocket] HTTP send failed: sid=" + sessionId, e);
			try {
				JocketIoUtil.writeText(response, "{}");
			}
			catch (Exception e2) {
				logger.error("[Jocket] Failed to write response on error: sid=" + sessionId, e2);
			}
			response.setStatus(200);
		}
	}

	/**
	 * Convert the escaped text back.
	 *
	 * @see Jocket.Ajax.prototype.dataToString in Jocket.js
	 */
	private static String unescape(String text)
	{
		return text == null ? null : text.replaceAll("\uF000" + "1", ".").replaceAll("\uF000" + "0", "\uF000");
	}
}
