package com.jeedsoft.jocket.connection;

import org.json.JSONObject;

public class JocketCloseReason
{
    private int code;
    
    private String message;
    
    public JocketCloseReason(int code)
	{
		this.code = code;
	}
    
    public JocketCloseReason(int code, String message)
	{
		this.code = code;
		this.message = message;
	}

	public int getCode()
	{
		return code;
	}

	public String getMessage()
	{
		return message;
	}

	public JSONObject toJson()
	{
		JSONObject json = new JSONObject();
		json.put("code", code);
		if (message != null) {
			json.put("message", message);
		}
		return json;
	}

	@Override
	public String toString()
	{
		return toJson().toString();
	}

	public static JocketCloseReason parse(String text)
	{
		if (text == null) {
			return null;
		}
		JSONObject json = new JSONObject(text);
		int code = json.getInt("code");
		String message = json.optString("message", null);
		return new JocketCloseReason(code, message);
	}
}
