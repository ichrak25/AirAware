package tn.airaware.iam.boundaries;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import tn.airaware.core.entities.Identity;
import tn.airaware.iam.repositories.IdentityRepository;
import tn.airaware.iam.repositories.OAuthClientRepository;
import tn.airaware.api.repositories.TenantRepository;
import tn.airaware.core.security.Argon2Utils;
import tn.airaware.iam.security.AuthorizationCode;
import tn.airaware.iam.entities.OAuthClient;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * OAuth 2.0 Authorization Endpoint for AirAware
 * Implements Authorization Code flow with PKCE
 */
@Path("/")
public class OAuthAuthorizationEndpoint {

    private static final Logger LOGGER = Logger.getLogger(OAuthAuthorizationEndpoint.class.getName());
    public static final String CHALLENGE_RESPONSE_COOKIE_ID = "airaware_auth_id";

    @Inject
    Argon2Utils argon2Utils;

    @Inject
    OAuthClientRepository oauthClientRepository;

    @Inject
    TenantRepository tenantRepository;

    @Inject
    IdentityRepository identityRepository;

    /**
     * OAuth 2.0 Authorization endpoint
     * GET /authorize
     */
    @GET
    @Path("/authorize")
    @Produces(MediaType.TEXT_HTML)
    public Response authorize(@Context UriInfo uriInfo) {
        var params = uriInfo.getQueryParameters();

        // 1. Validate client_id (OAuth client)
        String clientId = params.getFirst("client_id");
        if (clientId == null || clientId.isEmpty()) {
            return informUserAboutError("Invalid client_id: " + clientId);
        }

        // Check if OAuth client exists
        Optional<OAuthClient> oauthClientOpt = oauthClientRepository.findByClientId(clientId);
        if (oauthClientOpt.isEmpty() || !oauthClientOpt.get().isActive()) {
            return informUserAboutError("Invalid or inactive OAuth client: " + clientId);
        }

        var client = oauthClientOpt.get();

        // 2. Check if client supports authorization_code grant type
        String supportedGrants = client.getSupportedGrantTypes();
        if (supportedGrants == null || !supportedGrants.contains("authorization_code")) {
            return informUserAboutError("Client does not support authorization_code grant type");
        }

        // 3. Validate tenant organization exists (optional)
        var tenant = tenantRepository.findByOrganizationName(clientId);
        if (tenant == null) {
            return informUserAboutError("Associated organization not found: " + clientId);
        }

        // 4. Validate redirect_uri
        String redirectUri = params.getFirst("redirect_uri");
        if (redirectUri == null || redirectUri.isEmpty()) {
            return informUserAboutError("redirect_uri is required");
        }

        // Verify redirect URI matches registered URI
        String registeredUri = client.getRedirectUri();
        if (registeredUri != null && !redirectUri.startsWith(registeredUri)) {
            return informUserAboutError("redirect_uri does not match registered URI");
        }

        // 5. Validate response_type
        String responseType = params.getFirst("response_type");
        if (!"code".equals(responseType)) {
            return informUserAboutError("Invalid response_type: " + responseType +
                    ". Only 'code' is supported.");
        }

        // 6. Handle scope
        String requestedScope = params.getFirst("scope");
        if (requestedScope == null || requestedScope.isEmpty()) {
            // Default scopes for AirAware
            requestedScope = "sensor:read sensor:write alert:read alert:write dashboard:view";
        }

        // 7. Validate code_challenge (PKCE)
        String codeChallenge = params.getFirst("code_challenge");
        if (codeChallenge == null || codeChallenge.isEmpty()) {
            return informUserAboutError("code_challenge is required (PKCE)");
        }

        // 8. Validate code_challenge_method must be S256
        String codeChallengeMethod = params.getFirst("code_challenge_method");
        if (codeChallengeMethod == null || !codeChallengeMethod.equals("S256")) {
            return informUserAboutError("code_challenge_method must be 'S256'");
        }

        // Show login page
        StreamingOutput stream = output -> {
            try (InputStream is = Objects.requireNonNull(
                    getClass().getResource("/Login.html")).openStream()) {
                output.write(is.readAllBytes());
            }
        };

        // Store authorization context in cookie
        String cookieValue = client.getId() + "#" + requestedScope + "$" + redirectUri;

        return Response.ok(stream)
                .location(uriInfo.getBaseUri().resolve("/login/authorization"))
                .cookie(new NewCookie.Builder(CHALLENGE_RESPONSE_COOKIE_ID)
                        .httpOnly(true)
                        .secure(true)
                        .sameSite(NewCookie.SameSite.STRICT)
                        .value(cookieValue)
                        .build())
                .build();
    }

