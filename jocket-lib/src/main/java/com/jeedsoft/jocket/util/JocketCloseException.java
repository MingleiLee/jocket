package com.jeedsoft.jocket.util;

public class JocketCloseException extends JocketException
{
	private static final long serialVersionUID = 1L;
	
	private int code;
	
	public JocketCloseException(int code, String message)
	{
		super(message);
		this.code = code;
	}
	
	public JocketCloseException(int code, String message, Throwable e)
	{
		super(message, e);
		this.code = code;
	}

	public int getCode()
	{
		return code;
	}
}
