package com.jeedsoft.jocket;

import com.jeedsoft.jocket.connection.JocketConnectionManager;
import com.jeedsoft.jocket.connection.JocketSessionManager;
import com.jeedsoft.jocket.connection.JocketSessionStore;
import com.jeedsoft.jocket.endpoint.JocketDeployer;
import com.jeedsoft.jocket.event.JocketQueue;
import com.jeedsoft.jocket.event.JocketQueueManager;
import com.jeedsoft.jocket.storage.JocketCleaner;

public class JocketServer
{
	private static boolean isRunning = false;

	/**
	 * Start the Jocket service
	 * This method should be called in ServletContextListener.contextInitialized()
	 */
	public static void start()
	{
		JocketCleaner.start();
		JocketQueueManager.start();
		isRunning = true;
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
