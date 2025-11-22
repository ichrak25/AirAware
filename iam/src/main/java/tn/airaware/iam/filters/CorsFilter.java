package tn.airaware.iam.filters;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.ConfigProvider;

import java.io.IOException;

/**
 * CORS Filter for AirAware
 * Handles Cross-Origin Resource Sharing
 */
@Provider
public class CorsFilter implements ContainerResponseFilter {

    private static final String ALLOWED_ORIGINS = ConfigProvider.getConfig()
            .getOptionalValue("cors.allowed.origins", String.class)
            .orElse("*");
    
    private static final String ALLOWED_METHODS = ConfigProvider.getConfig()
            .getOptionalValue("cors.allowed.methods", String.class)
            .orElse("GET, POST, PUT, DELETE, OPTIONS");
    
    private static final String ALLOWED_HEADERS = ConfigProvider.getConfig()
            .getOptionalValue("cors.allowed.headers", String.class)
            .orElse("Content-Type, Authorization, X-Requested-With");

    @Override
    public void filter(ContainerRequestContext requestContext, 
                      ContainerResponseContext responseContext) throws IOException {
        
        responseContext.getHeaders().add("Access-Control-Allow-Origin", ALLOWED_ORIGINS);
        responseContext.getHeaders().add("Access-Control-Allow-Methods", ALLOWED_METHODS);
        responseContext.getHeaders().add("Access-Control-Allow-Headers", ALLOWED_HEADERS);
        responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
        responseContext.getHeaders().add("Access-Control-Max-Age", "3600");
    }
}