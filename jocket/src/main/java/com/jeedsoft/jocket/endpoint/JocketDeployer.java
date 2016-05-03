package com.jeedsoft.jocket.endpoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig.Builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.exception.JocketException;
import com.jeedsoft.jocket.exception.JocketRuntimeException;
import com.jeedsoft.jocket.util.StringUtil;
import com.jeedsoft.jocket.websocket.JocketWebSocketEndpoint;

public class JocketDeployer
{
	private static final Logger logger = LoggerFactory.getLogger(JocketDeployer.class);

	private static final String WEBSOCKET_CONTAINER_KEY = "javax.websocket.server.ServerContainer";

	private static Node root = new Node("", 0);
	
	public static void deploy(ServletContext context, Class<? extends JocketAbstractEndpoint>[] classes)
	{
        JocketWebSocketEndpoint.setApplicationContextPath(context.getContextPath());
		for (Class<? extends JocketAbstractEndpoint> cls: classes) {
			deploy(context, cls);
		}
	}
	
	private static void deploy(ServletContext context, Class<? extends JocketAbstractEndpoint> cls)
	{
		if (!JocketAbstractEndpoint.class.isAssignableFrom(cls)) {
			String message = "The Jocket class '" + cls.getName() + "' must extends " + JocketAbstractEndpoint.class;
			throw new JocketRuntimeException(message);
		}
        try {
        	JocketEndpoint annotation = cls.getAnnotation(JocketEndpoint.class);
        	String path = annotation.value();
        	logger.info("[Jocket] Deploying: path={}, class={}", path, cls.getName());
            ServerContainer container = (ServerContainer)context.getAttribute(WEBSOCKET_CONTAINER_KEY);
        	Builder builder = Builder.create(JocketWebSocketEndpoint.class, path);
        	container.addEndpoint(builder.build());
        	add(new JocketEndpointConfig(annotation, cls));
        }
        catch (DeploymentException e) {
        	throw new JocketRuntimeException("Failed to deploy Jocket endpoint as WebSocket", e);
        }
		catch (JocketException e) {
        	throw new JocketRuntimeException("Failed to deploy Jocket endpoint: " + e.getMessage(), e);
		}
	}

	private static synchronized void add(JocketEndpointConfig item) throws JocketException
	{
		String[] parts = StringUtil.split(item.getPath().replaceAll("^/$", ""), "/");
		Node node = root;
		for (int i = 1; i < parts.length; ++i) {
			String part = parts[i];
			if (!node.children.containsKey(part)) {
				node.children.put(part, new Node(part, node.level + 1));
			}
			node = node.children.get(part);
		}
		if (node.config != null) {
			String cls1 = node.config.getHandlerClass().getName();
			String cls2 = item.getHandlerClass().getName();
			throw new JocketException("The Jocket path duplicated. class1=" + cls1 + ", class2=" + cls2);
		}
		node.config = item;
	}
	
	public static synchronized JocketEndpointConfig getConfig(String path) throws JocketException
	{
		if (StringUtil.isEmpty(path)) {
			throw new JocketException("The Jocket request path cannot be empty.");
		}
		String[] parts = StringUtil.split(path.replaceAll("^/$|\\?.*", ""), "/");
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
	
	public static synchronized void clear()
	{
		root = new Node("", 0);
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
			sb.append(" (").append(node.config.getHandlerClass().getName()).append(")");
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
