package com.jeedsoft.jocket.transport.polling;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(urlPatterns="/jocket", name="JocketCommonServlet")
public class JocketCommonServlet extends HttpServlet
{
	private static final Logger logger = LoggerFactory.getLogger(JocketCommonServlet.class);
	
	private static final long serialVersionUID = 1L;
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException
	{
		logger.warn("[Jocket] The '/jocket' request is deprecated, please use Jocket by new way.");
		JocketPollingServlet.process(request, response);
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		logger.warn("[Jocket] The '/jocket' request is deprecated, please use Jocket by new way.");
		JocketSendServlet.process(request, response);
	}
}
