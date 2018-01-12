package com.jeedsoft.jocket.util;

public class JocketArrayUtil
{
	/**
	 * Test if an array contains a value
	 * 
	 * This method should only be used for small array.
	 */
	public static <T> boolean contains(T[] array, T value)
	{
		for (T item: array) {
			if (item == value || (item != null && item.equals(value))) {
				return true;
			}
		}
		return false;
	}
}
