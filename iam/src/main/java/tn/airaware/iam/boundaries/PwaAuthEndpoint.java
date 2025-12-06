package tn.airaware.iam.boundaries;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tn.airaware.core.entities.Identity;
import tn.airaware.iam.enums.Role;
import tn.airaware.iam.repositories.IdentityRepository;
import tn.airaware.iam.security.JwtManager;
import tn.airaware.iam.services.IdentityServices;
import tn.airaware.core.security.Argon2Utils;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PWA Authentication Endpoint for AirAware
 * Provides JSON-based REST API for PWA login, registration, and token management
 * 
 * Endpoints:
 * - POST /oauth/login - Direct login with username/password, returns JWT tokens
 * - POST /register - Register new user
 * - POST /register/activate - Activate account with code
 * - POST /register/resend - Resend activation code
 * - GET /oauth/userinfo - Get current user info from token
 */
@Path("/")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PwaAuthEndpoint {

    private static final Logger LOGGER = Logger.getLogger(PwaAuthEndpoint.class.getName());
    private static final String DEFAULT_TENANT = "airaware-default";
    
    @Inject
    IdentityRepository identityRepository;

    @Inject
    IdentityServices identityServices;

    @Inject
    Argon2Utils argon2Utils;

    @Inject
    JwtManager jwtManager;

    // ==================== LOGIN ====================

    /**
     * Direct login endpoint for PWA
     * POST /oauth/login
     * Request: { "username": "...", "password": "..." }
     * Response: { "access_token": "...", "refresh_token": "...", "token_type": "Bearer", "expires_in": 3600 }
     */
    @POST
    @Path("/oauth/login")
    public Response login(LoginRequest request) {
        try {
            LOGGER.info("Login attempt for user: " + request.username);

            // Validate input
            if (request.username == null || request.username.trim().isEmpty()) {
                return errorResponse(400, "Username is required");
            }
            if (request.password == null || request.password.isEmpty()) {
                return errorResponse(400, "Password is required");
            }

            // Find user
            Optional<Identity> identityOpt = identityRepository.findByUsername(request.username.trim());
            if (identityOpt.isEmpty()) {
                LOGGER.warning("User not found: " + request.username);
                return errorResponse(401, "Invalid username or password");
            }

            Identity identity = identityOpt.get();

            // Check if account is activated
            if (!identity.isAccountActivated()) {
                LOGGER.warning("Account not activated: " + request.username);
                return errorResponse(403, "Account not activated. Please check your email for the activation code.");
            }

            // Verify password
            if (!argon2Utils.check(identity.getPassword(), request.password.toCharArray())) {
                LOGGER.warning("Invalid password for user: " + request.username);
                return errorResponse(401, "Invalid username or password");
            }

            // Generate tokens
            String[] roles = getRolesArray(identity.getRoles());
            String scopes = identity.getScopes() != null ? identity.getScopes() : "sensor:read dashboard:view";
            
            String accessToken = jwtManager.generateToken(DEFAULT_TENANT, identity.getUsername(), scopes, roles);
            String refreshToken = generateRefreshToken(identity);

            LOGGER.info("Login successful for user: " + request.username);

            // Return tokens
            JsonObject response = Json.createObjectBuilder()
                    .add("access_token", accessToken)
                    .add("refresh_token", refreshToken)
                    .add("token_type", "Bearer")
                    .add("expires_in", 3600)
                    .add("name", identity.getUsername())
                    .add("email", identity.getEmail() != null ? identity.getEmail() : "")
                    .add("roles", Role.toString(identity.getRoles()))
                    .build();

            return Response.ok(response).build();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Login error: " + e.getMessage(), e);
            return errorResponse(500, "An error occurred during login");
        }
    }

    // ==================== REGISTRATION ====================

    /**
     * Register new user
     * POST /register
     * Request: { "username": "...", "email": "...", "password": "..." }
     * Response: { "message": "..." } or { "error": "..." }
     */
    @POST
    @Path("/register")
    public Response register(RegisterRequest request) {
        try {
            LOGGER.info("Registration attempt for: " + request.username);

            // Validate input
            if (request.username == null || request.username.trim().isEmpty()) {
                return errorResponse(400, "Username is required");
            }
            if (request.email == null || request.email.trim().isEmpty()) {
                return errorResponse(400, "Email is required");
            }
            if (request.password == null || request.password.isEmpty()) {
                return errorResponse(400, "Password is required");
            }

            // Register identity (this will validate and send activation email)
            identityServices.registerIdentity(request.username.trim(), request.password, request.email.trim());

            LOGGER.info("Registration successful for: " + request.username);

            JsonObject response = Json.createObjectBuilder()
                    .add("message", "Registration successful! Please check your email for the activation code.")
                    .add("email", request.email)
                    .build();

            return Response.status(201).entity(response).build();

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Registration error: " + e.getMessage());
            return errorResponse(400, e.getMessage());
        }
    }

    /**
     * Activate account with activation code
     * POST /register/activate
     * Request: { "code": "123456", "email": "...", "username": "..." }
     * Response: { "access_token": "..." } or { "message": "..." } or { "error": "..." }
     */
    @POST
    @Path("/register/activate")
    public Response activate(ActivateRequest request) {
        try {
            LOGGER.info("Activation attempt with code: " + request.code);

            // Validate input
            if (request.code == null || request.code.trim().isEmpty()) {
                return errorResponse(400, "Activation code is required");
            }

            // Activate identity
            identityServices.activateIdentity(request.code.trim());

            // Try to find the user and auto-login
            Optional<Identity> identityOpt = Optional.empty();
            if (request.username != null && !request.username.isEmpty()) {
                identityOpt = identityRepository.findByUsername(request.username);
            } else if (request.email != null && !request.email.isEmpty()) {
                identityOpt = identityRepository.findByEmail(request.email);
            }

            if (identityOpt.isPresent()) {
                Identity identity = identityOpt.get();
                
                // Generate tokens for auto-login
                String[] roles = getRolesArray(identity.getRoles());
                String scopes = identity.getScopes() != null ? identity.getScopes() : "sensor:read dashboard:view";
                String accessToken = jwtManager.generateToken(DEFAULT_TENANT, identity.getUsername(), scopes, roles);
                String refreshToken = generateRefreshToken(identity);

                LOGGER.info("Account activated and auto-login for: " + identity.getUsername());

                JsonObject response = Json.createObjectBuilder()
                        .add("message", "Account activated successfully!")
                        .add("access_token", accessToken)
                        .add("refresh_token", refreshToken)
                        .add("token_type", "Bearer")
                        .add("expires_in", 3600)
                        .add("name", identity.getUsername())
                        .build();

                return Response.ok(response).build();
            }

            // If can't auto-login, just return success message
            JsonObject response = Json.createObjectBuilder()
                    .add("message", "Account activated successfully! Please sign in.")
                    .build();

            return Response.ok(response).build();

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Activation error: " + e.getMessage());
            return errorResponse(400, e.getMessage());
        }
    }

    /**
     * Resend activation code
     * POST /register/resend
     * Request: { "email": "..." }
     * Response: { "message": "..." } or { "error": "..." }
     */
    @POST
    @Path("/register/resend")
    public Response resendActivationCode(ResendRequest request) {
        try {
            LOGGER.info("Resend activation code request for: " + request.email);

            // Validate input
            if (request.email == null || request.email.trim().isEmpty()) {
                return errorResponse(400, "Email is required");
            }

            // Use the service to resend activation code
            identityServices.resendActivationCode(request.email.trim());

            JsonObject response = Json.createObjectBuilder()
                    .add("message", "A new activation code has been sent to your email.")
                    .build();

            return Response.ok(response).build();

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Resend activation error: " + e.getMessage());
            return errorResponse(400, e.getMessage());
        }
    }

    // ==================== USER INFO ====================

    /**
     * Get current user info from JWT token
     * GET /oauth/userinfo
     * Header: Authorization: Bearer <token>
     * Response: { "sub": "...", "username": "...", "email": "...", "roles": "..." }
     */
    @GET
    @Path("/oauth/userinfo")
    public Response getUserInfo(@HeaderParam("Authorization") String authHeader) {
        try {
            // Validate Authorization header
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return errorResponse(401, "Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7); // Remove "Bearer " prefix

            // Verify token
            Map<String, String> claims = jwtManager.verifyToken(token);
            if (claims.isEmpty()) {
                return errorResponse(401, "Invalid or expired token");
            }

            String username = claims.get("sub");
            
            // Get full user info from database
            Optional<Identity> identityOpt = identityRepository.findByUsername(username);
            if (identityOpt.isEmpty()) {
                return errorResponse(404, "User not found");
            }

            Identity identity = identityOpt.get();

            JsonObject response = Json.createObjectBuilder()
                    .add("sub", identity.getUsername())
                    .add("username", identity.getUsername())
                    .add("email", identity.getEmail() != null ? identity.getEmail() : "")
                    .add("name", identity.getUsername())
                    .add("roles", Role.toString(identity.getRoles()))
                    .add("scopes", identity.getScopes() != null ? identity.getScopes() : "")
                    .add("email_verified", identity.isAccountActivated())
                    .build();

            return Response.ok(response).build();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "UserInfo error: " + e.getMessage(), e);
            return errorResponse(500, "Failed to get user info");
        }
    }

    /**
     * Refresh access token
     * POST /oauth/token
     * Request: { "grant_type": "refresh_token", "refresh_token": "..." }
     * Response: { "access_token": "...", "refresh_token": "...", ... }
     */
    @POST
    @Path("/oauth/token")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response refreshToken(TokenRequest request) {
        try {
            if (!"refresh_token".equals(request.grant_type)) {
                return errorResponse(400, "Invalid grant_type. Only 'refresh_token' is supported.");
            }

            if (request.refresh_token == null || request.refresh_token.isEmpty()) {
                return errorResponse(400, "Refresh token is required");
            }

            // Verify refresh token and get claims
            Map<String, String> claims = jwtManager.verifyToken(request.refresh_token);
            if (claims.isEmpty()) {
                return errorResponse(401, "Invalid or expired refresh token");
            }

            String username = claims.get("sub");
            Optional<Identity> identityOpt = identityRepository.findByUsername(username);
            if (identityOpt.isEmpty()) {
                return errorResponse(401, "User not found");
            }

            Identity identity = identityOpt.get();

            // Generate new tokens
            String[] roles = getRolesArray(identity.getRoles());
            String scopes = identity.getScopes() != null ? identity.getScopes() : "sensor:read dashboard:view";
            String newAccessToken = jwtManager.generateToken(DEFAULT_TENANT, identity.getUsername(), scopes, roles);
            String newRefreshToken = generateRefreshToken(identity);

            JsonObject response = Json.createObjectBuilder()
                    .add("access_token", newAccessToken)
                    .add("refresh_token", newRefreshToken)
                    .add("token_type", "Bearer")
                    .add("expires_in", 3600)
                    .build();

            return Response.ok(response).build();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Token refresh error: " + e.getMessage(), e);
            return errorResponse(500, "Failed to refresh token");
        }
    }

    // ==================== HELPER METHODS ====================

    private Response errorResponse(int status, String message) {
        JsonObject error = Json.createObjectBuilder()
                .add("error", message)
                .add("status", status)
                .build();
        return Response.status(status).entity(error).build();
    }

    private String[] getRolesArray(Long rolesBitfield) {
        if (rolesBitfield == null || rolesBitfield == 0) {
            return new String[]{"VIEWER"};
        }
        String rolesString = Role.toString(rolesBitfield);
        return rolesString.split(", ");
    }

    private String generateRefreshToken(Identity identity) {
        // Generate a longer-lived refresh token (7 days)
        String[] roles = getRolesArray(identity.getRoles());
        return jwtManager.generateToken(
                DEFAULT_TENANT, 
                identity.getUsername(), 
                "refresh", 
                roles
        );
    }

    // ==================== REQUEST DTOs ====================

    public static class LoginRequest {
        public String username;
        public String password;
    }

    public static class RegisterRequest {
        public String username;
        public String email;
        public String password;
    }

    public static class ActivateRequest {
        public String code;
        public String email;
        public String username;
    }

    public static class ResendRequest {
        public String email;
    }

    public static class TokenRequest {
        public String grant_type;
        public String refresh_token;
        public String client_id;
    }
}