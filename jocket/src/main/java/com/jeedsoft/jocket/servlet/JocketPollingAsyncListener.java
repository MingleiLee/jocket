package com.jeedsoft.jocket.servlet;

import java.io.IOException;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;

import com.jeedsoft.jocket.connection.impl.JocketPollingConnection;
import com.jeedsoft.jocket.event.JocketEvent;

public class JocketPollingAsyncListener implements AsyncListener
{
	private JocketPollingConnection connection;
	
	public JocketPollingAsyncListener(JocketPollingConnection connection)
	{
		this.connection = connection;
	}

	@Override
	public void onStartAsync(AsyncEvent event) throws IOException
	{
	}

	@Override
	public void onComplete(AsyncEvent event) throws IOException
	{
		
	}

	@Override
	public void onError(AsyncEvent event) throws IOException
	{
	}

	@Override
	public void onTimeout(AsyncEvent event) throws IOException
	{
		JocketPollingServlet.downstream(connection, new JocketEvent(JocketEvent.TYPE_TIMEOUT));
	}
}