    /**
     * Handle login form submission
     * POST /login/authorization
     */
    @POST
    @Path("/login/authorization")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response login(@CookieParam(CHALLENGE_RESPONSE_COOKIE_ID) Cookie cookie,
                          @FormParam("username") String username,
                          @FormParam("password") String password,
                          @Context UriInfo uriInfo) throws Exception {
        try {
            if (cookie == null) {
                LOGGER.severe("Authorization cookie is missing");
                throw new Exception("Authorization cookie is missing.");
            }

            // Find identity - properly handle Optional
            Optional<Identity> identityOpt = identityRepository.findByUsername(username);
            if (identityOpt.isEmpty()) {
                LOGGER.warning("Identity not found for username: " + username);
                throw new Exception("Invalid credentials");
            }

            // Extract Identity from Optional
            Identity identity = identityOpt.get();

            // Check if account is activated
            if (!identity.isAccountActivated()) {
                String redirectUri = cookie.getValue().split("\\$")[1];
                var location = UriBuilder.fromUri(redirectUri)
                        .queryParam("error", "account_not_activated")
                        .queryParam("error_description", "Please activate your account before logging in.")
                        .build();

                return Response.status(Response.Status.FORBIDDEN)
                        .entity("Account not activated. Please verify your account.")
                        .location(location)
                        .build();
            }

            // Verify password
            if (argon2Utils.check(identity.getPassword(), password.toCharArray())) {
                MultivaluedMap<String, String> params = uriInfo.getQueryParameters();

                String redirectURI = buildActualRedirectURI(
                        cookie.getValue().split("\\$")[1],  // redirectUri
                        params.getFirst("response_type"),
                        cookie.getValue().split("#")[0],    // clientId
                        username,
                        cookie.getValue().split("#")[1].split("\\$")[0],  // scopes
                        params.getFirst("code_challenge"),
                        params.getFirst("state")
                );

                return Response.seeOther(UriBuilder.fromUri(redirectURI).build()).build();
            } else {
                // Invalid credentials
                String redirectUri = cookie.getValue().split("\\$")[1];
                var location = UriBuilder.fromUri(redirectUri)
                        .queryParam("error", "access_denied")
                        .queryParam("error_description", "Invalid credentials")
                        .build();

                return Response.seeOther(location).build();
            }
        } catch (Exception e) {
            LOGGER.severe("Error during login: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("An error occurred during login. Please try again.")
                    .build();
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Build the actual redirect URI with authorization code
     */
    private String buildActualRedirectURI(String redirectUri, String responseType,
                                          String clientId, String userId,
                                          String approvedScopes, String codeChallenge,
                                          String state) throws Exception {
        StringBuilder sb = new StringBuilder(redirectUri);

        if ("code".equals(responseType)) {
            // Create authorization code with 2 minute expiration
            AuthorizationCode authorizationCode = new AuthorizationCode(
                    clientId,
                    userId,
                    approvedScopes,
                    Instant.now().plus(2, ChronoUnit.MINUTES).getEpochSecond(),
                    redirectUri
            );

            sb.append("?code=").append(URLEncoder.encode(
                    authorizationCode.getCode(codeChallenge),
                    StandardCharsets.UTF_8));
        } else {
            // Implicit flow not supported
            return null;
        }

        // Add state parameter if provided
        if (state != null) {
            sb.append("&state=").append(state);
        }

        return sb.toString();
    }

    /**
     * Show error page to user
     */
    private Response informUserAboutError(String error) {
        String errorMessage = String.format(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "<meta charset=\"UTF-8\"/>" +
                        "<title>AirAware - Authorization Error</title>" +
                        "<style>" +
                        "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; " +
                        "       margin: 0; padding: 50px; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); }" +
                        ".container { max-width: 600px; margin: auto; background: white; " +
                        "            padding: 40px; border-radius: 12px; box-shadow: 0 10px 40px rgba(0,0,0,0.2); }" +
                        "h1 { color: #d32f2f; font-size: 28px; margin-bottom: 20px; }" +
                        "p { color: #666; line-height: 1.8; font-size: 16px; }" +
                        ".icon { font-size: 64px; text-align: center; margin-bottom: 20px; }" +
                        "</style>" +
                        "</head>" +
                        "<body>" +
                        "<div class=\"container\">" +
                        "<div class=\"icon\">⚠️</div>" +
                        "<h1>Authorization Error</h1>" +
                        "<p>%s</p>" +
                        "<p><small>If you believe this is an error, please contact your system administrator.</small></p>" +
                        "</div>" +
                        "</body>" +
                        "</html>", error);

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorMessage)
                .type(MediaType.TEXT_HTML)
                .build();
    }
}