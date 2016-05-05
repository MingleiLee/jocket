package com.jeedsoft.jocket.connection;

import org.json.JSONObject;

public class JocketCloseReason
{
    public static final int NORMAL				= 1000;
    public static final int GOING_AWAY			= 1001;
    public static final int CLOSED_ABNORMALLY	= 1006;

    private int code = NORMAL;
    
    private String description;
    
    public JocketCloseReason(int code)
	{
		this.code = code;
	}
    
    public JocketCloseReason(int code, String description)
	{
		this.code = code;
		this.description = description;
	}

	public int getCode()
	{
		return code;
	}

	public String getDescription()
	{
		return description;
	}

	public String toJsonString()
	{
		JSONObject json = new JSONObject();
		json.put("code", code);
		if (description != null) {
			json.put("description", description);
		}
		return json.toString();
	}

	@Override
	public String toString()
	{
		return toJsonString();
	}

	public static JocketCloseReason parse(String text)
	{
		JSONObject json = new JSONObject(text);
		int code = json.getInt("code");
		String description = json.optString("description");
		return new JocketCloseReason(code, description);
	}
}
