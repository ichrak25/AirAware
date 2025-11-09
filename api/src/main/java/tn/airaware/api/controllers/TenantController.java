package tn.airaware.api.controllers;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tn.airaware.api.entities.Sensor;
import tn.airaware.api.entities.Tenant;
import tn.airaware.api.services.SensorService;
import tn.airaware.api.services.TenantService;

import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * REST Controller for Tenant management
 * Handles organization/owner registration, profile management, and tenant lifecycle
 */
@Path("/tenants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TenantController {

    private static final Logger LOGGER = Logger.getLogger(TenantController.class.getName());
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9\\s()-]{8,20}$");

    @Inject
    private TenantService tenantService;

    @Inject
    private SensorService sensorService;

    /**
     * Create a new tenant
     * POST /api/tenants
     */
    @POST
    public Response createTenant(Tenant tenant) {
        try {
            if (tenant == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Tenant data is required"))
                        .build();
            }

            // Validate required fields
            if (tenant.getOrganizationName() == null || tenant.getOrganizationName().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Organization name is required"))
                        .build();
            }

            if (tenant.getContactEmail() == null || tenant.getContactEmail().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Contact email is required"))
                        .build();
            }

            // Validate email format
            if (!isValidEmail(tenant.getContactEmail())) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Invalid email format"))
                        .build();
            }

            // Validate phone if provided
            if (tenant.getContactPhone() != null && !tenant.getContactPhone().trim().isEmpty()) {
                if (!isValidPhone(tenant.getContactPhone())) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(new ErrorResponse("Invalid phone number format"))
                            .build();
                }
            }

            // Check if organization name already exists
            Tenant existingTenant = tenantService.findByOrganizationName(tenant.getOrganizationName());
            if (existingTenant != null) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(new ErrorResponse("Organization name already exists: " + tenant.getOrganizationName()))
                        .build();
            }

            tenantService.saveTenant(tenant);
            LOGGER.info("Tenant created successfully: " + tenant.getOrganizationName());

            return Response.status(Response.Status.CREATED)
                    .entity(tenant)
                    .build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOGGER.severe("Error creating tenant: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to create tenant: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get all tenants
     * GET /api/tenants
     */
    @GET
    public Response getAllTenants() {
        try {
            List<Tenant> tenants = tenantService.getAllTenants();
            LOGGER.info("Retrieved " + tenants.size() + " tenants");

            return Response.ok(tenants).build();

        } catch (Exception e) {
            LOGGER.severe("Error retrieving tenants: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve tenants: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get tenant by ID
     * GET /api/tenants/{id}
     */
    @GET
    @Path("/{id}")
    public Response getTenantById(@PathParam("id") String id) {
        try {
            if (id == null || id.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Tenant ID is required"))
                        .build();
            }

            Tenant tenant = tenantService.findById(id);

            if (tenant == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Tenant not found with ID: " + id))
                        .build();
            }

            return Response.ok(tenant).build();

        } catch (Exception e) {
            LOGGER.severe("Error retrieving tenant: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve tenant: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get tenant by organization name
     * GET /api/tenants/organization/{organizationName}
     */
    @GET
    @Path("/organization/{organizationName}")
    public Response getTenantByOrganization(@PathParam("organizationName") String organizationName) {
        try {
            if (organizationName == null || organizationName.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Organization name is required"))
                        .build();
            }

            Tenant tenant = tenantService.findByOrganizationName(organizationName);

            if (tenant == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Tenant not found with organization name: " + organizationName))
                        .build();
            }

            return Response.ok(tenant).build();

        } catch (Exception e) {
            LOGGER.severe("Error retrieving tenant by organization: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve tenant: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get active tenants only
     * GET /api/tenants/active
     */
    @GET
    @Path("/active")
    public Response getActiveTenants() {
        try {
            List<Tenant> allTenants = tenantService.getAllTenants();
            List<Tenant> activeTenants = allTenants.stream()
                    .filter(Tenant::isActive)
                    .collect(Collectors.toList());

            LOGGER.info("Retrieved " + activeTenants.size() + " active tenants");

            return Response.ok(activeTenants).build();

        } catch (Exception e) {
            LOGGER.severe("Error retrieving active tenants: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve active tenants: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get tenants by country
     * GET /api/tenants/country/{country}
     */
    @GET
    @Path("/country/{country}")
    public Response getTenantsByCountry(@PathParam("country") String country) {
        try {
            if (country == null || country.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Country is required"))
                        .build();
            }

            List<Tenant> allTenants = tenantService.getAllTenants();
            List<Tenant> countryTenants = allTenants.stream()
                    .filter(t -> country.equalsIgnoreCase(t.getCountry()))
                    .collect(Collectors.toList());

            LOGGER.info("Retrieved " + countryTenants.size() + " tenants from country: " + country);

            return Response.ok(countryTenants).build();

        } catch (Exception e) {
            LOGGER.severe("Error retrieving tenants by country: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve tenants: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get tenant profile with sensors count
     * GET /api/tenants/{id}/profile
     */
    @GET
    @Path("/{id}/profile")
    public Response getTenantProfile(@PathParam("id") String id) {
        try {
            if (id == null || id.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Tenant ID is required"))
                        .build();
            }

            Tenant tenant = tenantService.findById(id);
            if (tenant == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Tenant not found with ID: " + id))
                        .build();
            }

            // Get all sensors for this tenant
            List<Sensor> allSensors = sensorService.getAllSensors();
            List<Sensor> tenantSensors = allSensors.stream()
                    .filter(s -> s.getTenant() != null &&
                            tenant.getOrganizationName().equals(s.getTenant().getOrganizationName()))
                    .collect(Collectors.toList());

            // Count active sensors
            long activeSensorsCount = tenantSensors.stream()
                    .filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus()))
                    .count();

            TenantProfile profile = new TenantProfile();
            profile.tenant = tenant;
            profile.totalSensors = tenantSensors.size();
            profile.activeSensors = (int) activeSensorsCount;
            profile.inactiveSensors = tenantSensors.size() - (int) activeSensorsCount;

            LOGGER.info("Generated profile for tenant: " + tenant.getOrganizationName());

            return Response.ok(profile).build();

        } catch (Exception e) {
            LOGGER.severe("Error generating tenant profile: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to generate profile: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Update tenant information
     * PUT /api/tenants/{id}
     */
    @PUT
    @Path("/{id}")
    public Response updateTenant(@PathParam("id") String id, Tenant tenant) {
        try {
            if (id == null || id.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Tenant ID is required"))
                        .build();
            }

            if (tenant == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Tenant data is required"))
                        .build();
            }

            // Check if tenant exists
            Tenant existingTenant = tenantService.findById(id);
            if (existingTenant == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Tenant not found with ID: " + id))
                        .build();
            }

            // Validate email if provided
            if (tenant.getContactEmail() != null && !tenant.getContactEmail().trim().isEmpty()) {
                if (!isValidEmail(tenant.getContactEmail())) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(new ErrorResponse("Invalid email format"))
                            .build();
                }
            }

            // Validate phone if provided
            if (tenant.getContactPhone() != null && !tenant.getContactPhone().trim().isEmpty()) {
                if (!isValidPhone(tenant.getContactPhone())) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(new ErrorResponse("Invalid phone number format"))
                            .build();
                }
            }

            // Check if organization name change conflicts with existing tenant
            if (tenant.getOrganizationName() != null &&
                    !tenant.getOrganizationName().equals(existingTenant.getOrganizationName())) {
                Tenant conflictTenant = tenantService.findByOrganizationName(tenant.getOrganizationName());
                if (conflictTenant != null && !conflictTenant.getId().equals(id)) {
                    return Response.status(Response.Status.CONFLICT)
                            .entity(new ErrorResponse("Organization name already exists: " + tenant.getOrganizationName()))
                            .build();
                }
            }

            tenant.setId(id);
            tenantService.saveTenant(tenant);
            LOGGER.info("Tenant updated successfully: " + id);

            return Response.ok(tenant).build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOGGER.severe("Error updating tenant: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to update tenant: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Deactivate a tenant
     * PUT /api/tenants/{id}/deactivate
     */
    @PUT
    @Path("/{id}/deactivate")
    public Response deactivateTenant(@PathParam("id") String id) {
        try {
            if (id == null || id.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Tenant ID is required"))
                        .build();
            }

            Tenant tenant = tenantService.findById(id);
            if (tenant == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Tenant not found with ID: " + id))
                        .build();
            }

            tenantService.deactivateTenant(id);
            LOGGER.info("Tenant deactivated: " + id);

            // Fetch updated tenant
            Tenant updatedTenant = tenantService.findById(id);

            return Response.ok(updatedTenant)
                    .entity(new SuccessResponse("Tenant deactivated successfully"))
                    .build();

        } catch (Exception e) {
            LOGGER.severe("Error deactivating tenant: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to deactivate tenant: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Reactivate a tenant
     * PUT /api/tenants/{id}/activate
     */
    @PUT
    @Path("/{id}/activate")
    public Response activateTenant(@PathParam("id") String id) {
        try {
            if (id == null || id.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Tenant ID is required"))
                        .build();
            }

            Tenant tenant = tenantService.findById(id);
            if (tenant == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Tenant not found with ID: " + id))
                        .build();
            }

            tenant.setActive(true);
            tenantService.saveTenant(tenant);
            LOGGER.info("Tenant activated: " + id);

            return Response.ok(tenant)
                    .entity(new SuccessResponse("Tenant activated successfully"))
                    .build();

        } catch (Exception e) {
            LOGGER.severe("Error activating tenant: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to activate tenant: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Delete a tenant
     * DELETE /api/tenants/{id}
     */
    @DELETE
    @Path("/{id}")
    public Response deleteTenant(@PathParam("id") String id) {
        try {
            if (id == null || id.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Tenant ID is required"))
                        .build();
            }

            Tenant tenant = tenantService.findById(id);
            if (tenant == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Tenant not found with ID: " + id))
                        .build();
            }

            // Check if tenant has associated sensors
            List<Sensor> allSensors = sensorService.getAllSensors();
            boolean hasSensors = allSensors.stream()
                    .anyMatch(s -> s.getTenant() != null &&
                            tenant.getOrganizationName().equals(s.getTenant().getOrganizationName()));

            if (hasSensors) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(new ErrorResponse("Cannot delete tenant with associated sensors. Please remove or reassign sensors first."))
                        .build();
            }

            tenantService.deleteTenant(id);
            LOGGER.info("Tenant deleted successfully: " + id);

            return Response.ok()
                    .entity(new SuccessResponse("Tenant deleted successfully"))
                    .build();

        } catch (Exception e) {
            LOGGER.severe("Error deleting tenant: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to delete tenant: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get tenant statistics
     * GET /api/tenants/statistics
     */
    @GET
    @Path("/statistics")
    public Response getTenantStatistics() {
        try {
            List<Tenant> allTenants = tenantService.getAllTenants();

            TenantStatistics stats = new TenantStatistics();
            stats.totalTenants = allTenants.size();
            stats.activeTenants = (int) allTenants.stream().filter(Tenant::isActive).count();
            stats.inactiveTenants = allTenants.size() - stats.activeTenants;

            // Count tenants by country
            stats.tenantsByCountry = allTenants.stream()
                    .filter(t -> t.getCountry() != null)
                    .collect(Collectors.groupingBy(Tenant::getCountry, Collectors.counting()));

            LOGGER.info("Generated tenant statistics");

            return Response.ok(stats).build();

        } catch (Exception e) {
            LOGGER.severe("Error generating statistics: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to generate statistics: " + e.getMessage()))
                    .build();
        }
    }

    // --- Helper Methods ---

    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    private boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    // --- Response DTOs ---

    public static class ErrorResponse {
        public String error;
        public long timestamp;

        public ErrorResponse(String error) {
            this.error = error;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public static class SuccessResponse {
        public String message;
        public long timestamp;

        public SuccessResponse(String message) {
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public static class TenantProfile {
        public Tenant tenant;
        public int totalSensors;
        public int activeSensors;
        public int inactiveSensors;
    }

    public static class TenantStatistics {
        public int totalTenants;
        public int activeTenants;
        public int inactiveTenants;
        public java.util.Map<String, Long> tenantsByCountry;
    }
}