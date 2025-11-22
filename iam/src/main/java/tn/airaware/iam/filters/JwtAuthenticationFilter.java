package tn.airaware.iam.filters;

import jakarta.annotation.Priority;
import jakarta.ejb.EJB;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import tn.airaware.iam.security.JwtManager;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * JWT Authentication Filter for AirAware
 * Validates JWT tokens on protected endpoints
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthenticationFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = Logger.getLogger(JwtAuthenticationFilter.class.getName());

    @EJB
    private JwtManager jwtManager;

    // Public endpoints that don't require authentication
    private static final String[] PUBLIC_PATHS = {
            "/register",
            "/authorize",
            "/login",
            "/oauth/token",
            "/activate"
    };

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();

        // Skip authentication for public endpoints
        if (isPublicPath(path)) {
            return;
        }

        // Get Authorization header
        String authorizationHeader = requestContext.getHeaderString("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            LOGGER.warning("Missing or invalid Authorization header for path: " + path);
            abortWithUnauthorized(requestContext, "Missing or invalid Authorization header");
            return;
        }

        String token = authorizationHeader.substring("Bearer ".length());

        try {
            // Verify token
            Map<String, String> claims = jwtManager.verifyToken(token);

            if (claims.isEmpty()) {
                LOGGER.warning("Invalid or expired token for path: " + path);
                abortWithUnauthorized(requestContext, "Invalid or expired token");
                return;
            }

            // Store claims in request context for use in endpoints
            requestContext.setProperty("jwt.claims", claims);
            requestContext.setProperty("jwt.subject", claims.get("sub"));
            requestContext.setProperty("jwt.tenant_id", claims.get("tenant_id"));
            requestContext.setProperty("jwt.scopes", claims.get("scope"));
            requestContext.setProperty("jwt.roles", claims.get("groups"));

            LOGGER.fine("Request authenticated for user: " + claims.get("sub"));

        } catch (Exception e) {
            LOGGER.severe("Error validating token: " + e.getMessage());
            abortWithUnauthorized(requestContext, "Token validation failed");
        }
    }

    private boolean isPublicPath(String path) {
        for (String publicPath : PUBLIC_PATHS) {
            if (path.startsWith(publicPath)) {
                return true;
            }
        }
        return false;
    }

    private void abortWithUnauthorized(ContainerRequestContext requestContext, String message) {
        requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\": \"" + message + "\"}")
                        .build()
        );
    }
}