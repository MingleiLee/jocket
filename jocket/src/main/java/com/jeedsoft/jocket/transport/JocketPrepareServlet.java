package com.jeedsoft.jocket.transport;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.connection.JocketSessionManager;
import com.jeedsoft.jocket.endpoint.JocketEndpointConfig;
import com.jeedsoft.jocket.endpoint.JocketDeployer;
import com.jeedsoft.jocket.transport.polling.JocketPollingServlet;
import com.jeedsoft.jocket.util.JocketException;
import com.jeedsoft.jocket.util.JocketIoUtil;

@WebServlet(urlPatterns="*.jocket_prepare", name="JocketPrepareServlet")
public class JocketPrepareServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(JocketPollingServlet.class);

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		doPost(request, response);
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		try {
			String path = request.getServletPath().replaceFirst("\\.jocket_prepare.*", "");
			logger.debug("[Jocket] Jocket preparing: path={}, query string={}", path, request.getQueryString());
			JocketEndpointConfig config = JocketDeployer.getConfig(path);
			Map<String, String> parameters = config.getPathParameterMap(path);
			for (Map.Entry<String, String[]> entry: request.getParameterMap().entrySet()) {
				parameters.put(entry.getKey(), entry.getValue()[0]);
			}
			parameters.remove("jocket_rnd");
			
			//add session
			JocketSession session = new JocketSession();
			session.setHttpSessionId(request.getSession().getId());
			session.setEndpointClassName(config.getEndpointClassName());
			session.setParameters(parameters);
			session.setStatus(JocketSession.STATUS_PREPARED);
			String sessionId = JocketSessionManager.add(session);
			
			//set response
			JSONObject json = new JSONObject().put("sessionId", sessionId);
			JocketIoUtil.write(response, json.toString());
			logger.debug("[Jocket] Jocket prepared: sid={}", sessionId);
		}
		catch (JocketException e) {
			throw new ServletException(e);
		}
	}
}
