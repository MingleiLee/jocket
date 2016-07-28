package com.jeedsoft.jocket.endpoint;

import com.jeedsoft.jocket.connection.JocketSession;

public interface JocketMessageHandler
{
	void handle(JocketSession session, String name, String data);
}
