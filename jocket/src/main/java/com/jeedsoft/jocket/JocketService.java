package com.jeedsoft.jocket;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.connection.JocketConnectionManager;
import com.jeedsoft.jocket.connection.JocketSessionManager;
import com.jeedsoft.jocket.connection.JocketSessionStore;
import com.jeedsoft.jocket.endpoint.JocketDeployer;
import com.jeedsoft.jocket.event.JocketQueue;
import com.jeedsoft.jocket.event.JocketQueueManager;
import com.jeedsoft.jocket.storage.JocketCleaner;
import com.jeedsoft.jocket.transport.websocket.JocketWebSocketDeployer;
import com.jeedsoft.jocket.transport.websocket.JocketWebSocketEndpoint;
import com.jeedsoft.jocket.util.JocketException;

public class JocketService
{
	private static final Logger logger = LoggerFactory.getLogger(JocketService.class);

	private static boolean isRunning = false;

	/**
	 * Start the Jocket service
	 * This method should be called in ServletContextListener.contextInitialized()
	 */
	public static void start(ServletContext context) throws JocketException
	{
		JocketWebSocketEndpoint.setApplicationContextPath(context.getContextPath());
		JocketWebSocketDeployer.deploy(context);
		JocketCleaner.start();
		JocketQueueManager.start();
		isRunning = true;
        logger.info("[Jocket] Service started. tree structure:\n" + JocketDeployer.getTreeText());
	}
	
	/**
	 * Free the resources
	 * This method should be called in ServletContextListener.contextDestroyed()
	 */
	public static void stop()
	{
		isRunning = false;
		JocketCleaner.stop();
		JocketQueueManager.stop();
		JocketDeployer.clear();
		JocketConnectionManager.clear();
        logger.info("[Jocket] Service stopped.");
	}
	
	public static void setSessionStore(JocketSessionStore store)
	{
		JocketSessionManager.setStore(store);
	}
	
	public static void setEventQueue(JocketQueue queue)
	{
		JocketQueueManager.setQueue(queue);
	}

	public static boolean isRunning()
	{
		return isRunning;
	}
}
