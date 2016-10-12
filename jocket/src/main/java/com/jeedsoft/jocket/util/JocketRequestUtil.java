package com.jeedsoft.jocket.util;

import javax.servlet.http.HttpServletRequest;

public class JocketRequestUtil
{
	public static String getQueryStringWithoutSessionId(HttpServletRequest request)
	{
		String query = request.getQueryString();
		if (query == null) {
			return "";
		}
		else if (query.startsWith("s=")) {
			int index = query.indexOf('&');
			return index == -1 ? "" : query.substring(index + 1);
		}
		else {
			int start = query.indexOf("&s=");
			if (start == -1) {
				return "";
			}
			int end = query.indexOf('&', start + 3);
			if (end == -1) {
				return query.substring(0, start);
			}
			else {
				return query.substring(0, start) + query.substring(end);
			}
		}
	}
}
