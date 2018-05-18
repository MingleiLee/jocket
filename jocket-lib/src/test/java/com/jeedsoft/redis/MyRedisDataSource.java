package com.jeedsoft.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.util.Pool;

public class MyRedisDataSource implements RedisDataSource
{
    private static Pool<Jedis> pool;

    @Override
    public synchronized Jedis getJedis()
    {
        if (pool == null) {
            JedisPoolConfig config = new JedisPoolConfig();
            pool = new JedisPool(config, "127.0.0.1", 6379, 2000, null, 10);
        }
        return pool.getResource();
    }
}
