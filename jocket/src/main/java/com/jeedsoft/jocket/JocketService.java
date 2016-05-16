package com.jeedsoft.jocket;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.connection.JocketConnectionManager;
import com.jeedsoft.jocket.connection.JocketSessionManager;
import com.jeedsoft.jocket.connection.JocketSessionStore;
import com.jeedsoft.jocket.endpoint.JocketDeployer;
import com.jeedsoft.jocket.message.JocketQueue;
import com.jeedsoft.jocket.message.JocketQueueManager;
import com.jeedsoft.jocket.storage.JocketCleaner;
import com.jeedsoft.jocket.transport.websocket.JocketWebSocketDeployer;
import com.jeedsoft.jocket.transport.websocket.JocketWebSocketEndpoint;
import com.jeedsoft.jocket.util.JocketException;

public class JocketService
{
	private static final Logger logger = LoggerFactory.getLogger(JocketService.class);

	private static boolean isRunning = false;

	private static String[] transports = {"websocket", "polling"};
	
	/**
	 * Start the Jocket service
	 * 
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
	 * Stop the Jocket servie and free the resources
	 * 
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
	
	/**
	 * Set the Jocket session store
	 * 
	 * By default, JocketLocalSessionStore is used. For cluster, you can use JocketRedisSessionStore
	 * or write your own session store.
	 *  
	 * @param store the store to use
	 */
	public static void setSessionStore(JocketSessionStore store)
	{
		JocketSessionManager.setStore(store);
	}
	
	public static void setEventQueue(JocketQueue queue)
	{
		JocketQueueManager.setQueue(queue);
	}
	
	public static String[] getTransports()
	{
		return transports;
	}

	public static void setTransports(String[] transports)
	{
		if (transports == null || transports.length == 0) {
			throw new IllegalArgumentException("transports cannot be empty");
		}
		Set<String> validTransports = new HashSet<>();
		validTransports.add("websocket");
		validTransports.add("polling");
		for (String transport: transports) {
			if (!validTransports.contains(transport)) {
				throw new IllegalArgumentException("invalid transport: " + transport);
			}
		}
		JocketService.transports = transports;
	}

	public static boolean isRunning()
	{
		return isRunning;
	}
}
