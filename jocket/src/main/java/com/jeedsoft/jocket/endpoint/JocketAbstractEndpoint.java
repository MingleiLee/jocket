package com.jeedsoft.jocket.endpoint;

import com.jeedsoft.jocket.connection.JocketConnection;

public abstract class JocketAbstractEndpoint
{
	public void onOpen(JocketConnection connection)
	{
	}
	
	public void onClose(JocketConnection connection, JocketCloseReason closeReason)
	{
	}
	
	public void onMessage(JocketConnection connection, String name, String data)
	{
	}
}
