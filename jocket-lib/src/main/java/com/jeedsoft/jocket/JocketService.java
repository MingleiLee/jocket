package com.jeedsoft.jocket;

import javax.servlet.ServletContext;

import com.jeedsoft.jocket.storage.redis.JocketRedisQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.connection.JocketConnectionManager;
import com.jeedsoft.jocket.connection.JocketSessionManager;
import com.jeedsoft.jocket.connection.JocketSessionStore;
import com.jeedsoft.jocket.endpoint.JocketDeployer;
import com.jeedsoft.jocket.message.JocketQueue;
import com.jeedsoft.jocket.message.JocketQueueManager;
import com.jeedsoft.jocket.storage.JocketCleaner;
import com.jeedsoft.jocket.util.JocketException;

public class JocketService
{
	private static final Logger logger = LoggerFactory.getLogger(JocketService.class);

	/**
	 * Whether WebSocket is enabled. If enabled, the client will try to use (upgrade to) WebSocket.
	 * If not, the client will use polling only.
	 */
	private static boolean isWebSocketEnabled = true;

	/**
	 * The ping interval (milliseconds)
	 */
	private static int pingInterval = 25_000;

	/**
	 * The timeout (milliseconds) after last ping packet sent
	 */
	private static int pingTimeout = 20_000;

	/**
	 * The capacity of queue
	 */
	private static int queueCapacity = 1000;

	/**
	 * Whether the Jocket service is running
	 */
	private static boolean isRunning = false;

	/**
	 * Start the Jocket service
	 * 
	 * This method should be called in ServletContextListener.contextInitialized()
	 */
	public static void start(ServletContext context) throws JocketException
	{
		JocketCleaner.start();
		JocketQueueManager.start();
		isRunning = true;
        logger.info("[Jocket] Service started. tree structure:\n{}", JocketDeployer.getTreeText());
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

	public static int getPingInterval()
	{
		return pingInterval;
	}

	public static void setPingInterval(int pingInterval)
	{
		JocketService.pingInterval = pingInterval;
	}

	public static int getPingTimeout()
	{
		return pingTimeout;
	}

	public static void setPingTimeout(int pingTimeout)
	{
		JocketService.pingTimeout = pingTimeout;
	}

	public static int getConnectionTimeout()
	{
		return pingInterval + pingTimeout + 5000; //extra 5 seconds
	}

	public static int getQueueCapacity()
	{
		return queueCapacity;
	}

	public static void setQueueCapacity(int queueCapacity)
	{
		JocketService.queueCapacity = queueCapacity;
	}

	public static boolean isWebSocketEnabled()
	{
		return isWebSocketEnabled;
	}

	public static void setWebSocketEnabled(boolean isWebSocketEnabled)
	{
		JocketService.isWebSocketEnabled = isWebSocketEnabled;
	}

	public static boolean isRunning()
	{
		return isRunning;
	}

    public static boolean isCluster()
    {
        return JocketQueueManager.getQueue() instanceof JocketRedisQueue;
    }
}
