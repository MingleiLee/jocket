package com.jeedsoft.jocket;

import com.jeedsoft.jocket.connection.JocketStubManager;
import com.jeedsoft.jocket.connection.JocketStubStore;
import com.jeedsoft.jocket.connection.impl.JocketStandaloneStubStore;
import com.jeedsoft.jocket.event.JocketQueue;
import com.jeedsoft.jocket.event.JocketQueueManager;
import com.jeedsoft.jocket.event.impl.JocketStandaloneQueue;

public class JocketGlobalConfig
{
	public static void setStandalone(boolean standalone)
	{
		if (standalone) {
			JocketStubManager.setStore(new JocketStandaloneStubStore());
			JocketQueueManager.setQueue(new JocketStandaloneQueue());
		}
		else {
			//TODO redis
		}
	}

	public static void setStubStore(JocketStubStore store)
	{
		JocketStubManager.setStore(store);
	}
	
	public static void setEventQueue(JocketQueue queue)
	{
		JocketQueueManager.setQueue(queue);
	}
}
