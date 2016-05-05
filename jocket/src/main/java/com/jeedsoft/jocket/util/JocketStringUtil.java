package com.jeedsoft.jocket.util;

import java.util.ArrayList;
import java.util.List;

public class JocketStringUtil
{
	private static boolean[] regexSpecialChars = new boolean[127];
	
	private static boolean[] jsSpecialChars = new boolean[127];

	static
	{
		String s = "()[]{}^$.?+*|\\";
		for (int i = s.length() - 1; i >= 0; --i) {
			regexSpecialChars[s.charAt(i)] = true;
		}
		s = "\"\\\n\r";
		for (int i = s.length() - 1; i >= 0; --i) {
			jsSpecialChars[s.charAt(i)] = true;
		}
	}
	
	public static boolean isEmpty(String s)
	{
		return s == null || s.isEmpty();
	}
	
	public static String[] split(String s, String splitter)
	{
		if (s == null) {
			throw new IllegalArgumentException("The string to be splitted cannot be null");
		}
		if (splitter == null || splitter.length() == 0) {
			throw new IllegalArgumentException("The string splitter cannot be empty");
		}
		List<String> parts = new ArrayList<>();
		int n = splitter.length();
		int i = 0;
		int j = s.indexOf(splitter);
		while (j != -1) {
			parts.add(s.substring(i, j));
			i = j + n;
			j = s.indexOf(splitter, i);
		}
		parts.add(s.substring(i));
		return parts.toArray(new String[parts.size()]);
	}
	
	public static String escapeJs(String s)
	{
		return escape(s, jsSpecialChars);
	}

	public static String escapeRegex(String s)
	{
		return escape(s, regexSpecialChars);
	}
	
	private static String escape(String s, boolean[] mask)
	{
		if (s == null) {
			return s;
		}
		StringBuilder sb = new StringBuilder(10 * s.length() / 9);
		for (int i = 0, n = s.length(); i < n; ++i) {
			char c = s.charAt(i);
			if (c < mask.length && mask[c]) {
				sb.append("\\");
			}
			sb.append(c);
		}
		return sb.toString();
	}
}
