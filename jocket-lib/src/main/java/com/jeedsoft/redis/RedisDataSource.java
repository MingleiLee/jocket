package com.jeedsoft.redis;

import redis.clients.jedis.Jedis;

public interface RedisDataSource
{
	Jedis getJedis();
}
