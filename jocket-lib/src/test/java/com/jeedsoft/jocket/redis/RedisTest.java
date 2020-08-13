package com.jeedsoft.jocket.redis;

import com.jeedsoft.jocket.storage.redis.JocketRedisDataSource;
import com.jeedsoft.jocket.storage.redis.JocketRedisExecutor;
import com.jeedsoft.jocket.storage.redis.JocketRedisManager;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.util.Pool;

public class RedisTest
{
	public static void main(String[] args)
	{
		JedisPoolConfig config = new JedisPoolConfig();
		try  (Pool<Jedis> pool = new JedisPool(config, "127.0.0.1", 6379, 2000, null, 10)) {
			JocketRedisManager.initialize(new RedisDataSource(pool));
			System.out.println(System.currentTimeMillis());
			System.out.println(System.currentTimeMillis());
			System.out.println(System.currentTimeMillis());
			System.out.println(System.currentTimeMillis());
			System.out.println(JocketRedisExecutor.time());
			System.out.println(JocketRedisExecutor.time());
			System.out.println(JocketRedisExecutor.time());
			System.out.println(JocketRedisExecutor.time());
		}
	}
	
	private static class RedisDataSource implements JocketRedisDataSource
	{
		private Pool<Jedis> pool;
		
		public RedisDataSource(Pool<Jedis> pool)
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
