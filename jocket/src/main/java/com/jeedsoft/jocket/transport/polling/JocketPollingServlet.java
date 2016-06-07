package com.jeedsoft.jocket.transport.polling;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.Jocket;
import com.jeedsoft.jocket.JocketService;
import com.jeedsoft.jocket.connection.JocketCloseCode;
import com.jeedsoft.jocket.connection.JocketCloseReason;
import com.jeedsoft.jocket.connection.JocketConnectionManager;
import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.connection.JocketSessionManager;
import com.jeedsoft.jocket.endpoint.JocketEndpointRunner;
import com.jeedsoft.jocket.message.JocketPacket;
import com.jeedsoft.jocket.message.JocketQueueManager;
import com.jeedsoft.jocket.util.JocketException;
import com.jeedsoft.jocket.util.JocketIoUtil;

@WebServlet(urlPatterns="/jocket", name="JocketPollingServlet", asyncSupported=true)
public class JocketPollingServlet extends HttpServlet
{
	private static final Logger logger = LoggerFactory.getLogger(JocketPollingServlet.class);

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		try {
			String sessionId = request.getParameter("s");
			logger.trace("[Jocket] Polling start: sid={}", sessionId);

			//get session and check status
			JocketSession session = JocketSessionManager.get(sessionId);
			if (session == null) {
				int code = JocketCloseCode.SESSION_NOT_FOUND;
				JocketCloseReason reason = new JocketCloseReason(code, "session not found");
				JocketPacket packet = new JocketPacket(JocketPacket.TYPE_CLOSE, null, reason);
				JocketIoUtil.writeJson(response, packet.toJson());
				return;
			}
			String status = session.getStatus();
			if (JocketSession.STATUS_CLOSED.equals(status)) {
				logger.debug("[Jocket] Already closed: sid={}", sessionId);
				JocketCloseReason reason = session.getCloseReason();
				JocketPacket packet = new JocketPacket(JocketPacket.TYPE_CLOSE, null, reason);
				JocketIoUtil.writeJson(response, packet.toJson());
				JocketSessionManager.remove(sessionId);
				return;
			}
			if (session.isConnected()) {
				logger.debug("[Jocket] Already connected: sid={}", sessionId);
				JocketPacket packet = new JocketPacket(JocketPacket.TYPE_NOOP);
				JocketIoUtil.writeJson(response, packet.toJson());
				return;
			}

			//update status
	        if (JocketSession.STATUS_NEW.equals(status)) {
	        	session.setStatus(JocketSession.STATUS_HANDSHAKING);
	        }
	        else if (JocketSession.STATUS_HANDSHAKING.equals(status)) {
				session.setStatus(JocketSession.STATUS_OPEN);
	        }

	        //return PONG when waiting for heartbeat
			if (session.isHeartbeating()) {
				logger.trace("[Jocket] Jocket is waiting for handshaking: sid={}", sessionId);
				session.setHeartbeating(false);
				JocketPacket packet = new JocketPacket(JocketPacket.TYPE_PONG);
				JocketIoUtil.writeJson(response, packet.toJson());
				return;
			}
			
			//start the async context
	        AsyncContext context = request.startAsync();
			JocketPollingConnection cn = new JocketPollingConnection(session, context);
	        context.addListener(new JocketPollingAsyncListener(cn));
	        context.setTimeout(JocketService.getConnectionTimeout());
	        synchronized (cn) {
		        JocketConnectionManager.add(cn);
		        if (JocketSession.STATUS_OPEN.equals(status)) {
		    		JocketQueueManager.addSubscriber(cn);
		        }
		        else if (JocketSession.STATUS_HANDSHAKING.equals(status)) {
					JocketEndpointRunner.doOpen(session, request.getSession());
		    		JocketQueueManager.addSubscriber(cn);
					if (logger.isDebugEnabled()) {
						Object[] args = {sessionId, session.getRequestPath()};
						logger.debug("[Jocket] Jocket opened: sid={}, path={}", args);
					}
		        }
		        else if (!JocketSession.STATUS_NEW.equals(status)) {
		        	throw new JocketException("Invalid status: " + status);
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
		String sessionId = request.getParameter("s");
		String text = JocketIoUtil.readText(request);
		logger.trace("[Jocket] Packet received: sid={}, packet={}", sessionId, text);
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
			session.setHeartbeating(true);
	        session.setLastHeartbeatTime(System.currentTimeMillis());
			JocketQueueManager.publishEvent(sessionId, new JocketPacket(JocketPacket.TYPE_PING));
		}
		else if (JocketPacket.TYPE_CLOSE.equals(type)) {
			Jocket.close(sessionId, JocketCloseCode.NORMAL, "Jocket session closed by user");
		}
		else {
			logger.error("[Jocket] Invalid packet type for polling connection: " + type);
		}
		JocketIoUtil.writeText(response, "{}");
	}
	
	private static class JocketPollingAsyncListener implements AsyncListener
	{
		private JocketPollingConnection cn;
		
		public JocketPollingAsyncListener(JocketPollingConnection cn)
		{
			this.cn = cn;
		}
		
		@Override
		public void onTimeout(AsyncEvent event) throws IOException
		{
			cn.downstream(new JocketPacket(JocketPacket.TYPE_NOOP));
		}

		@Override
		public void onStartAsync(AsyncEvent event) throws IOException {}
	
		@Override
		public void onComplete(AsyncEvent event) throws IOException {}
	
		@Override
		public void onError(AsyncEvent event) throws IOException {}
	}
}
