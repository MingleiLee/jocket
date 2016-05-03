package com.jeedsoft.jocket.servlet;

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

import com.jeedsoft.jocket.connection.JocketStub;
import com.jeedsoft.jocket.connection.JocketStubManager;
import com.jeedsoft.jocket.endpoint.JocketEndpointConfig;
import com.jeedsoft.jocket.endpoint.JocketDeployer;
import com.jeedsoft.jocket.exception.JocketException;
import com.jeedsoft.jocket.util.IoUtil;

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
			Map<String, String> parameterMap = config.getPathParameterMap(path);
			for (Map.Entry<String, String[]> entry: request.getParameterMap().entrySet()) {
				parameterMap.put(entry.getKey(), entry.getValue()[0]);
			}
			
			//add stub
			JocketStub stub = new JocketStub();
			stub.setHttpSessionId(request.getSession().getId());
			stub.setHandlerClass(config.getHandlerClass());
			stub.setStatus(JocketStub.STATUS_PREPARED);
			stub.setParameterMap(parameterMap);
			String connectionId = JocketStubManager.add(stub);
			
			//set response
			JSONObject json = new JSONObject().put("connectionId", connectionId);
			IoUtil.write(response, json.toString());
			logger.debug("[Jocket] Jocket prepared: cid={}", connectionId);
		}
		catch (JocketException e) {
			throw new ServletException(e);
		}
	}
}
