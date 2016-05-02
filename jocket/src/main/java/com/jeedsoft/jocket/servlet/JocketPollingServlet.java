package com.jeedsoft.jocket.servlet;

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
import com.jeedsoft.jocket.connection.JocketConnection;
import com.jeedsoft.jocket.connection.JocketConnectionManager;
import com.jeedsoft.jocket.connection.JocketStub;
import com.jeedsoft.jocket.connection.JocketStubManager;
import com.jeedsoft.jocket.connection.impl.JocketPollingConnection;
import com.jeedsoft.jocket.endpoint.JocketCloseReason;
import com.jeedsoft.jocket.endpoint.JocketEndpointRunner;
import com.jeedsoft.jocket.event.JocketEvent;
import com.jeedsoft.jocket.exception.JocketException;
import com.jeedsoft.jocket.util.IoUtil;

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
			String cid = request.getParameter("jocket_cid");
			
			//get stub and check status
			JocketStub stub = JocketStubManager.get(cid);
			if (stub == null) {
				int code = JocketCloseReason.CLOSED_ABNORMALLY;
				JocketCloseReason reason = new JocketCloseReason(code, "connection id not found");
				JocketEvent event = new JocketEvent(JocketEvent.TYPE_CLOSE, null, reason);
				IoUtil.write(response, event.toJsonString());
				return;
			}
			int status = stub.getStatus();
			boolean isFirstPolling = status == JocketStub.STATUS_PREPARED;
			if (!isFirstPolling && status != JocketStub.STATUS_RECONNECTING) {
				throw new JocketException("Jocket status invalid: cid=" + cid + ", status=" + status);
			}
			logger.trace("[Jocket] Polling start: cid={}, path={}", cid, path);
			
			//start the async context
	        AsyncContext context = request.startAsync();
			JocketPollingConnection cn = new JocketPollingConnection(cid, context);
	        context.addListener(new JocketPollingAsyncListener(cn));
	        context.setTimeout(JocketPollingConnection.POLLING_INTERVAL);
	        JocketStubManager.setLastPolling(cid, System.currentTimeMillis());
	        JocketConnectionManager.add(cn);
			if (isFirstPolling) {
				JocketStubManager.setTransport(cid, JocketStub.TRANSPORT_POLLING);
				JocketEndpointRunner.doOpen(cn);
				logger.debug("[Jocket] Jocket opened: transport=long-polling, cid={}, path={}", cid, path);
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
			String cid = request.getParameter("jocket_cid");
	        JocketConnection cn = JocketConnectionManager.get(cid);
	        if (cn == null) {
				JocketStub stub = JocketStubManager.get(cid);
				if (stub == null) {
					throw new JocketException("Jocket connection id not found: " + cid);
				}
		        cn = new JocketPollingConnection(cid);
	        }
			String requestText = IoUtil.readText(request);
			JocketEvent event = JocketEvent.parse(requestText);
			if (event.getType() == JocketEvent.TYPE_CLOSE) {
				Jocket.close(cid, JocketCloseReason.NORMAL, "Jocket connection closed by user");
			}
			else {
				JocketEndpointRunner.doMessage(cn, event);
			}
			IoUtil.write(response, "{}");
		}
		catch (JocketException e) {
			throw new ServletException(e);
		}
	}
	
	public static void downstream(JocketPollingConnection cn, JocketEvent event) throws IOException
	{
		if (logger.isTraceEnabled()) {
			logger.trace("[Jocket] Polling finished: cid={}, content={}", cn.getId(), event);
		}
		boolean executed = false;
		//the following code must be synchronized to avoid concurrent write
		synchronized (cn) {
			AsyncContext context = cn.getPollingContext();
			if (context != null) {
				try {
			        HttpServletResponse response = (HttpServletResponse)context.getResponse();
			        IoUtil.write(response, event.toJsonString());
			        context.complete();
				}
				finally {
			        JocketConnectionManager.remove(cn);
			        cn.setPollingContext(null);
			        executed = true;
				}
			}
		}
		if (executed && event.getType() == JocketEvent.TYPE_CLOSE) {
			JocketStub stub = JocketStubManager.get(cn.getId());
			if (stub != null) {
				cn.setStub(stub);
				JocketStubManager.remove(cn.getId());
				JocketEndpointRunner.doClose(cn, JocketCloseReason.parse(event.getData()));
			}
		}
	}
}
