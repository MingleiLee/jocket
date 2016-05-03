package com.jeedsoft.jocket;

import com.jeedsoft.jocket.connection.JocketConnectionManager;
import com.jeedsoft.jocket.connection.JocketStubManager;
import com.jeedsoft.jocket.connection.JocketStubStore;
import com.jeedsoft.jocket.endpoint.JocketDeployer;
import com.jeedsoft.jocket.event.JocketQueue;
import com.jeedsoft.jocket.event.JocketQueueManager;
import com.jeedsoft.jocket.listener.JocketCleaner;

public class JocketServer
{
	private static boolean isRunning = false;

	/**
	 * 
	 */
	public static void start()
	{
		JocketCleaner.start();
		JocketQueueManager.start();
		isRunning = true;
	}
	
	/**
	 * Free the resources
	 * This method should only be invoked in ServletContextListener.contextDestroyed()
	 */
	public static void stop()
	{
		isRunning = false;
		JocketCleaner.stop();
		JocketQueueManager.stop();
		JocketDeployer.clear();
		JocketConnectionManager.clear();
	}
	
	public static void setStubStore(JocketStubStore store)
	{
		JocketStubManager.setStore(store);
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
