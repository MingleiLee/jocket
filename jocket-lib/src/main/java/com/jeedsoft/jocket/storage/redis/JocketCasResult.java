package com.jeedsoft.jocket.storage.redis;

public class JocketCasResult<T>
{
	private boolean success;
	
	private T value;

	public JocketCasResult(boolean success, T value)
	{
		this.success = success;
		this.value = value;
	}

	public boolean isSuccess()
	{
		return success;
	}

	public T getValue()
	{
		return value;
	}
	
	public String toString()
	{
		return success + ", " + value;
	}
}
