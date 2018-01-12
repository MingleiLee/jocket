package com.jeedsoft.jocket.util;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.util.useragent.Browser;
import com.jeedsoft.jocket.util.useragent.BrowserType;
import com.jeedsoft.jocket.util.useragent.DeviceType;
import com.jeedsoft.jocket.util.useragent.OperatingSystem;
import com.jeedsoft.jocket.util.useragent.UserAgent;

public class JocketRequestUtil
{
	private static final Logger logger = LoggerFactory.getLogger(JocketRequestUtil.class);

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
	
	public static String getDevice(String userAgent)
	{
		try {
			UserAgent ua = new UserAgent(userAgent);
			Browser browser = ua.getBrowser();
			OperatingSystem os = ua.getOperatingSystem();
			if (browser.getBrowserType() == BrowserType.UNKNOWN && os.getDeviceType() == DeviceType.UNKNOWN) {
				return "Unknown";
			}
			StringBuilder sb = new StringBuilder();
			sb.append(browser.getName());
			if (ua.getBrowserVersion() != null) {
				sb.append(" ").append(ua.getBrowserVersion());
			}
			sb.append(" on ").append(os.getName());
			return sb.toString();
		}
		catch (Exception e) {
			logger.debug("[Jocket] Failed to detect device", e);
			return "Unknown";
		}
	}
}
