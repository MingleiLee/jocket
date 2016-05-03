package com.jeedsoft.jocket.storage.redis;

import redis.clients.jedis.Jedis;

public interface JocketRedisDataSource
{
	Jedis getJedis();
}
