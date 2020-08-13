package com.jeedsoft.jocket.util;

import com.jeedsoft.jocket.util.useragent.UserAgent;

public class UserAgentTest
{
	public static void main(String[] args)
	{
		test("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36");
		System.out.println("--------------");
		test("Mozilla/5.0 (iPhone; CPU iPhone OS 11_0_1 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A403 Safari/604.1");
		System.out.println("--------------");
		test(null);
	}
	
	private static void test(String userAgent)
	{
		UserAgent ua = new UserAgent(userAgent);
		System.out.println(JocketRequestUtil.getDevice(userAgent));
		System.out.printf("%s %s on %s\n", ua.getBrowser().getName(), ua.getBrowserVersion(), ua.getOperatingSystem().getName());
		System.out.println(ua);
		System.out.println(ua.getBrowser().getName());
		System.out.println(ua.getBrowser().getGroup());
		System.out.println(ua.getBrowser().getBrowserType());
		System.out.println(ua.getBrowserVersion());
		System.out.println(ua.getOperatingSystem().getDeviceType());
		System.out.println(ua.getOperatingSystem().getName());
		System.out.println(ua.getOperatingSystem().getGroup());
	}
}
