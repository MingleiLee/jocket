package com.jeedsoft.jocket.storage.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.util.JocketRuntimeException;
import com.jeedsoft.jocket.util.JocketClock;

import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

public class JocketRedisManager
{
	private static final Logger logger = LoggerFactory.getLogger(JocketRedisManager.class);
	
	private static JocketRedisDataSource ds;
	
	public static void initialize(JocketRedisDataSource ds)
	{
		JocketRedisManager.ds = ds;
		JocketClock.setInstance(new JocketClock.RedisClock());
	}
	
	public static void initialize(Pool<Jedis> pool)
	{
		JocketRedisManager.ds = new SimpleDataSource(pool);
		JocketClock.setInstance(new JocketClock.RedisClock());
	}
	
	public static void destroy()
	{
		if (ds instanceof SimpleDataSource) {
			try {
				((SimpleDataSource)ds).pool.close();
			}
			catch (Throwable e) {
				logger.error("[Jocket] Failed to close Jedis pool", e);
			}
		}
	}

	public static Jedis getJedis()
	{
		if (ds == null) {
			throw new JocketRuntimeException("Jocket datasource is not set");
		}
		return ds.getJedis();
	}

	private static class SimpleDataSource implements JocketRedisDataSource
	{
		private Pool<Jedis> pool;
		
		public SimpleDataSource(Pool<Jedis> pool)
		{
			this.pool = pool;
		}
		
		@Override
		public Jedis getJedis()
		{
			return pool.getResource();
		}
	}
}
