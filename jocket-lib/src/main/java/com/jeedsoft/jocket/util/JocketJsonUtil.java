package com.jeedsoft.jocket.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class JocketJsonUtil
{
	public static Map<String, String> toStringMap(JSONObject json)
	{
		Map<String, String> map = new HashMap<>();
		for (Iterator<?> i = json.keys(); i.hasNext();) {
			String key = (String)i.next();
			map.put(key, json.isNull(key) ? null : json.getString(key));
		}
		return map;
	}
	
	public static JSONArray putAll(JSONArray json, Object[] items)
	{
		for (Object item: items) {
			json.put(item);
		}
		return json;
	}
}
