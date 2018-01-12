package com.jeedsoft.jocket.message;

import com.jeedsoft.jocket.connection.JocketConnection;

public interface JocketQueue
{
	void start();

	void stop();

	void addSubscriber(JocketConnection cn);

	void removeSubscriber(String sessionId, boolean isPermanent);
	
	void publishMessage(String sessionId, JocketPacket packet);

	void publishEvent(String sessionId, JocketPacket packet);

	int getSubscriberCount();

	int getQueueCount();
}
