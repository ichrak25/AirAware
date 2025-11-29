package tn.airaware.iam.boundaries;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.io.InputStream;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * OAuth Callback Endpoint for AirAware
 * Serves the callback page that handles authorization codes
 */
@Path("/callback")
public class CallbackEndpoint {

    private static final Logger LOGGER = Logger.getLogger(CallbackEndpoint.class.getName());

    /**
     * Serve the callback HTML page
     * This page will automatically exchange the authorization code for tokens
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response callback(@Context UriInfo uriInfo) {
        try {
            StreamingOutput stream = output -> {
                try (InputStream is = Objects.requireNonNull(
                        getClass().getResource("/Callback.html")).openStream()) {
                    output.write(is.readAllBytes());
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error streaming Callback.html", e);
                    throw new WebApplicationException("Failed to load callback page.", e);
                }
            };

            return Response.ok(stream).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error serving callback page", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to load callback page: " + e.getMessage())
                    .build();
        }
    }
}