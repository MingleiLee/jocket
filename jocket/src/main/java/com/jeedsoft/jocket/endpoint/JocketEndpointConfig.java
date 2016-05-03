package com.jeedsoft.jocket.endpoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jeedsoft.jocket.exception.JocketException;
import com.jeedsoft.jocket.util.StringUtil;

public class JocketEndpointConfig
{
	private Class<? extends JocketAbstractEndpoint> handlerClass;
	
	private String path;
	
	private Pattern pattern;
	
	private List<String> pathParamNames;
	
	public JocketEndpointConfig(JocketEndpoint annotation, Class<? extends JocketAbstractEndpoint> cls) throws JocketException
	{
		this.handlerClass = cls;
		String annoPath = annotation.value();
		String regex = "(/([^/*\\{\\}]+|\\{[^/*\\{\\}]+\\}))+";
		if (annoPath == null || (!annoPath.equals("/") && !Pattern.matches(regex, annoPath))) {
			throw new JocketException("Jocket path invalid: [" + annoPath + "]");
		}
		if (!annoPath.contains("{")) {
			this.path = annoPath;
		}
		else {
			StringBuilder path = new StringBuilder();
			StringBuilder pattern = new StringBuilder();
			pathParamNames = new ArrayList<>();
			String[] parts = annoPath.split("/");
			for (int i = 1; i < parts.length; ++i) {
				String part = parts[i];
				pattern.append("/");
				path.append("/");
				if (part.startsWith("{")) {
					path.append("*");
					pattern.append("([^/]+)");
					pathParamNames.add(annoPath.substring(1, part.length() - 1));
				}
				else {
					path.append(part);
					pattern.append(StringUtil.escapeRegex(part));
				}
			}
			this.path = path.toString();
			this.pattern = Pattern.compile(pattern.toString());
		}
	}

	public String getPath()
	{
		return path;
	}

	public void setPath(String path)
	{
		this.path = path;
	}

	public Pattern getPattern()
	{
		return pattern;
	}

	public void setPattern(Pattern pattern)
	{
		this.pattern = pattern;
	}

	public List<String> getPathParamNames()
	{
		return pathParamNames;
	}

	public void setPathParamNames(List<String> pathParamNames)
	{
		this.pathParamNames = pathParamNames;
	}

	public Class<? extends JocketAbstractEndpoint> getHandlerClass()
	{
		return handlerClass;
	}

	public void setHandlerClass(Class<? extends JocketAbstractEndpoint> handlerClass)
	{
		this.handlerClass = handlerClass;
	}
	
	public Map<String, String> getPathParameterMap(String path) throws JocketException
	{
		Map<String, String> map = new HashMap<>();
		if (pattern != null) {
			Matcher m = pattern.matcher(path);
			if (!m.matches()) {
				throw new JocketException("Path not matches. pattern=" + pattern + ", path=" + path);
			}
			for (int i = pathParamNames.size() - 1; i >= 0; --i) {
				map.put(pathParamNames.get(i), m.group(i + 1));
			}
		}
		return map;
	}
}
