package tn.airaware.iam.boundaries;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import tn.airaware.iam.entities.Identity;
import tn.airaware.iam.services.IdentityServices;
import tn.airaware.iam.security.JwtManager;

/**
 * Identity Management Endpoint for AirAware
 * Handles user profile management and updates
 */
@Path("/identities")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IdentityManagementEndpoint {

    @Inject
    IdentityServices identityService;

    @EJB
    private JwtManager jwtManager;

    /**
     * Get user profile from JWT token
     */
    @GET
    @Path("/profile")
    public Response getUserProfile(@HeaderParam("Authorization") String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(errorResponse("Authorization header missing or invalid"))
                    .build();
        }

        String token = authorizationHeader.substring("Bearer ".length());

        try {
            var claims = jwtManager.verifyToken(token);
            String username = claims.get("sub");
            String tenantId = claims.get("tenant_id");

            JsonObject profile = Json.createObjectBuilder()
                    .add("username", username)
                    .add("tenant_id", tenantId)
                    .add("roles", claims.get("groups"))
                    .add("scopes", claims.get("scope"))
                    .build();

            return Response.ok(profile).build();
        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(errorResponse("Invalid or expired token"))
                    .build();
        }
    }

    /**
     * Update identity information
     */
    @PUT
    @Path("/{id}")
    public Response updateIdentity(@PathParam("id") String id,
                                   Identity updatedIdentity,
                                   @QueryParam("currentPassword") String currentPassword,
                                   @QueryParam("newPassword") String newPassword) {
        try {
            // Validate current password is provided
            if (currentPassword == null || currentPassword.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(errorResponse("Current password is required"))
                        .build();
            }

            // Update the identity
            Identity updated = identityService.updateIdentity(
                    id,
                    updatedIdentity.getUsername(),
                    updatedIdentity.getEmail(),
                    newPassword,
                    currentPassword
            );

            return Response.ok(updated).build();
        } catch (EJBException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorResponse(e.getMessage()))
                    .build();
        }
    }

    /**
     * Delete identity (user account)
     */
    @DELETE
    @Path("/{id}")
    public Response deleteIdentity(@PathParam("id") String id) {
        try {
            identityService.deleteIdentityById(id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (EJBException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorResponse("Identity not found with ID: " + id))
                    .build();
        }
    }

    /**
     * Get identity by ID
     */
    @GET
    @Path("/{id}")
    public Response getIdentity(@PathParam("id") String id) {
        try {
            Identity identity = identityService.getIdentityById(id);
            return Response.ok(identity).build();
        } catch (EJBException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorResponse(e.getMessage()))
                    .build();
        }
    }

    // ==================== Helper Methods ====================

    private JsonObject errorResponse(String message) {
        return Json.createObjectBuilder()
                .add("error", message)
                .add("timestamp", System.currentTimeMillis())
                .build();
    }
}