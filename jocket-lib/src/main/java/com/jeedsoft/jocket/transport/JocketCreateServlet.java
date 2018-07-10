package com.jeedsoft.jocket.transport;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jeedsoft.jocket.util.JocketIdGenerator;
import com.jeedsoft.jocket.endpoint.JocketEndpointRunner;
import com.jeedsoft.jocket.util.JocketClock;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.Jocket;
import com.jeedsoft.jocket.JocketService;
import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.connection.JocketSessionManager;
import com.jeedsoft.jocket.endpoint.JocketDeployer;
import com.jeedsoft.jocket.endpoint.JocketEndpointConfig;
import com.jeedsoft.jocket.util.JocketException;
import com.jeedsoft.jocket.util.JocketIoUtil;
import com.jeedsoft.jocket.util.JocketRequestUtil;

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
	    // Get Jocket path
		String servletPath = request.getServletPath();
		String path = request.getParameter("jocket_path");
        String clientVersion = request.getParameter("jocket_version");
		if (path == null) {
			// For backward compatibility. In old versions, Jocket use the URL path as Jocket path
			path = servletPath.replaceFirst("\\.jocket.*", "");
		}
		logger.debug("[Jocket] Session creating: path={}, query string={}", path, request.getQueryString());

		// Get Jocket configuration
		JocketEndpointConfig config;
		try {
			config = JocketDeployer.getConfig(path);
		}
		catch (JocketException e) {
			response.setStatus(404);
			JocketIoUtil.writeText(response, "Jocket configuration not found.");
            logger.error("[Jocket] Configuration not found: path=" + path, e);
			return;
		}
		
		try {
			// Get parameters
			Map<String, String> parameters = config.getPathParameterMap(path);
			for (Map.Entry<String, String[]> entry: request.getParameterMap().entrySet()) {
				String key = entry.getKey();
				if (!key.startsWith("jocket_")) {
					parameters.put(key, entry.getValue()[0]);
				}
			}
			
			// Create session
            long now = JocketClock.now();
            String sessionId = JocketIdGenerator.generate();
			JocketSession session = new JocketSession();
            session.setId(sessionId);
            session.setRequestPath(path);
			session.setHttpSessionId(request.getSession().getId());
			session.setEndpointClassName(config.getEndpointClassName());
			session.setParameters(parameters);
			session.setStatus(JocketSession.STATUS_OPEN);
            session.setStartTime(now);
            session.setLastHeartbeatTime(now);
			JocketSessionManager.add(session);
			if (logger.isDebugEnabled()) {
				String ua = request.getHeader("User-Agent");
				String device = JocketRequestUtil.getDevice(ua);
				Object[] args = {sessionId, clientVersion, Jocket.VERSION, device, ua};
				logger.debug("[Jocket] Session created: sid={}, clientVersion={}, serverVersion={}, device={}, userAgent={}", args);
			}

			// Invoke callback. For redis compatibility, the session must get from manager to
			// support methods such as setAttribute()
			JocketSession theSession = JocketSessionManager.get(sessionId);
            JocketEndpointRunner.doOpen(theSession, request.getSession());

            // Write HTTP response
			JSONObject result = new JSONObject();
			result.put("pathDepth", servletPath.replaceAll("[^/]+", "").length());
			result.put("upgrade", JocketService.isWebSocketEnabled());
			result.put("pingInterval", JocketService.getPingInterval());
			result.put("pingTimeout", JocketService.getPingTimeout());
			result.put("sessionId", sessionId);
			JocketIoUtil.writeJson(response, result);
		}
		catch (JocketException e) {
            logger.error("[Jocket] Failed to create session: path=" + path, e);
			throw new ServletException(e);
		}
	}
}
