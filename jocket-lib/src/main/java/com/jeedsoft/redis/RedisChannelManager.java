package com.jeedsoft.redis;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisChannelManager
{
    private static final Logger logger = LoggerFactory.getLogger(RedisChannelManager.class);

    private static Map<String, RedisChannel> instances = new HashMap<>();
    
    private static ScheduledFuture<?> monitorFuture;
    
    public static synchronized RedisChannel create(String name)
    {
        if (instances.containsKey(name)) {
            throw new RuntimeException("Channel '" + name + "' already created");
        }
        RedisChannel instance = new RedisChannel(name);
        instances.put(name, instance);
        if (monitorFuture == null) {
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            Runnable task = new MonitorTask();
            final long interval = 5000;
            monitorFuture = scheduler.scheduleWithFixedDelay(task, interval, interval, TimeUnit.MILLISECONDS);
        }
        return instance;
    }
    
    public static synchronized void shutdown()
    {
        for (RedisChannel instance: instances.values()) {
            instance.stop();
        }
        instances.clear();
        if (monitorFuture != null) {
            try {
                monitorFuture.cancel(true);
            }
            catch (Throwable e) {
                logger.error("Failed to cancel Redis channel monitor", e);
            }
            finally {
                monitorFuture = null;
            }
        }
    }

    public static void publish(String name, JSONObject message)
    {
        publish(name, message, true);
    }

    public static void publish(String name, JSONObject message, boolean includeSelf)
    {
        RedisChannel instance = instances.get(name);
        if (instance == null) {
            throw new RuntimeException("RedisChannel not found: name=" + name);
        }
        instance.publish(message, includeSelf);
    }

    private static class MonitorTask implements Runnable
    {
        public void run()
        {
            logger.trace("Run Redis subscriber monitor task.");
            synchronized (RedisChannelManager.class) {
                for (RedisChannel instance: instances.values()) {
                    instance.monitor();
                }
            }
        }
    }
}
