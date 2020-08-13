package com.jeedsoft.jocket.storage.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.JocketService;
import com.jeedsoft.jocket.util.JocketClock;
import com.jeedsoft.jocket.util.JocketRuntimeException;

import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

public class JocketRedisManager
{
	private static final Logger logger = LoggerFactory.getLogger(JocketRedisManager.class);
	
	private static JocketRedisDataSource ds;
	   
    public static void initialize(JocketRedisDataSource ds)
    {
        initialize(ds, null);
    }

	public static void initialize(JocketRedisDataSource ds, String clusterId)
	{
		JocketRedisManager.ds = ds;
		JocketClock.setInstance(new JocketClock.RedisClock());
        JocketRedisSubscriber.setClusterId(clusterId);
        JocketService.setEventQueue(new JocketRedisQueue());
        JocketService.setSessionStore(new JocketRedisSessionStore());
	}
	
	public static void initialize(Pool<Jedis> pool)
	{
        initialize(pool, null);
	}
	   
    public static void initialize(Pool<Jedis> pool, String clusterId)
    {
        initialize(new SimpleDataSource(pool), clusterId);
    }

	public static JocketRedisDataSource getDataSource()
	{
	    return ds;
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
