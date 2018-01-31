package com.jeedsoft.jocket.message;

public interface JocketQueue
{
	void start();

	void stop();

	void publish(String sessionId, JocketPacket packet);

	JocketPacket poll(String sessionId);

	JocketPacket peek(String sessionId);

	void clear(String sessionId);

	int getQueueCount();
}
