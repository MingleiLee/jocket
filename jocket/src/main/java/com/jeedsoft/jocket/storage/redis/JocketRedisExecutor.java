package com.jeedsoft.jocket.storage.redis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisException;

public class JocketRedisExecutor
{
	private static final Logger logger = LoggerFactory.getLogger(JocketRedisExecutor.class);

	public static boolean exists(String key)
	{
		try (Jedis jedis = JocketRedisManager.getJedis()) {
			return jedis.exists(key);
		}
	}

	public static List<String> time()
	{
		try (Jedis jedis = JocketRedisManager.getJedis()) {
			return jedis.time();
		}
	}

	public static long currentTimeMillis()
	{
		List<String> list = time();
		return Long.parseLong(list.get(0)) * 1000 + Long.parseLong(list.get(1)) / 1000;
	}

	public static long del(String... keys)
	{
		try (Jedis jedis = JocketRedisManager.getJedis()) {
			return jedis.del(keys);
		}
	}

	public static Set<String> keys(String pattern)
	{
		try (Jedis jedis = JocketRedisManager.getJedis()) {
			return jedis.keys(pattern);
		}
	}

	public static long sadd(String key, String... members)
	{
		try (Jedis jedis = JocketRedisManager.getJedis()) {
			return jedis.sadd(key, members);
		}
	}

	public static long srem(String key, String... members)
	{
		try (Jedis jedis = JocketRedisManager.getJedis()) {
			return jedis.srem(key, members);
		}
	}

	public static Set<String> smembers(String key)
	{
		try (Jedis jedis = JocketRedisManager.getJedis()) {
			return jedis.smembers(key);
		}
	}

	public static boolean hexists(String key, String field)
	{
		try (Jedis jedis = JocketRedisManager.getJedis()) {
			return jedis.hexists(key, field);
		}
	}

	public static String hget(String key, String field)
	{
		try (Jedis jedis = JocketRedisManager.getJedis()) {
			return jedis.hget(key, field);
		}
	}

	public static long hset(String key, String field, String value)
	{
		try (Jedis jedis = JocketRedisManager.getJedis()) {
			return jedis.hset(key, field, value);
		}
	}

	public static long lpush(String key, String values)
	{
		try (Jedis jedis = JocketRedisManager.getJedis()) {
			return jedis.lpush(key, values);
		}
	}

	public static String rpop(String key)
	{
		try (Jedis jedis = JocketRedisManager.getJedis()) {
			return jedis.rpop(key);
		}
	}

	public static String ltrim(String key, long start, long end)
	{
		try (Jedis jedis = JocketRedisManager.getJedis()) {
			return jedis.ltrim(key, start, end);
		}
	}

	public static Map<String, String> hgetAll(String key)
	{
		try (Jedis jedis = JocketRedisManager.getJedis()) {
			return jedis.hgetAll(key);
		}
	}

	public static String hmset(String key, Map<String, String> hash)
	{
		try (Jedis jedis = JocketRedisManager.getJedis()) {
			return jedis.hmset(key, hash);
		}
	}

	public static long publish(String channel, String message)
	{
		try (Jedis jedis = JocketRedisManager.getJedis()) {
			return jedis.publish(channel, message);
		}
	}

	public static void subscribe(JedisPubSub pubSub, String channel)
	{
		try (Jedis jedis = JocketRedisManager.getJedis()) {
			jedis.subscribe(pubSub, channel);
		}
	}

	//-------------------------------------------------------------------------
	// script methods
	//-------------------------------------------------------------------------

	public static boolean hsetOnKeyExist(String key, String field, String value)
	{
		String script	= "local exists = redis.call('exists', KEYS[1]) \n"
						+ "if exists == 1 then \n"
						+ 	"redis.call('hmset', KEYS[1], unpack(ARGV)) \n"
						+ "end \n"
						+ "return exists";
		return (Long)eval(script, new String[]{key}, new String[]{field, value}) == 1;
	}

	public static JocketCasResult<Long> hcheckAndIncr(String key, String field, long oldValue)
	{
		String script	=	"local v = redis.call('hget', KEYS[1], KEYS[2]) \n"
						+ 	"if v == ARGV[1] or (not v and ARGV[1] == '0') then \n"
						+		"return {1, redis.call('hincrby', KEYS[1], KEYS[2], 1)} \n"
						+	"else \n"
						+		"return {0, v} \n"
						+	"end \n";
		List<?> list	= eval(script, new String[]{key, field}, Long.toString(oldValue));
		Object v		= list.get(1);
		boolean success	= (Long)list.get(0) == 1;
		long value		= v == null ? 0 : v instanceof Long ? (Long)v : Long.parseLong((String)v);
		return new JocketCasResult<Long>(success, value);
	}

	@SuppressWarnings("unchecked")
	private static <T> T eval(String script, String[] keys, String... args)
	{
		List<String> keyList = keys == null ? new ArrayList<String>() : Arrays.asList(keys);
		List<String> argList = Arrays.asList(args);
		try (Jedis jedis = JocketRedisManager.getJedis()) {
			return (T)jedis.eval(script, keyList, argList);
		}
		catch (JedisException e) {
			String message	= "[Jocket] Failed to evel Redis script"
							+ ": script=" + script
							+ ", keys=" + keyList
							+ ", args=" + argList;
			logger.error(message, e);
			throw e;
		}
	}
}
