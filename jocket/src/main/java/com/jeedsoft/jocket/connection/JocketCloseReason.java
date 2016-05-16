package com.jeedsoft.jocket.connection;

import org.json.JSONObject;

public class JocketCloseReason
{
    public static final int NORMAL				= 1000;
    public static final int GOING_AWAY			= 1001;
    public static final int CLOSED_ABNORMALLY	= 1006;
    public static final int NEED_INIT			= 5000;
    public static final int NO_SESSION			= 5001;
    public static final int INIT_FAILED			= 5002;
    public static final int CONNECT_FAILED		= 5100;

    private int code = NORMAL;
    
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
		String message = json.optString("message");
		return new JocketCloseReason(code, message);
	}
}
