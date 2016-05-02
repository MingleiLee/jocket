package com.jeedsoft.jocket.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.jeedsoft.jocket.exception.JocketException;

public class ReflectUtil
{
	public static <T> T newInstance(Class<T> cls) throws JocketException
	{
		try {
			return getConstructor(cls).newInstance();
		}
		catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
			throw new JocketException("Failed to create new instance of class" + cls.getName(), e);
		}
	}
	
	public static <T> Constructor<T> getConstructor(Class<T> c, Class<?>... parameterTypes) throws JocketException
	{
		try {
			return c.getConstructor(parameterTypes);
		}
		catch (SecurityException | NoSuchMethodException e) {
			throw new JocketException("Failed to get constructor", e);
		}
	}
}
