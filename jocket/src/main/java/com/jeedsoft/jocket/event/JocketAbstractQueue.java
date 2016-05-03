package com.jeedsoft.jocket.event;

import java.util.HashMap;
import java.util.Map;

public abstract class JocketAbstractQueue implements JocketQueue
{
	protected final Map<String, JocketSubscriber> subscribers = new HashMap<>();
	
	@Override
	public void subscribe(JocketSubscriber subscriber, String connectionId)
	{
		synchronized(subscribers) {
			subscribers.put(connectionId, subscriber);
		}
		notifySubscriber(connectionId);
	}

	@Override
	public int getSubscriberCount()
	{
		synchronized(subscribers) {
			return subscribers.size();
		}
	}

	protected JocketSubscriber getSubscriber(String connectionId)
	{
		synchronized(subscribers) {
			return subscribers.get(connectionId);
		}
	}

	public void notifySubscriber(String connectionId)
	{
		new Thread(new Consumer(this, connectionId)).start();
	}
	
	protected abstract JocketEvent pollEvent(String connectionId);

	protected static class Consumer implements Runnable
	{
		private JocketAbstractQueue impl;
		
		private String connectionId;
		
		public Consumer(JocketAbstractQueue impl, String connectionId)
		{
			this.impl = impl;
			this.connectionId = connectionId;
		}

		@Override
		public void run()
		{
			JocketSubscriber subscriber = impl.getSubscriber(connectionId);
			if (subscriber != null) {
				synchronized(subscriber) {
					JocketEvent event = impl.pollEvent(connectionId);
					if (event != null) {
						subscriber.onEvent(connectionId, event);
						if (subscriber.isAutoNext()) {
							impl.notifySubscriber(connectionId);
						}
					}
				}
			}
		}
	}
}
