package com.jeedsoft.jocket.util;

import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JocketClientLogger
{
    private static final Logger logger = LoggerFactory.getLogger(JocketClientLogger.class);

    public static void log(String sessionId, JSONArray logs)
	{
	    if (logger.isTraceEnabled()) {
            for (Object row: logs) {
                logger.trace("Client log: sid={}, log={}", sessionId, row);
            }
        }
	}
}
