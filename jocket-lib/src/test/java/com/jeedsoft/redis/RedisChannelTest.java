package com.jeedsoft.redis;

import org.json.JSONObject;
import org.junit.Test;

import com.jeedsoft.redis.RedisChannel.Callback;

public class RedisChannelTest
{
    @Test
    public void test() throws Exception
    {
        final RedisDataSource ds = new MyRedisDataSource();
        for (int i = 0; i < 3; ++i) {
            final long sleepTime = 1000 + i * 1000;
            final String channel = "channel-" + i;
            Thread thread = new Thread() {
                public void run() {
                    RedisChannel subscriber = RedisChannelManager.create(channel);
                    subscriber.setDataSource(ds);
                    subscriber.setCallback(new Callback() {
                        public void onMessage(JSONObject message) {
                            System.out.printf("[%s] message received: %s\n", getName(), message);
                        }
                    });
                    subscriber.start();
                    RedisChannelTest.sleep(sleepTime);
                    subscriber.publish(new JSONObject().put("a", sleepTime));
                    subscriber.publish(new JSONObject().put("b", 1), false);
                }
            };
            thread.setName("TEST" + i);
            thread.start();
        }
        RedisChannelTest.sleep(10_000);
        System.out.println("shutdown");
        RedisChannelManager.shutdown();
        RedisChannelTest.sleep(10_000);
    }
    
    private static void sleep(long ms)
    {
        try {
            Thread.sleep(ms);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
