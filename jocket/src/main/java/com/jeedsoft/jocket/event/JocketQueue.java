package com.jeedsoft.jocket.event;

public interface JocketQueue
{
	void start();

	void stop();
	
	void publish(String sessionId, JocketEvent event);
	
	void subscribe(JocketSubscriber subscriber, String sessionId);

	void unsubscribe(String sessionId, boolean isPermenant);

	int getQueueCount();

	int getSubscriberCount();
}
