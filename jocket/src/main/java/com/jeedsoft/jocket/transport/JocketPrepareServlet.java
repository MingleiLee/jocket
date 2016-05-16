package com.jeedsoft.jocket.transport;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.JocketService;
import com.jeedsoft.jocket.connection.JocketSession;
import com.jeedsoft.jocket.connection.JocketSessionManager;
import com.jeedsoft.jocket.endpoint.JocketDeployer;
import com.jeedsoft.jocket.endpoint.JocketEndpointConfig;
import com.jeedsoft.jocket.util.JocketArrayUtil;
import com.jeedsoft.jocket.util.JocketException;
import com.jeedsoft.jocket.util.JocketIoUtil;
import com.jeedsoft.jocket.util.JocketJsonUtil;
import com.jeedsoft.jocket.util.JocketStringUtil;

@WebServlet(urlPatterns="*.jocket_prepare", name="JocketPrepareServlet")
public class JocketPrepareServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(JocketPrepareServlet.class);

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		doPost(request, response);
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		if (request.getHeader("Referer") == null) {
			JocketIoUtil.writeText(response, "This URL should be opened by Jocket library.");
			return;
		}
		try {
			String path = request.getServletPath().replaceFirst("\\.jocket_prepare.*", "");
			logger.debug("[Jocket] Jocket preparing: path={}, query string={}", path, request.getQueryString());
			JocketEndpointConfig config = JocketDeployer.getConfig(path);
			JSONObject result = new JSONObject();
			
			//get parameters
			Map<String, String> parameters = config.getPathParameterMap(path);
			for (Map.Entry<String, String[]> entry: request.getParameterMap().entrySet()) {
				parameters.put(entry.getKey(), entry.getValue()[0]);
			}
			parameters.remove("jocket_rnd");
			
			//get options
			String optionsText = JocketIoUtil.readText(request);
			JSONObject options = JocketStringUtil.isEmpty(optionsText) ? null : new JSONObject(optionsText);
			JSONArray transports = options == null ? null : options.optJSONArray("transports");
			result.put("transports", getTransports(transports));

			//add session
			JocketSession session = new JocketSession();
			session.setRequestPath(path);
			session.setHttpSessionId(request.getSession().getId());
			session.setEndpointClassName(config.getEndpointClassName());
			session.setParameters(parameters);
			session.setStatus(JocketSession.STATUS_PREPARED);
			String sessionId = JocketSessionManager.add(session);
			result.put("sessionId", sessionId);
			
			//set response
			JocketIoUtil.writeJson(response, result);
			logger.debug("[Jocket] Jocket prepared: sid={}", sessionId);
		}
		catch (JocketException e) {
			throw new ServletException(e);
		}
	}

	private JSONArray getTransports(JSONArray transports) throws JocketException
	{
		String[] serverTransports = JocketService.getTransports();
		if (transports == null || transports.length() == 0) {
			return JocketJsonUtil.putAll(new JSONArray(), serverTransports);
		}
		JSONArray result = new JSONArray();
		for (int i = 0, n = transports.length(); i < n; ++i) {
			String transport = transports.getString(i);
			if (JocketArrayUtil.contains(serverTransports, transport)) {
				result.put(transport);
			}
		}
		if (result.length() == 0) {
			throw new JocketException("None of the the following transports supported by server: " + transports);
		}
		return result;
	}
}
