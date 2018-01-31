package com.jeedsoft.jocket.transport.polling;

import com.jeedsoft.jocket.JocketService;
import com.jeedsoft.jocket.connection.*;
import com.jeedsoft.jocket.message.JocketPacket;
import com.jeedsoft.jocket.util.JocketIoUtil;
import com.jeedsoft.jocket.util.JocketRequestUtil;
import com.jeedsoft.jocket.util.JocketStringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns="/poll.jocket", name="JocketPollingServlet", asyncSupported=true)
public class JocketPollingServlet extends HttpServlet
{
	private static final Logger logger = LoggerFactory.getLogger(JocketPollingServlet.class);

	private static final long serialVersionUID = 1L;
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException
	{
		process(request, response);
	}
	
	public static void process(HttpServletRequest request, HttpServletResponse response) throws ServletException
	{
		String sessionId = request.getParameter("s");
		try {
			if (logger.isTraceEnabled()) {
				String params = JocketRequestUtil.getQueryStringWithoutSessionId(request);
				logger.trace("[Jocket] Polling start: sid={}, params=[{}]", sessionId, params);
                String confirmPacketId = request.getParameter("p");
                if (!JocketStringUtil.isEmpty(confirmPacketId)) {
                    logger.trace("[Jocket] Message confirmed: sid={}, packetId={}", sessionId, confirmPacketId);
                }
			}

			// Get session and check status
			JocketSession session = JocketSessionManager.get(sessionId);
			if (session == null) {
				int code = JocketCloseCode.SESSION_NOT_FOUND;
				JocketCloseReason reason = new JocketCloseReason(code, "session not found");
				JocketPacket packet = new JocketPacket(JocketPacket.TYPE_CLOSE, reason);
				JocketIoUtil.writeJson(response, packet.toJson());
				return;
			}
			String status = session.getStatus();
			if (JocketSession.STATUS_CLOSED.equals(status)) {
				logger.debug("[Jocket] Already closed: sid={}", sessionId);
				JocketCloseReason reason = session.getCloseReason();
				JocketPacket packet = new JocketPacket(JocketPacket.TYPE_CLOSE, reason);
				JocketIoUtil.writeJson(response, packet.toJson());
				JocketSessionManager.remove(sessionId);
				return;
			}

			// start the async context
			request.setAttribute("org.apache.catalina.ASYNC_SUPPORTED", true);
			AsyncContext context = request.startAsync();
			JocketPollingConnection cn = new JocketPollingConnection(session, context);
	        context.addListener(new JocketPollingAsyncListener(cn));
	        context.setTimeout(JocketService.getConnectionTimeout());
			JocketConnectionManager.add(cn);
		}
		catch (Exception e) {
			logger.error("[Jocket] HTTP polling failed: sid=" + sessionId, e);
			throw new ServletException(e);
		}
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
		    String sessionId = cn.getSessionId();
			logger.trace("[Jocket] Polling timeout. sid={}", sessionId);
            JocketCloseReason reason = new JocketCloseReason(JocketCloseCode.NO_HEARTBEAT, "no heartbeat");
			JocketPacket packet = new JocketPacket(JocketPacket.TYPE_CLOSE, reason);
			if (cn.downstream(packet)) {
                JocketSessionManager.close(sessionId, reason, false);
            }
		}

		@Override
		public void onStartAsync(AsyncEvent event) throws IOException {}
	
		@Override
		public void onComplete(AsyncEvent event) throws IOException {}
	
		@Override
		public void onError(AsyncEvent event) throws IOException {}
	}
}
