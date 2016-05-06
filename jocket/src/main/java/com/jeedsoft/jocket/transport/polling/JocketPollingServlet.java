package com.jeedsoft.jocket.transport.polling;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.Jocket;
import com.jeedsoft.jocket.connection.JocketCloseReason;
import com.jeedsoft.jocket.connection.JocketConnectionManager;
import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.connection.JocketSessionManager;
import com.jeedsoft.jocket.endpoint.JocketEndpointRunner;
import com.jeedsoft.jocket.event.JocketEvent;
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
		try {
			String path = request.getServletPath().replaceFirst("\\.jocket_polling.*", "");
			String sessionId = request.getParameter("jocket_sid");
			logger.trace("[Jocket] Polling start: sid={}, path={}", sessionId, path);

			//get session and check status
			JocketSession session = JocketSessionManager.get(sessionId);
			if (session == null) {
				int code = JocketCloseReason.CLOSED_ABNORMALLY;
				JocketCloseReason reason = new JocketCloseReason(code, "session not found");
				JocketEvent event = new JocketEvent(JocketEvent.TYPE_CLOSE, null, reason);
				JocketIoUtil.write(response, event.toJsonString());
				return;
			}
			String status = session.getStatus();
			boolean isFirstPolling = JocketSession.STATUS_PREPARED.equals(status);
			boolean isReconnecting = JocketSession.STATUS_RECONNECTING.equals(status);
			if (!isFirstPolling && !isReconnecting) {
				logger.error("[Jocket] status invalid for new polling: sid={}, status={}", sessionId, status);
				throw new JocketException("Jocket status invalid: sid=" + sessionId + ", status=" + status);
			}
			
			//start the async context
	        AsyncContext context = request.startAsync();
			JocketPollingConnection cn = new JocketPollingConnection(session, context);
	        context.addListener(new JocketPollingAsyncListener(cn));
	        context.setTimeout(JocketConstant.POLLING_INTERVAL);
	        session.setLastHeartbeatTime(System.currentTimeMillis());
	        JocketConnectionManager.add(cn);
			if (isFirstPolling) {
				JocketEndpointRunner.doOpen(session);
				logger.debug("[Jocket] Jocket opened: transport=long-polling, sid={}, path={}", sessionId, path);
			}
		}
		catch (JocketException e) {
			throw new ServletException(e);
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		try {
			String sessionId = request.getParameter("jocket_sid");
			JocketSession session = JocketSessionManager.get(sessionId);
			if (session == null) {
				throw new JocketException("Jocket session not found: id=" + sessionId);
			}
			String requestText = JocketIoUtil.readText(request);
			JocketEvent event = JocketEvent.parse(requestText);
			if (event.getType() == JocketEvent.TYPE_CLOSE) {
				Jocket.close(sessionId, JocketCloseReason.NORMAL, "Jocket session closed by user");
			}
			else {
				JocketEndpointRunner.doMessage(session, event);
			}
			JocketIoUtil.write(response, "{}");
		}
		catch (JocketException e) {
			throw new ServletException(e);
		}
	}
	
	public static void downstream(JocketPollingConnection cn, JocketEvent event) throws IOException
	{
		String sessionId = cn.getSessionId();
		logger.trace("[Jocket] Polling finished: sid={}, content={}", sessionId, event);
		boolean executed = false;
		//the following code must be synchronized to avoid concurrent write
		synchronized (cn) {
			AsyncContext context = cn.getPollingContext();
			if (context != null) {
				try {
			        JocketConnectionManager.remove(sessionId); //connection must be removed before write response
			        HttpServletResponse response = (HttpServletResponse)context.getResponse();
			        JocketIoUtil.write(response, event.toJsonString());
			        context.complete();
				}
				finally {
			        cn.setPollingContext(null);
			        executed = true;
				}
			}
		}
		if (executed && event.getType() == JocketEvent.TYPE_CLOSE) {
			JocketSession session = JocketSessionManager.remove(sessionId);
			if (session != null) {
				JocketEndpointRunner.doClose(session, JocketCloseReason.parse(event.getData()));
			}
		}
	}
}
