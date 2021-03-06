package com.jeedsoft.jocket.endpoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jeedsoft.jocket.util.JocketException;
import com.jeedsoft.jocket.util.JocketStringUtil;

public class JocketEndpointConfig
{
	private String endpointClassName;
	
	private String annotationPath;
	
	private String path;
	
	private Pattern pattern;
	
	private List<String> pathParamNames;
	
	public JocketEndpointConfig(JocketServerEndpoint annotation, Class<? extends JocketEndpoint> cls) throws JocketException
	{
		this.endpointClassName = cls.getName();
		this.annotationPath = annotation.value();
		String annoPath = this.annotationPath;
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
					pattern.append(JocketStringUtil.escapeRegex(part));
				}
			}
			this.path = path.toString();
			this.pattern = Pattern.compile(pattern.toString());
		}
	}

	public String getAnnotationPath()
	{
		return annotationPath;
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

	public String getEndpointClassName()
	{
		return endpointClassName;
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
