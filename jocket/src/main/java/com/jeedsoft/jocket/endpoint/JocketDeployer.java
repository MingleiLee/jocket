package com.jeedsoft.jocket.endpoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jeedsoft.jocket.util.JocketException;
import com.jeedsoft.jocket.util.JocketStringUtil;

public class JocketDeployer
{
	private static Node root = new Node("", 0);
	
	private static Map<String, Class<? extends JocketAbstractEndpoint>> classMap = new HashMap<>();

	public static void deploy(Class<? extends JocketAbstractEndpoint>[] classes) throws JocketException
	{
		for (Class<? extends JocketAbstractEndpoint> cls: classes) {
	    	JocketEndpoint annotation = cls.getAnnotation(JocketEndpoint.class);
	    	addToTree(new JocketEndpointConfig(annotation, cls));
	    	classMap.put(cls.getName(), cls);
		}
	}

	private static synchronized void addToTree(JocketEndpointConfig item) throws JocketException
	{
		String[] parts = JocketStringUtil.split(item.getPath().replaceAll("^/$", ""), "/");
		Node node = root;
		for (int i = 1; i < parts.length; ++i) {
			String part = parts[i];
			if (!node.children.containsKey(part)) {
				node.children.put(part, new Node(part, node.level + 1));
			}
			node = node.children.get(part);
		}
		if (node.config != null) {
			String cls1 = node.config.getEndpointClassName();
			String cls2 = item.getEndpointClassName();
			throw new JocketException("The Jocket path duplicated. class1=" + cls1 + ", class2=" + cls2);
		}
		node.config = item;
	}
	
	public static synchronized JocketEndpointConfig getConfig(String path) throws JocketException
	{
		if (JocketStringUtil.isEmpty(path)) {
			throw new JocketException("The Jocket request path cannot be empty.");
		}
		String[] parts = JocketStringUtil.split(path.replaceAll("^/$|\\?.*", ""), "/");
		List<Node> stack = new ArrayList<>(parts.length);
		stack.add(root);
		while (!stack.isEmpty()) {
			Node node = stack.remove(stack.size() - 1);
			for (int i = node.level + 1; i < parts.length && node != null; ++i) {
				Node starNode = node.children.get("*");
				if (starNode != null) {
					stack.add(starNode);
				}
				node = node.children.get(parts[i]);
			}
			if (node != null && node.level == parts.length - 1 && node.config != null) {
				return node.config;
			}
		}
		throw new JocketException("Jocket path not found: [" + path + "]");
	}
	
	public static Class<? extends JocketAbstractEndpoint> getEndpointClass(String className)
	{
		return classMap.get(className);
	}
	
	public static synchronized void clear()
	{
		root = new Node("", 0);
		classMap.clear();
	}

	public static synchronized List<String> getPaths()
	{
		List<String> paths = new ArrayList<>();
		fillPaths(root, paths);
		Collections.sort(paths);
		return paths;
	}
	
	private static void fillPaths(Node node, List<String> paths)
	{
		if (node.config != null) {
			paths.add(node.config.getAnnotationPath());
		}
		if (!node.children.isEmpty()) {
			for (Node child: node.children.values()) {
				fillPaths(child, paths);
			}
		}
	}
	
	public static synchronized String getTreeText()
	{
		StringBuilder sb = new StringBuilder();
		fillTreeText(root, sb, new ArrayList<Boolean>());
		return sb.toString();
	}
	
	private static void fillTreeText(Node node, StringBuilder sb, List<Boolean> isAncestorLast)
	{
		for (int i = 0, n = isAncestorLast.size() - 1; i < n; ++i) {
			sb.append(isAncestorLast.get(i) ? "    " : "|   ");
		}
		if (node.level > 0) {
			sb.append("|---");
		}
		sb.append(node);
		if (node.config != null) {
			sb.append(" (").append(node.config.getEndpointClassName()).append(")");
		}
		sb.append("\n");
		if (!node.children.isEmpty()) {
			Node[] children = node.children.values().toArray(new Node[node.children.size()]);
			Arrays.sort(children);
			for (Node child: children) {
				isAncestorLast.add(child == children[children.length - 1]);
				fillTreeText(child, sb, isAncestorLast);
				isAncestorLast.remove(isAncestorLast.size() - 1);
			}
		}
	}
	
	private static class Node implements Comparable<Node>
	{
		private String name;
		
		private int level;
		
		private JocketEndpointConfig config;
		
		private Map<String, Node> children = new HashMap<>();

		public Node(String name, int level)
		{
			this.name = name;
			this.level = level;
		}

		@Override
		public String toString()
		{
			return level == 0 ? "/" : name;
		}
		
		@Override
		public int compareTo(Node another)
		{
			if (this.name.equals("*")) {
				return -1;
			}
			else if (another.name.equals("*")) {
				return 1;
			}
			else {
				return this.name.compareTo(another.name);
			}
		}
	}
}
