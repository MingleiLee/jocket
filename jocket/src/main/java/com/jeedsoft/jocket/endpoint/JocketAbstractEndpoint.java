package com.jeedsoft.jocket.endpoint;

import com.jeedsoft.jocket.connection.JocketCloseReason;
import com.jeedsoft.jocket.connection.JocketSession;

public abstract class JocketAbstractEndpoint
{
	public void onOpen(JocketSession session)
	{
	}
	
	public void onClose(JocketSession session, JocketCloseReason closeReason)
	{
	}
	
	public void onMessage(JocketSession session, String name, String data)
	{
	}
}
