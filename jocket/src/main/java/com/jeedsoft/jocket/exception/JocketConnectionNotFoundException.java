package com.jeedsoft.jocket.exception;

public class JocketConnectionNotFoundException extends JocketException
{
	private static final long serialVersionUID = 1L;
	
	public JocketConnectionNotFoundException(String connectionId)
	{
		super("Jocket connection not found: id=" + connectionId);
	}
}
