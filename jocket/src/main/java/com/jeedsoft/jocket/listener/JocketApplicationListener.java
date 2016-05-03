package com.jeedsoft.jocket.listener;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.jeedsoft.jocket.JocketServer;
import com.jeedsoft.jocket.websocket.JocketWebSocketEndpoint;

//@WebListener
public class JocketApplicationListener implements ServletContextListener
{
	@Override
	public void contextInitialized(ServletContextEvent event)
	{
        ServletContext context =  event.getServletContext();
        JocketWebSocketEndpoint.setApplicationContextPath(context.getContextPath());
		JocketCleaner.start();
		//TODO add auto-deployment codes here
	}

	@Override
	public void contextDestroyed(ServletContextEvent event)
	{
		JocketServer.stop();
	}
}
