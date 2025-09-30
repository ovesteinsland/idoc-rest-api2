package no.softwarecontrol.idoc.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;


/**
 * Created by ove on 12/02/14.
 */
public class BasicAuthenticationFilter implements Filter {

    private String username = "";
    private String password = "";
    private String realm = "Protected";
    private boolean enableBasic = true;
    private List<String> disableBasicOnPaths = List.of();

    private static final java.util.Set<String> PUBLIC_PATHS = java.util.Set.of(
            "/no.softwarecontrol.idoc.entityobject.user/countByLoginName",
            "/no.softwarecontrol.idoc.entityobject.user/signupUser",
            //"/google.cloud.storage/signed-url",
            "no.softwarecontrol.idoc.entityobject.language"
    );


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        username = filterConfig.getInitParameter("username");
        password = filterConfig.getInitParameter("password");

        // Les toggle fra systemproperty/env: AUTH_ENABLE_BASIC (default true for bakoverkomp)
        String v = System.getProperty("AUTH_ENABLE_BASIC");
        if (v == null || v.isBlank()) v = System.getenv("AUTH_ENABLE_BASIC");
        if (v != null && !v.isBlank()) {
            enableBasic = Boolean.parseBoolean(v);
        }
        String disabled = filterConfig.getInitParameter("disableBasicOnPaths");
        if (disabled != null && !disabled.isBlank()) {
            disableBasicOnPaths = Arrays.stream(disabled.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        if (!enableBasic) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        String path = request.getRequestURI();
        String authHeader = request.getHeader("Authorization");
        // Slipp gjennom Bearer slik at CognitoJwtAuthFilter kan håndtere det
        if (authHeader != null && authHeader.regionMatches(true, 0, "Bearer", 0, 6)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // Slipp gjennom offentlig whiteliste uten noen auth-krav
        if (isPublicPath(request)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }


        // Nekt Basic på konfigurerte stier (f.eks. SmsFacadeREST)
        if (isBasicDisabledForPath(path)) {
            if (authHeader != null && authHeader.regionMatches(true, 0, "Basic", 0, 5)) {
                unauthorized(response, "Basic authentication er ikke tillatt for denne ressursen");
                return;
            }
            // Ikke forsøk Basic her; la neste filter/ sikkerhetsmekanisme håndtere
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }


        if (authHeader != null) {
            StringTokenizer st = new StringTokenizer(authHeader);
            if (st.hasMoreTokens()) {
                String basic = st.nextToken();

                if (basic.equalsIgnoreCase("Basic")) {
                    try {
                        String credentials = new String(Base64.decodeBase64(st.nextToken()), "UTF-8");
                        int p = credentials.indexOf(":");
                        if (p != -1) {
                            String _username = credentials.substring(0, p).trim();
                            String _password = credentials.substring(p + 1).trim();

                            if (!username.equals(_username) || !password.equals(_password)) {
                                unauthorized(response, "Bad credentials");
                            }
//                            System.out.println("request.getRequestURI() = "
//                                    + request.getMethod()
//                                    + ":" + response.getStatus() + " - "
//                                    + request.getRequestURI());
                            filterChain.doFilter(servletRequest, servletResponse);
                        } else {
                            unauthorized(response, "Invalid authentication token");
                        }
                    } catch (UnsupportedEncodingException e) {
                        throw new Error("Couldn't retrieve authentication", e);
                    }
                }
            }
        } else {
            unauthorized(response);
        }
    }

    @Override
    public void destroy() {
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setHeader("WWW-Authenticate", "Basic realm=\"" + realm + "\"");
        response.sendError(401, message);
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        unauthorized(response, "Unauthorized");
    }

    private boolean isBasicDisabledForPath(String requestUri) {
        if (disableBasicOnPaths.isEmpty() || requestUri == null) return false;
        // Enkel prefix-match; juster etter behov
        for (String p : disableBasicOnPaths) {
            if (requestUri.startsWith(p)) {
                return true;
            }
        }
        return false;
    }

    // Matcher offentlig whitelist på en robust måte uavhengig av kontekstpath og case
    private boolean isPublicPath(HttpServletRequest request) {
        String context = request.getContextPath(); // f.eks. /iDocWebServices
        String uri = request.getRequestURI();      // f.eks. /iDocWebServices/webresources/no.softwarecontrol.../countByLoginName/x
        String pathNoCtx = (context != null && !context.isEmpty() && uri.startsWith(context))
                ? uri.substring(context.length())
                : uri;
        String lower = pathNoCtx.toLowerCase(java.util.Locale.ROOT);
        // Tillat både eksakt og prefix (for path-parametre)
        for (String p : PUBLIC_PATHS) {
            if (lower.equals(p) || lower.contains(p.toLowerCase())) {
                return true;
            }
        }
        return false;
    }


}
