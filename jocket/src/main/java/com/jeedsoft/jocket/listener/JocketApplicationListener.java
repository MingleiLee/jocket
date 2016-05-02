package com.jeedsoft.jocket.listener;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.jeedsoft.jocket.connection.JocketConnectionManager;
import com.jeedsoft.jocket.endpoint.JocketConfigManager;
import com.jeedsoft.jocket.websocket.JocketWebSocketEndpoint;

@WebListener
public class JocketApplicationListener implements ServletContextListener
{
	@Override
	public void contextInitialized(ServletContextEvent event)
	{
        ServletContext context =  event.getServletContext();
        JocketWebSocketEndpoint.setApplicationContextPath(context.getContextPath());
		JocketCleaner.start();
	}

	@Override
	public void contextDestroyed(ServletContextEvent event)
	{
		JocketCleaner.stop();
		JocketConfigManager.clear();
		JocketConnectionManager.clear();
	}
}
