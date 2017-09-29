package com.jeedsoft.jocket.util;

import com.jeedsoft.jocket.storage.redis.JocketRedisExecutor;

public class JocketClock
{
	private static Clock clock = new DefaultClock();
	
	public static long now()
	{
		return clock.now();
	}
	
	public static interface Clock
	{
		long now();
	}
	
	public static void setInstance(Clock clock)
	{
		JocketClock.clock = clock;
	}

	public static class DefaultClock implements Clock
	{
		public long now()
		{
			return System.currentTimeMillis();
		}
	}
	
	public static class RedisClock implements Clock
	{
		public long now()
		{
			return JocketRedisExecutor.currentTimeMillis();
		}
	}
}
