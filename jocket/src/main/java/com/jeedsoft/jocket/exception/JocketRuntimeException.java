package com.jeedsoft.jocket.exception;

public class JocketRuntimeException extends RuntimeException
{
	private static final long serialVersionUID = 1L;
	
	public JocketRuntimeException(String message)
	{
		super(message);
	}
	
	public JocketRuntimeException(String message, Throwable e)
	{
		super(message, e);
	}
}
