package no.softwarecontrol.idoc.filter;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

/**
 * JAX-RS filter for Cognito Bearer JWT.
 * - 401 ved manglende/ugyldig token
 * - 403 ved manglende rettigheter (scope/grupper)
 * Overgang: respekterer AUTH_ENABLE_BASIC slik at BasicAuth kan slippe gjennom.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class CognitoJwtAuthFilter implements ContainerRequestFilter {

    private final AuthConfig config = AuthConfig.load();
    private final CognitoJwtVerifier verifier = new CognitoJwtVerifier(config);

    @Context
    jakarta.servlet.http.HttpServletRequest servletRequest;

    private static final java.util.Set<String> PUBLIC_PATHS = java.util.Set.of(
            "no.softwarecontrol.idoc.entityobject.user/countByLoginName",
            "no.softwarecontrol.idoc.entityobject.user/signupUser",
            "no.softwarecontrol.idoc.entityobject.language"
    );

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // Begrens filteret til JAX-RS-API-et
        String path = requestContext.getUriInfo().getPath(false);
        // Forventer base path: /iDocWebServices/webresources/*
        // ContainerRequestFilter trigges kun for JAX-RS, så dette er i praksis allerede begrenset.
        if (isPublicPath(path)) {
            return;
        }

        if (!config.enableJwt) {
            // JWT deaktivert – tillat videre (f.eks. under migrering/testing)
            return;
        }

        String auth = requestContext.getHeaderString("Authorization");
        if (auth == null || auth.isBlank()) {
            // Overgang: la BasicAuth håndtere dersom aktivert, ellers 401
            if (config.enableBasic && hasBasicHeader()) {
                return;
            }
            abort401(requestContext, "Mangler Authorization header");
            return;
        }

        if (auth.regionMatches(true, 0, "Basic ", 0, 6)) {
            if (config.enableBasic) {
                // La eksisterende BasicAuthenticationFilter håndtere
                return;
            } else {
                abort401(requestContext, "Basic Auth er deaktivert");
                return;
            }
        }

        if (!auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            // Ukjent skjema
            abort401(requestContext, "Authorization må være Bearer");
            return;
        }

        // Verifiser Cognito JWT
        CognitoJwtVerifier.Result res;
        try {
            res = verifier.verifyAccessToken(auth);
        } catch (CognitoJwtVerifier.MissingScopeException mse) {
            abort403(requestContext, mse.getMessage());
            return;
        } catch (Exception ex) {
            abort401(requestContext, "Ugyldig token");
            return;
        }

        // Sett SecurityContext slik at @RolesAllowed virker
        SecurityContext current = requestContext.getSecurityContext();
        Set<String> roles = new HashSet<>(res.groups);
        // Hvis en spesifikk rolle må mappes (typisk ApplicationRole), legg til denne:
        roles.add(config.roleGroup);

        SecurityContext newCtx = new JwtSecurityContext(current, res.subject, roles, "JWT");
        requestContext.setSecurityContext(newCtx);
    }

    private boolean isPublicPath(String path) {
        if (path == null) return false;
        // Matcher enten eksakt eller som prefix med trailing slash (for path-parametre)
        for (String p : PUBLIC_PATHS) {
            if (path.equals(p) || path.startsWith(p + "/")) {
                return true;
            }
        }
        return false;
    }


    private boolean hasBasicHeader() {
        String h = servletRequest != null ? servletRequest.getHeader("Authorization") : null;
        return h != null && h.regionMatches(true, 0, "Basic ", 0, 6);
    }

    private void abort401(ContainerRequestContext ctx, String msg) {
        ctx.abortWith(jakarta.ws.rs.core.Response.status(401)
                .header("WWW-Authenticate", "Bearer realm=\"api\"")
                .entity(msg)
                .build());
    }

    private void abort403(ContainerRequestContext ctx, String msg) {
        ctx.abortWith(jakarta.ws.rs.core.Response.status(403).entity(msg).build());
    }

    static final class JwtSecurityContext implements SecurityContext {
        private final SecurityContext delegate;
        private final Principal principal;
        private final Set<String> roles;
        private final String scheme;

        JwtSecurityContext(SecurityContext delegate, String subject, Set<String> roles, String scheme) {
            this.delegate = delegate;
            this.principal = () -> subject;
            this.roles = roles;
            this.scheme = scheme;
        }

        @Override public Principal getUserPrincipal() { return principal; }
        @Override public boolean isUserInRole(String role) { return roles.contains(role); }
        @Override public boolean isSecure() { return delegate != null && delegate.isSecure(); }
        @Override public String getAuthenticationScheme() { return scheme; }
    }
}
