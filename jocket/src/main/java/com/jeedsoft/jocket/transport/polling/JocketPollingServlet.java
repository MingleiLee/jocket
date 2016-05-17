package com.jeedsoft.jocket.transport.polling;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.connection.JocketCloseCode;
import com.jeedsoft.jocket.connection.JocketCloseReason;
import com.jeedsoft.jocket.connection.JocketConnectionManager;
import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.connection.JocketSessionManager;
import com.jeedsoft.jocket.message.JocketPacket;
import com.jeedsoft.jocket.message.JocketQueueManager;
import com.jeedsoft.jocket.transport.JocketUpstreamHandler;
import com.jeedsoft.jocket.util.JocketConstant;
import com.jeedsoft.jocket.util.JocketException;
import com.jeedsoft.jocket.util.JocketIoUtil;

@WebServlet(urlPatterns="*.jocket_polling", name="JocketPollingServlet", asyncSupported=true)
public class JocketPollingServlet extends HttpServlet
{
	private static final Logger logger = LoggerFactory.getLogger(JocketPollingServlet.class);

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		if (request.getHeader("Referer") == null) {
			JocketIoUtil.writeText(response, "This URL should be opened by Jocket library.");
			return;
		}
		try {
			String path = request.getServletPath().replaceFirst("\\.jocket_polling.*", "");
			String sessionId = request.getParameter("jocket_sid");
			logger.trace("[Jocket] Polling start: sid={}, path={}", sessionId, path);

			//get session and check status
			JocketSession session = JocketSessionManager.get(sessionId);
			if (session == null) {
				int code = JocketCloseCode.CLOSED_ABNORMALLY;
				JocketCloseReason reason = new JocketCloseReason(code, "session not found");
				JocketPacket packet = new JocketPacket(JocketPacket.TYPE_CLOSE, null, reason);
				JocketIoUtil.writeJson(response, packet.toJson());
				return;
			}
			else if (JocketSession.STATUS_CLOSED.equals(session.getStatus())) {
				JocketCloseReason reason = session.getCloseReason();
				JocketPacket packet = new JocketPacket(JocketPacket.TYPE_CLOSE, null, reason);
				JocketIoUtil.writeJson(response, packet.toJson());
				JocketSessionManager.remove(sessionId);
				return;
			}
			else if (session.isWaitingHeartbeat()) {
				JocketPacket packet = new JocketPacket(JocketPacket.TYPE_PONG);
				JocketIoUtil.writeJson(response, packet.toJson());
				session.setWaitingHeartbeat(false);
				return;
			}
			else if (session.isConnected()) {
				logger.error("[Jocket] Already connected: sid={}", sessionId);
				throw new JocketException("Jocket already connected: sid=" + sessionId);
			}
			
			//start the async context
	        AsyncContext context = request.startAsync();
			JocketPollingConnection cn = new JocketPollingConnection(session, context);
	        context.addListener(new JocketPollingAsyncListener(cn));
	        context.setTimeout(JocketConstant.POLLING_INTERVAL);
	        session.setLastHeartbeatTime(System.currentTimeMillis());
	        synchronized (cn) {
		        JocketConnectionManager.add(cn);
		        if (session.isOpen()) {
		    		JocketQueueManager.addSubscriber(cn);
		        }
	        }
		}
		catch (JocketException e) {
			throw new ServletException(e);
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		if (request.getHeader("Referer") == null) {
			JocketIoUtil.writeText(response, "This URL should be opened by Jocket library.");
			return;
		}
		String sessionId = request.getParameter("jocket_sid");
		String text = JocketIoUtil.readText(request);
		JocketUpstreamHandler.handle(sessionId, text);			
		JocketIoUtil.writeJson(response, new JSONObject());
	}
}
