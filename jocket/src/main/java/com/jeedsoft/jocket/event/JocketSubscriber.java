package com.jeedsoft.jocket.event;

public interface JocketSubscriber
{
	void onEvent(String sessionId, JocketEvent event);

	boolean isAutoNext();
}
