package com.jeedsoft.jocket.event;

public interface JocketQueue
{
	void publish(String connectionId, JocketEvent event);
	
	void subscribe(JocketSubscriber subscriber, String connectionId);
	
	void unsubscribe(JocketSubscriber subscriber, String connectionId);

	void unsubscribe(String connectionId);

	int getQueueCount();

	int getSubscriberCount();
}
