package com.jeedsoft.jocket.util;

import java.util.concurrent.atomic.AtomicLong;

public class JocketThreadUtil
{
	private static final AtomicLong serial = new AtomicLong();

	public static void updateWebSocketThreadName(String method)
	{
		Thread.currentThread().setName("Jocket-WS-" + serial.incrementAndGet() + ":" + method);
	}

	public static void updatePollingThreadName(String path)
	{
		Thread.currentThread().setName("Jocket-PL-" + serial.incrementAndGet() + ":" + path);
	}
}
