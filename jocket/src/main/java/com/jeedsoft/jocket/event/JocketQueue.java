package com.jeedsoft.jocket.event;

public interface JocketQueue
{
	void start();

	void stop();
	
	void publish(String connectionId, JocketEvent event);
	
	void subscribe(JocketSubscriber subscriber, String connectionId);

	void unsubscribe(String connectionId, boolean isPermenant);

	int getQueueCount();

	int getSubscriberCount();
}
