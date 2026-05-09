package no.softwarecontrol.idoc.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class CorsFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization logic if needed
        System.out.println("CorsFilter init");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // Cast request and response to HTTP-specific classes
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        // Debug logging
        String uri = httpRequest.getRequestURI();

        // Slipp gjennom WebSocket-forespørsler uten CORS-behandling
        String upgradeHeader = httpRequest.getHeader("Upgrade");
        if (upgradeHeader != null && upgradeHeader.equalsIgnoreCase("websocket")) {
            System.out.println("=== CorsFilter: WebSocket upgrade detected, passing through ===");
            chain.doFilter(request, response);
            return;
        }

        String[] allowedOrigins = {
                "http://localhost:8181",
                "http://localhost:8080",
                "https://reportserver.idoc.no",
                "https://wasm.idoc.no",
                "https://api.thermidoc.com",
                "https://staging.d1p30z7jidk52m.amplifyapp.com",
        };
        String origin = httpRequest.getHeader("Origin");
        if (origin != null && isAllowedOrigin(origin, allowedOrigins)) {
            // Add CORS headers to the HTTP response
            httpResponse.setHeader("Access-Control-Allow-Origin", origin); // Change as needed
            httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            httpResponse.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
            httpResponse.setHeader("Access-Control-Allow-Credentials", "true"); // If cookies are required
            httpResponse.setHeader("Access-Control-Max-Age", "3600"); // Cache preflight response for 1 hour
        }
        // Handle preflight (OPTIONS) requests explicitly
        String method = httpRequest.getMethod(); // Cast fixed here
        if ("OPTIONS".equalsIgnoreCase(method)) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return; // Stop further processing for preflight requests
        }

        // Continue with the filter chain for other request methods
        chain.doFilter(request, response);
    }
    private boolean isAllowedOrigin(String origin, String[] allowedOrigins) {
        if (origin == null) {
            return false;
        }
        for (String allowedOrigin : allowedOrigins) {
            if (allowedOrigin.equalsIgnoreCase(origin)) {
                return true;
            }
        }
        return origin.matches("^https?://(localhost|127\\.0\\.0\\.1):\\d+$");
    }

    @Override
    public void destroy() {
        // Clean-up logic if needed
    }
}
