package tn.airaware.iam.boundaries;

import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.ConfigProvider;
import tn.airaware.iam.repositories.IamRepository;
import tn.airaware.iam.security.AuthorizationCode;
import tn.airaware.iam.security.JwtManager;

import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * OAuth 2.0 Token Endpoint for AirAware
 * Handles token generation and refresh
 */
@Path("/oauth/token")
public class OAuthTokenEndpoint {
    
    private static final Logger LOGGER = Logger.getLogger(OAuthTokenEndpoint.class.getName());
    private final Set<String> supportedGrantTypes = Set.of("authorization_code", "refresh_token");

    @Inject
    private IamRepository iamRepository;

    @EJB
    private JwtManager jwtManager;

    /**
     * Token endpoint
     * POST /oauth/token
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response token(@FormParam("grant_type") String grantType,
                          @FormParam("code") String authCode,
                          @FormParam("code_verifier") String codeVerifier) {
        
        // Validate grant_type
        if (grantType == null || grantType.isEmpty()) {
            return responseError("invalid_request", "grant_type is required", Response.Status.BAD_REQUEST);
        }

        if (!supportedGrantTypes.contains(grantType)) {
            return responseError("unsupported_grant_type", 
                    "grant_type should be one of: " + supportedGrantTypes, 
                    Response.Status.BAD_REQUEST);
        }

        // Handle refresh_token grant type
        if ("refresh_token".equals(grantType)) {
            return handleRefreshToken(authCode, codeVerifier);
        }

        // Handle authorization_code grant type
        return handleAuthorizationCode(authCode, codeVerifier);
    }

    // ==================== Grant Type Handlers ====================

    /**
     * Handle authorization_code grant type
     */
    private Response handleAuthorizationCode(String authCode, String codeVerifier) {
        try {
            // Decode and verify authorization code with PKCE
            AuthorizationCode decoded = AuthorizationCode.decode(authCode, codeVerifier);
            
            if (decoded == null) {
                return responseError("invalid_grant", 
                        "Invalid authorization code or code verifier", 
                        Response.Status.UNAUTHORIZED);
            }

            // Check if code has expired
            if (Instant.now().getEpochSecond() > decoded.expirationDate()) {
                return responseError("invalid_grant", 
                        "Authorization code has expired", 
                        Response.Status.UNAUTHORIZED);
            }

            String tenantName = decoded.tenantName();
            String username = decoded.identityUsername();
            String approvedScopes = decoded.approvedScopes();

            // Get user roles
            String[] roles = iamRepository.getRoles(username);

            // Generate access token and refresh token
            String accessToken = jwtManager.generateToken(tenantName, username, approvedScopes, roles);
            String refreshToken = jwtManager.generateToken(tenantName, username, approvedScopes, 
                    new String[]{"refresh_role"});

            LOGGER.info("Token issued for user: " + username + " (tenant: " + tenantName + ")");

            return Response.ok(Json.createObjectBuilder()
                            .add("token_type", "Bearer")
                            .add("access_token", accessToken)
                            .add("expires_in", ConfigProvider.getConfig()
                                    .getValue("jwt.lifetime.duration", Integer.class))
                            .add("scope", approvedScopes)
                            .add("refresh_token", refreshToken)
                            .build())
                    .header("Cache-Control", "no-store")
                    .header("Pragma", "no-cache")
                    .build();

        } catch (GeneralSecurityException e) {
            LOGGER.severe("Security exception during token generation: " + e.getMessage());
            return responseError("server_error", 
                    "Failed to generate token", 
                    Response.Status.INTERNAL_SERVER_ERROR);
        } catch (WebApplicationException e) {
            return e.getResponse();
        } catch (Exception e) {
            LOGGER.severe("Unexpected error during token generation: " + e.getMessage());
            return responseError("invalid_request", 
                    "Cannot generate token", 
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Handle refresh_token grant type
     */
    private Response handleRefreshToken(String accessToken, String refreshToken) {
        try {
            // Verify both tokens
            var previousAccessToken = jwtManager.verifyToken(accessToken);
            var previousRefreshToken = jwtManager.verifyToken(refreshToken);

            if (previousAccessToken.isEmpty() || previousRefreshToken.isEmpty()) {
                return responseError("invalid_grant", 
                        "Invalid or expired tokens", 
                        Response.Status.UNAUTHORIZED);
            }

            // Extract claims from tokens
            String tenantId = previousAccessToken.get("tenant_id");
            String subject = previousRefreshToken.get("sub");
            String scopes = previousAccessToken.get("scope");

            // Extract roles from previous access token
            String[] roles = Json.createReader(new StringReader(previousAccessToken.get("groups")))
                    .readArray()
                    .getValuesAs(JsonString.class)
                    .stream()
                    .map(JsonString::getString)
                    .collect(Collectors.toList())
                    .toArray(new String[0]);

            // Verify refresh token claims match access token
            String refreshSubject = previousRefreshToken.get("sub");
            String refreshTenantId = previousRefreshToken.get("tenant_id");
            String refreshScopes = previousRefreshToken.get("scope");

            if (!refreshScopes.equals(scopes) || 
                !refreshTenantId.equals(tenantId) || 
                !refreshSubject.equals(subject)) {
                return responseError("invalid_grant", 
                        "Token mismatch", 
                        Response.Status.UNAUTHORIZED);
            }

            // Generate new tokens
            String newAccessToken = jwtManager.generateToken(tenantId, subject, scopes, roles);
            String newRefreshToken = jwtManager.generateToken(tenantId, subject, scopes, 
                    new String[]{"refresh_role"});

            LOGGER.info("Token refreshed for user: " + subject);

            return Response.ok(Json.createObjectBuilder()
                            .add("token_type", "Bearer")
                            .add("access_token", newAccessToken)
                            .add("expires_in", ConfigProvider.getConfig()
                                    .getValue("jwt.lifetime.duration", Integer.class))
                            .add("scope", scopes)
                            .add("refresh_token", newRefreshToken)
                            .build())
                    .header("Cache-Control", "no-store")
                    .header("Pragma", "no-cache")
                    .build();

        } catch (Exception e) {
            LOGGER.severe("Error refreshing token: " + e.getMessage());
            return responseError("invalid_request", 
                    "Cannot refresh token", 
                    Response.Status.UNAUTHORIZED);
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Create error response
     */
    private Response responseError(String error, String errorDescription, Response.Status status) {
        JsonObject errorResponse = Json.createObjectBuilder()
                .add("error", error)
                .add("error_description", errorDescription)
                .build();
        
        return Response.status(status)
                .entity(errorResponse)
                .build();
    }
}