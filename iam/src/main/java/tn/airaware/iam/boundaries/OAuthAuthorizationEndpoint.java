package tn.airaware.iam.boundaries;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import tn.airaware.core.entities.Identity;
import tn.airaware.iam.repositories.IdentityRepository;
import tn.airaware.iam.repositories.OAuthClientRepository;
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

    // REMOVED: TenantRepository - it was causing JNoSQL DocumentTemplate conflicts
    // The OAuth client already contains organization info

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

        // 3. Organization validation - use OAuth client's organization name
        // REMOVED: TenantRepository lookup - OAuth client already has organizationName
        String organizationName = client.getOrganizationName();
        if (organizationName == null || organizationName.isEmpty()) {
            LOGGER.warning("OAuth client has no organization name: " + clientId);
            // Continue anyway - organization is optional for basic auth flow
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
                        .secure(false)  // Set to true in production with HTTPS
                        .sameSite(NewCookie.SameSite.LAX)  // Changed from STRICT for better compatibility
                        .path("/")  // Make cookie available across paths
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
                return informUserAboutError("Authorization session expired. Please start the login process again.");
            }

            LOGGER.info("Login attempt for user: " + username);

            // Find identity
            Optional<Identity> identityOpt = identityRepository.findByUsername(username);
            if (identityOpt.isEmpty()) {
                LOGGER.warning("Identity not found for username: " + username);
                return informUserAboutError("Invalid username or password");
            }

            Identity identity = identityOpt.get();

            // Check if account is activated
            if (!identity.isAccountActivated()) {
                LOGGER.warning("Account not activated for user: " + username);
                return informUserAboutError("Account not activated. Please check your email for the activation code.");
            }

            // Verify password
            if (argon2Utils.check(identity.getPassword(), password.toCharArray())) {
                LOGGER.info("Password verified for user: " + username);

                MultivaluedMap<String, String> params = uriInfo.getQueryParameters();

                // Parse cookie value: clientId#scopes$redirectUri
                String[] cookieParts = cookie.getValue().split("\\$");
                String redirectUri = cookieParts.length > 1 ? cookieParts[1] : "http://localhost:3000/callback";

                String[] firstParts = cookieParts[0].split("#");
                String clientId = firstParts[0];
                String scopes = firstParts.length > 1 ? firstParts[1] : "sensor:read";

                String redirectURI = buildActualRedirectURI(
                        redirectUri,
                        params.getFirst("response_type") != null ? params.getFirst("response_type") : "code",
                        clientId,
                        username,
                        scopes,
                        params.getFirst("code_challenge"),
                        params.getFirst("state")
                );

                LOGGER.info("Redirecting to: " + redirectURI);
                return Response.seeOther(UriBuilder.fromUri(redirectURI).build()).build();
            } else {
                LOGGER.warning("Invalid password for user: " + username);
                return informUserAboutError("Invalid username or password");
            }
        } catch (Exception e) {
            LOGGER.severe("Error during login: " + e.getMessage());
            e.printStackTrace();
            return informUserAboutError("An error occurred during login. Please try again.");
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

            // Use a default code challenge if none provided (for testing)
            String challenge = codeChallenge != null ? codeChallenge : "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";

            sb.append("?code=").append(URLEncoder.encode(
                    authorizationCode.getCode(challenge),
                    StandardCharsets.UTF_8));
        } else {
            return redirectUri + "?error=unsupported_response_type";
        }

        // Add state parameter if provided
        if (state != null && !state.isEmpty()) {
            sb.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));
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
                        "a { color: #667eea; text-decoration: none; }" +
                        "a:hover { text-decoration: underline; }" +
                        "</style>" +
                        "</head>" +
                        "<body>" +
                        "<div class=\"container\">" +
                        "<div class=\"icon\">⚠️</div>" +
                        "<h1>Authorization Error</h1>" +
                        "<p>%s</p>" +
                        "<p><a href=\"authorize?client_id=airaware-web-client&redirect_uri=http://localhost:3000/callback&response_type=code&code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM&code_challenge_method=S256\">Try Again</a></p>" +
                        "</div>" +
                        "</body>" +
                        "</html>", error);

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorMessage)
                .type(MediaType.TEXT_HTML)
                .build();
    }
}