package com.jeedsoft.jocket.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class JocketReflection
{
	@SuppressWarnings("unchecked")
	public static <T> Class<T> getClass(String className) throws JocketException
	{
		try {
			return (Class<T>)Class.forName(className);
		}
		catch (ClassNotFoundException e) {
			throw new JocketException("Class not found: " + className, e);
		}
	}
	
	public static <T> Constructor<T> getConstructor(Class<T> c, Class<?>... parameterTypes) throws JocketException
	{
		try {
			return c.getConstructor(parameterTypes);
		}
		catch (SecurityException | NoSuchMethodException e) {
			throw new JocketException("Failed to get constructor in class " + c.getName(), e);
		}
	}
	
	public static Method getMethod(Class<?> c, String methodName, Class<?>... parameterTypes) throws JocketException
	{
		try {
			return c.getMethod(methodName, parameterTypes);
		}
		catch (SecurityException | NoSuchMethodException e) {
			String fullName = c.getName() + "." + methodName;
			throw new JocketException("Failed to get method " + fullName, e);
		}
	}

	public static <T> T newInstance(Class<T> cls) throws JocketException
	{
		try {
			return getConstructor(cls).newInstance();
		}
		catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
			throw new JocketException("Failed to create new instance of class" + cls.getName(), e);
		}
	}
	
	public static Object invoke(Object target, Method method, Object... args) throws JocketException
	{
		try {
			return method.invoke(target, args);
		}
		catch (InvocationTargetException | IllegalArgumentException | IllegalAccessException e) {
			throw new JocketException("Failed to invoke method " + method.getName(), e);
		}
	}
}
