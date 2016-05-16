package com.jeedsoft.jocket.util;

import java.io.IOException;
import java.util.List;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCode;
import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JocketWebSocketUtil
{
	private static final Logger logger = LoggerFactory.getLogger(JocketWebSocketUtil.class);

	public static String getParameter(Session wsSession, String key)
	{
		List<String> list = wsSession.getRequestParameterMap().get(key);
		return list == null ? null : list.get(0);
	}

	public static void close(Session wsSession, int code, String message)
	{
		try {
			wsSession.close(new CloseReason(new JocketCloseCode(code), message));
		}
		catch (IOException e) {
			logger.error("[Jocket] Failed to close WebSocket connection.", e);
		}
	}
	
	private static class JocketCloseCode implements CloseCode
	{
		private int code;
		
		public JocketCloseCode(int code)
		{
			this.code = code;
		}

		@Override
		public int getCode()
		{
			return code;
		}
	}
}