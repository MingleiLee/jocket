package com.jeedsoft.jocket.event;

public interface JocketSubscriber
{
	void onEvent(String connectionId, JocketEvent event);

	boolean isAutoNext();
}
