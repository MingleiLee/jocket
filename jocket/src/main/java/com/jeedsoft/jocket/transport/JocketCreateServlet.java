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

import com.jeedsoft.jocket.JocketService;
import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.connection.JocketSessionManager;
import com.jeedsoft.jocket.endpoint.JocketDeployer;
import com.jeedsoft.jocket.endpoint.JocketEndpointConfig;
import com.jeedsoft.jocket.util.JocketException;
import com.jeedsoft.jocket.util.JocketIoUtil;

@WebServlet(urlPatterns="*.jocket", name="JocketCreateServlet")
public class JocketCreateServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(JocketCreateServlet.class);

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		doPost(request, response);
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		try {
			String servletPath = request.getServletPath();
			String path = request.getParameter("jocket_path");
			if (path == null) {
				path = servletPath.replaceFirst("\\.jocket.*", "");
			}
			logger.debug("[Jocket] Session creating: path={}, query string={}", path, request.getQueryString());
			JocketEndpointConfig config = JocketDeployer.getConfig(path);
			JSONObject result = new JSONObject();
			result.put("pathDepth", servletPath.replaceAll("[^/]+", "").length());
			result.put("upgradable", JocketService.isWebSocketEnabled());
			result.put("pingInterval", JocketService.getPingInterval());
			result.put("pingTimeout", JocketService.getPingTimeout());

			//get parameters
			Map<String, String> parameters = config.getPathParameterMap(path);
			for (Map.Entry<String, String[]> entry: request.getParameterMap().entrySet()) {
				String key = entry.getKey();
				if (!key.startsWith("jocket_")) {
					parameters.put(key, entry.getValue()[0]);
				}
			}
			
			//add session
			JocketSession session = new JocketSession();
			session.setRequestPath(path);
			session.setHttpSessionId(request.getSession().getId());
			session.setEndpointClassName(config.getEndpointClassName());
			session.setParameters(parameters);
			session.setStatus(JocketSession.STATUS_NEW);
			String sessionId = JocketSessionManager.add(session);
			result.put("sessionId", sessionId);
			
			//set response
			JocketIoUtil.writeJson(response, result);
			logger.debug("[Jocket] Session created: sid={}", sessionId);
		}
		catch (JocketException e) {
			throw new ServletException(e);
		}
	}
}
