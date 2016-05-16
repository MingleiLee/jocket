package com.jeedsoft.jocket.endpoint;

import com.jeedsoft.jocket.connection.JocketCloseReason;
import com.jeedsoft.jocket.connection.JocketSession;

public interface JocketEndpoint
{
	void onOpen(JocketSession session);
	
	void onClose(JocketSession session, JocketCloseReason closeReason);
	
	void onMessage(JocketSession session, String name, String data);
}
