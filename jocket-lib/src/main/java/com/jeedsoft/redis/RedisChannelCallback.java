package com.jeedsoft.redis;

import org.json.JSONObject;

public interface RedisChannelCallback
{
    void onConnect();
    
    void onDisconnect(Throwable e);
    
    void onMessage(JSONObject message);
}
