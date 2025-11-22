package tn.airaware.iam.boundaries;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import tn.airaware.api.repositories.TenantRepository;
import tn.airaware.iam.services.IdentityServices;

import java.io.InputStream;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Identity Registration Endpoint for AirAware
 * Handles user registration and account activation
 */
@Path("/")
public class IdentityRegistrationEndpoint {

    private static final Logger LOGGER = Logger.getLogger(IdentityRegistrationEndpoint.class.getName());

    @Inject
    IdentityServices identityServices;

    @Inject
    TenantRepository tenantRepository;

    /**
     * Show registration authorization page
     */
    @GET
    @Path("/register/authorize")
    @Produces(MediaType.TEXT_HTML)
    public Response authorizeRegistration(@Context UriInfo uriInfo) {
        try {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();

            // Validate client_id (tenant)
            String clientId = params.getFirst("client_id");
            if (isNullOrEmpty(clientId)) {
                return informUserAboutError("Invalid client_id: " + clientId);
            }

            var tenant = tenantRepository.findByOrganizationName(clientId);
            if (tenant == null) {
                return informUserAboutError("Invalid tenant/organization: " + clientId);
            }

            // Validate redirectUri (if tenant has one configured)
            String redirectUri = params.getFirst("redirect_uri");
            // Note: Tenant entity uses different field names than SmartHome
            // Adapt validation if needed based on your Tenant entity

            // Stream the registration page
            StreamingOutput stream = createHtmlResponse("/Register.html");
            return Response.ok(stream)
                    .location(uriInfo.getBaseUri().resolve("/register"))
                    .build();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during registration authorization: ", e);
            return informUserAboutError("An unexpected error occurred.");
        }
    }

    /**
     * Process registration form submission
     */
    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response register(@FormParam("username") String username,
                             @FormParam("email") String email,
                             @FormParam("password") String password) {
        try {
            identityServices.registerIdentity(username, password, email);

            // Stream the activation instructions page
            StreamingOutput stream = createHtmlResponse("/Activate.html");
            return Response.ok(stream).build();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during registration: ", e);
            return informUserAboutError(e.getMessage());
        }
    }

    /**
     * Activate account with activation code
     */
    @POST
    @Path("/register/activate")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response activate(@FormParam("code") String code) {
        try {
            identityServices.activateIdentity(code);
            return Response.ok("Account activated successfully!").build();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during activation: ", e);
            return informUserAboutError(e.getMessage());
        }
    }

    // ==================== Helper Methods ====================

    private StreamingOutput createHtmlResponse(String filePath) {
        return output -> {
            try (InputStream is = Objects.requireNonNull(getClass().getResource(filePath)).openStream()) {
                output.write(is.readAllBytes());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error streaming HTML file: " + filePath, e);
                throw new WebApplicationException("Failed to load HTML content.", e);
            }
        };
    }

    private boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    private Response informUserAboutError(String error) {
        String errorMessage = String.format(
                "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"UTF-8\"/>" +
                "<title>AirAware - Registration Error</title>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; margin: 50px; background: #f0f8ff; }" +
                ".container { max-width: 600px; margin: auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }" +
                "h1 { color: #d32f2f; }" +
                "p { color: #666; line-height: 1.6; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"container\">" +
                "<h1>⚠️ Registration Error</h1>" +
                "<p>%s</p>" +
                "<p><a href=\"/register\">Try Again</a></p>" +
                "</div>" +
                "</body>" +
                "</html>", error);

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorMessage)
                .type(MediaType.TEXT_HTML)
                .build();
    }
}