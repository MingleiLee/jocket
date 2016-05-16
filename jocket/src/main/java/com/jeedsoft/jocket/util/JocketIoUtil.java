package com.jeedsoft.jocket.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

public class JocketIoUtil
{
	public static String readText(HttpServletRequest request) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		try (BufferedReader reader = request.getReader()) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (sb.length() > 0) {
					sb.append("\n");
				}
				sb.append(line);
			}
		}
		return sb.toString();
	}

	public static void writeJson(HttpServletResponse response, JSONObject json) throws IOException
	{
		response.setContentType("application/json; charset=utf-8");
		try (PrintWriter out = response.getWriter()) {
			out.print(json.toString());
			out.flush();
		}
	}

	public static void writeText(HttpServletResponse response, String text) throws IOException
	{
		response.setContentType("text/plain; charset=utf-8");
		try (PrintWriter out = response.getWriter()) {
			out.print(text);
			out.flush();
		}
	}
}
