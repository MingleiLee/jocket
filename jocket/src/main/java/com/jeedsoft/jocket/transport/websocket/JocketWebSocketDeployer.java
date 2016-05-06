package com.jeedsoft.jocket.transport.websocket;

import javax.servlet.ServletContext;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig.Builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.endpoint.JocketDeployer;
import com.jeedsoft.jocket.util.JocketException;

public class JocketWebSocketDeployer
{
	private static final Logger logger = LoggerFactory.getLogger(JocketWebSocketDeployer.class);
	
	private static final String WEBSOCKET_CONTAINER_KEY = "javax.websocket.server.ServerContainer";

	/**
	 * Attention: This method is invoked through reflection in JocketDeployer
	 */
	public static void deploy(ServletContext context) throws JocketException
	{
		for (String path: JocketDeployer.getPaths()) {
			try {
				logger.trace("[Jocket] Deploying as WebSocket: path={}", path);
		        ServerContainer container = (ServerContainer)context.getAttribute(WEBSOCKET_CONTAINER_KEY);
		    	Builder builder = Builder.create(JocketWebSocketEndpoint.class, path);
		    	container.addEndpoint(builder.build());
			}
	        catch (DeploymentException e) {
				logger.trace("[Jocket] Failed to deploy as WebSocket: path={}", path);
	        	throw new JocketException("Failed to deploy Jocket endpoint as WebSocket", e);
	        }
		}
	}
}
