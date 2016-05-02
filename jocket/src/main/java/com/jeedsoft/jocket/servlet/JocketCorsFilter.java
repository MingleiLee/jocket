package com.jeedsoft.jocket.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeedsoft.jocket.util.StringUtil;

@WebFilter(servletNames={"JocketPollingServlet", "JocketPrepareServlet"}, asyncSupported=true)
public final class JocketCorsFilter implements Filter
{
	private static final Logger logger = LoggerFactory.getLogger(JocketCorsFilter.class);

    private static final String METHOD_OPTIONS = "OPTIONS";

    private static final String REQUEST_HEADER_ORIGIN = "Origin";
    private static final String REQUEST_HEADER_ACCESS_CONTROL_REQUEST_METHOD	= "Access-Control-Request-Method";
    private static final String REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS	= "Access-Control-Request-Headers";

    public static final String RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN	= "Access-Control-Allow-Origin";
    public static final String RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    public static final String RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
	}

	@Override
	public void destroy()
	{
	}
    
	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
		throws IOException, ServletException
	{
		HttpServletRequest request = (HttpServletRequest)req;
		HttpServletResponse response = (HttpServletResponse)res;
		response.setHeader("Cache-Control", "no-store, no-cache");
		
		String origin = request.getHeader(REQUEST_HEADER_ORIGIN);
		if (StringUtil.isEmpty(origin)) {
			chain.doFilter(request, response);
			return;
		}
		
		String method = request.getMethod();
		boolean allowed = isOriginAllowed(origin);
		if (logger.isTraceEnabled()) {
			String path = request.getServletPath();
			logger.trace("[Jocket] CORS filter: path={}, method={}, origin={}, allowed={}", path, method, origin, allowed);
		}
		if (!allowed) {
        	handleInvalidCors(request, response, chain);
		}
		else if (METHOD_OPTIONS.equals(method)) {
            String accessControlRequestMethod = request.getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_METHOD);
            String accessControlRequestHeaders = request.getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS);
            response.addHeader(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            response.addHeader(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_METHODS, accessControlRequestMethod);
            response.addHeader(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_HEADERS, accessControlRequestHeaders);
		}
		else {
            response.addHeader(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "*");
    		chain.doFilter(request, response);
		}
	}
	
	private boolean isOriginAllowed(String origin)
	{
		//TODO add rules
		return true;
	}
	
	private void handleInvalidCors(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
    {
        response.setContentType("text/plain");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.resetBuffer();
        if (logger.isErrorEnabled()) {
            String origin = request.getHeader(REQUEST_HEADER_ORIGIN);
            String method = request.getMethod();
            String headers = request.getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS);
            String message = "[Jocket] Invalid CORS request: origin={}, method={}, access-control-request-headers={}";
            logger.error(message, origin, method, headers);
        }
    }
}
