package com.jeedsoft.jocket.storage.redis;

import com.jeedsoft.jocket.exception.JocketRuntimeException;

import redis.clients.jedis.Jedis;

public class JocketRedisManager
{
	private static JocketRedisDataSource ds;
	
	public static void setDataSource(JocketRedisDataSource ds)
	{
		JocketRedisManager.ds = ds;
	}

	public static Jedis getJedis()
	{
		if (ds == null) {
			throw new JocketRuntimeException("Jocket datasource is not set");
		}
		return ds.getJedis();
	}
}
