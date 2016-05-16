package com.jeedsoft.jocket.message;

import com.jeedsoft.jocket.connection.JocketConnection;

public interface JocketQueue
{
	void start();

	void stop();

	void addSubscriber(JocketConnection cn);

	void removeSubscriber(String sessionId, boolean isPermenant);
	
	void publishMessage(String sessionId, JocketPacket event);

	void publishEvent(String sessionId, JocketPacket event);

	int getSubscriberCount();

	int getQueueCount();
}
