package com.jeedsoft.jocket.exception;

public class JocketException extends Exception
{
	private static final long serialVersionUID = 1L;
	
	public JocketException(String message)
	{
		super(message);
	}
	
	public JocketException(String message, Throwable e)
	{
		super(message, e);
	}
}
