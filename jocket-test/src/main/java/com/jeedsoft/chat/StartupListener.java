package com.jeedsoft.chat;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.JocketService;
import com.jeedsoft.jocket.endpoint.JocketDeployer;
import com.jeedsoft.jocket.endpoint.JocketEndpoint;
import com.jeedsoft.jocket.util.JocketException;

@WebListener
public class StartupListener implements ServletContextListener
{
	private static final Logger logger = LoggerFactory.getLogger(StartupListener.class);
	
	@Override
	@SuppressWarnings("unchecked")
	public void contextInitialized(ServletContextEvent event)
	{
		try {
	        ServletContext context = event.getServletContext();
	        Class<? extends JocketEndpoint>[] classes = new Class[]{SimpleChat.class};
	        JocketDeployer.deploy(classes);
	        // JocketRedisManager.initialize(new JedisPool("127.0.0.1"), "12345");
			JocketService.start(context);
		}
		catch (JocketException e) {
	        logger.error("Failed to deploy Jocket endpoints", e);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent event)
	{
		JocketService.stop();
//		JocketRedisManager.destroy();
	}
}
