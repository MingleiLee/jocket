package com.jeedsoft.jocket.transport.polling;

import java.io.IOException;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;

import com.jeedsoft.jocket.message.JocketPacket;

public class JocketPollingAsyncListener implements AsyncListener
{
	private JocketPollingConnection cn;
	
	public JocketPollingAsyncListener(JocketPollingConnection cn)
	{
		this.cn = cn;
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
		cn.downstream(new JocketPacket(JocketPacket.TYPE_NOOP));
	}
}
