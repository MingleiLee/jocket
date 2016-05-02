package com.jeedsoft.jocket.event;

import java.io.Serializable;

import org.json.JSONObject;

public class JocketEvent implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	public static final int TYPE_NORMAL		= 0;
	public static final int TYPE_CLOSE		= 1;
	public static final int TYPE_TIMEOUT	= 2;
	
	private int type;
	
	private String name;
	
	private String data;
	
	public JocketEvent()
	{
	}

	public JocketEvent(int type)
	{
		this.type = type;
	}
	
	public JocketEvent(int type, String name, String data)
	{
		this.type = type;
		this.name = name;
		this.data = data;
	}
	
	public JocketEvent(int type, String name, Object data)
	{
		this.type = type;
		this.name = name;
		this.data = data == null ? null : data.toString();
	}

	public int getType()
	{
		return type;
	}

	public void setType(int type)
	{
		this.type = type;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getData()
	{
		return data;
	}

	public void setData(String data)
	{
		this.data = data;
	}

	public String toJsonString()
	{
		JSONObject json = new JSONObject();
		if (type != 0) {
			json.put("type", type);
		}
		if (name != null) {
			json.put("name", name);
		}
		if (data != null) {
			json.put("data", data);
		}
		return json.toString();
	}

	@Override
	public String toString()
	{
		return toJsonString();
	}

	public static JocketEvent parse(String text)
	{
		JSONObject json = new JSONObject(text);
		JocketEvent event = new JocketEvent();
		event.type = json.optInt("type", 0);
		event.name = json.optString("name");
		event.data = json.optString("data");
		return event;
	}
}
