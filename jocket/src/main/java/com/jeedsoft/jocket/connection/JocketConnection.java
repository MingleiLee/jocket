package com.jeedsoft.jocket.connection;

import com.jeedsoft.jocket.event.JocketSubscriber;

public abstract class JocketConnection implements JocketSubscriber
{
	private JocketSession session;

	public JocketConnection(JocketSession session)
	{
		this.session = session;
	}

	public JocketSession getSession()
	{
		return session;
	}

	public void setSession(JocketSession session)
	{
		this.session = session;
	}

	public String getSessionId()
	{
		return session.getId();
	}
}
