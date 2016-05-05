package com.jeedsoft.jocket.event;

import java.util.HashMap;
import java.util.Map;

public abstract class JocketAbstractQueue implements JocketQueue
{
	protected final Map<String, JocketSubscriber> subscribers = new HashMap<>();
	
	@Override
	public void subscribe(JocketSubscriber subscriber, String sessionId)
	{
		synchronized(subscribers) {
			subscribers.put(sessionId, subscriber);
		}
		notifySubscriber(sessionId);
	}

	@Override
	public int getSubscriberCount()
	{
		synchronized(subscribers) {
			return subscribers.size();
		}
	}

	protected JocketSubscriber getSubscriber(String sessionId)
	{
		synchronized(subscribers) {
			return subscribers.get(sessionId);
		}
	}

	public void notifySubscriber(String sessionId)
	{
		new Thread(new Consumer(this, sessionId)).start();
	}
	
	protected abstract JocketEvent pollEvent(String sessionId);

	protected static class Consumer implements Runnable
	{
		private JocketAbstractQueue queue;
		
		private String sessionId;
		
		public Consumer(JocketAbstractQueue queue, String sessionId)
		{
			this.queue = queue;
			this.sessionId = sessionId;
		}

		@Override
		public void run()
		{
			JocketSubscriber subscriber = queue.getSubscriber(sessionId);
			if (subscriber != null) {
				synchronized(subscriber) {
					JocketEvent event = queue.pollEvent(sessionId);
					if (event != null) {
						subscriber.onEvent(sessionId, event);
						if (subscriber.isAutoNext()) {
							queue.notifySubscriber(sessionId);
						}
					}
				}
			}
		}
	}
}
