package com.jeedsoft.jocket.message;

import org.json.JSONObject;

import java.io.Serializable;

public class JocketPacket implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	public static final String TYPE_CLOSE			= "close";
	public static final String TYPE_PING			= "ping";
	public static final String TYPE_PONG			= "pong";
	public static final String TYPE_UPGRADE			= "upgrade";
	public static final String TYPE_MESSAGE			= "message";
	public static final String TYPE_CONFIRM			= "confirm";
	public static final String TYPE_BROWSER_CLOSE	= "browserclose";
	public static final String TYPE_LOG				= "log";

	private String type;

	private String id;

	private String name;
	
	private String data;

	private JocketPacket()
	{
	}

	public JocketPacket(String type)
	{
		this(type, null, null);
	}

	public JocketPacket(String type, Object data)
	{
		this(type, null, data);
	}

	public JocketPacket(String type, String name, Object data)
	{
		this.type = type;
		this.name = name;
		this.data = data == null ? null : data.toString();
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
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

	public JSONObject toJson()
	{
		JSONObject json = new JSONObject();
		if (type != null) {
			json.put("type", type);
		}
		if (id != null) {
			json.put("id", id);
		}
		if (name != null) {
			json.put("name", name);
		}
		if (data != null) {
			json.put("data", data);
		}
		return json;
	}

	@Override
	public String toString()
	{
		return toJson().toString();
	}

	public static JocketPacket parse(String text)
	{
		if (text == null) {
			return null;
		}
		JSONObject json = new JSONObject(text);
		JocketPacket packet = new JocketPacket();
		packet.type = json.optString("type", null);
		packet.id = json.optString("id", null);
		packet.name = json.optString("name", null);
		packet.data = json.optString("data", null);
		return packet;
	}
}
