package tn.airaware.api.controllers;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tn.airaware.api.entities.Alert;
import tn.airaware.api.entities.Reading;
import tn.airaware.api.services.AlertService;

import java.util.List;
import java.util.logging.Logger;

/**
 * REST Controller for Alert management
 * Provides endpoints for creating, retrieving, updating, and deleting alerts
 */
@Path("/alerts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AlertController {

    private static final Logger LOGGER = Logger.getLogger(AlertController.class.getName());

    @Inject
    private AlertService alertService;

    /**
     * Create a new alert
     * POST /api/alerts
     */
    @POST
    public Response createAlert(Alert alert) {
        try {
            if (alert == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Alert data is required"))
                        .build();
            }

            // Validate required fields
            if (alert.getType() == null || alert.getType().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Alert type is required"))
                        .build();
            }

            if (alert.getSeverity() == null || alert.getSeverity().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Alert severity is required"))
                        .build();
            }

            if (alert.getSensorId() == null || alert.getSensorId().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Sensor ID is required"))
                        .build();
            }

            alertService.saveAlert(alert);
            LOGGER.info("Alert created successfully: " + alert.getId());

            return Response.status(Response.Status.CREATED)
                    .entity(alert)
                    .build();

        } catch (Exception e) {
            LOGGER.severe("Error creating alert: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to create alert: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get all alerts
     * GET /api/alerts
     */
    @GET
    public Response getAllAlerts() {
        try {
            List<Alert> alerts = alertService.getAllAlerts();
            LOGGER.info("Retrieved " + alerts.size() + " alerts");

            return Response.ok(alerts).build();

        } catch (Exception e) {
            LOGGER.severe("Error retrieving alerts: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve alerts: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get alert by ID
     * GET /api/alerts/{id}
     */
    @GET
    @Path("/{id}")
    public Response getAlertById(@PathParam("id") String id) {
        try {
            if (id == null || id.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Alert ID is required"))
                        .build();
            }

            Alert alert = alertService.findById(id);

            if (alert == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Alert not found with ID: " + id))
                        .build();
            }

            return Response.ok(alert).build();

        } catch (Exception e) {
            LOGGER.severe("Error retrieving alert: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve alert: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get alerts by severity
     * GET /api/alerts/severity/{severity}
     */
    @GET
    @Path("/severity/{severity}")
    public Response getAlertsBySeverity(@PathParam("severity") String severity) {
        try {
            if (severity == null || severity.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Severity level is required"))
                        .build();
            }

            List<Alert> alerts = alertService.getAlertsBySeverity(severity);
            LOGGER.info("Retrieved " + alerts.size() + " alerts with severity: " + severity);

            return Response.ok(alerts).build();

        } catch (Exception e) {
            LOGGER.severe("Error retrieving alerts by severity: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve alerts: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get alerts by sensor ID
     * GET /api/alerts/sensor/{sensorId}
     */
    @GET
    @Path("/sensor/{sensorId}")
    public Response getAlertsBySensor(@PathParam("sensorId") String sensorId) {
        try {
            if (sensorId == null || sensorId.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Sensor ID is required"))
                        .build();
            }

            List<Alert> alerts = alertService.getAlertsBySensor(sensorId);
            LOGGER.info("Retrieved " + alerts.size() + " alerts for sensor: " + sensorId);

            return Response.ok(alerts).build();

        } catch (Exception e) {
            LOGGER.severe("Error retrieving alerts by sensor: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve alerts: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Mark an alert as resolved with optional resolution notes
     * PUT /api/alerts/{id}/resolve
     * 
     * Request body (optional):
     * {
     *   "notes": "Fixed ventilation system"
     * }
     */
    @PUT
    @Path("/{id}/resolve")
    public Response resolveAlert(@PathParam("id") String id, ResolveRequest request) {
        try {
            if (id == null || id.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Alert ID is required"))
                        .build();
            }

            // Check if alert exists
            Alert alert = alertService.findById(id);
            if (alert == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Alert not found with ID: " + id))
                        .build();
            }

            // Get resolution notes from request (if provided)
            String resolutionNotes = (request != null) ? request.notes : null;
            
            alertService.resolveAlert(id, resolutionNotes);
            LOGGER.info("Alert resolved successfully: " + id + 
                    (resolutionNotes != null ? " with notes: " + resolutionNotes : ""));

            // Fetch updated alert
            Alert updatedAlert = alertService.findById(id);

            return Response.ok(updatedAlert).build();

        } catch (Exception e) {
            LOGGER.severe("Error resolving alert: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to resolve alert: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Request DTO for resolving alerts with notes
     */
    public static class ResolveRequest {
        public String notes;
    }

    /**
     * Update an existing alert
     * PUT /api/alerts/{id}
     */
    @PUT
    @Path("/{id}")
    public Response updateAlert(@PathParam("id") String id, Alert alert) {
        try {
            if (id == null || id.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Alert ID is required"))
                        .build();
            }

            if (alert == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Alert data is required"))
                        .build();
            }

            // Check if alert exists
            Alert existingAlert = alertService.findById(id);
            if (existingAlert == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Alert not found with ID: " + id))
                        .build();
            }

            // Set the ID to ensure we're updating the correct alert
            alert.setId(id);
            alertService.saveAlert(alert);
            LOGGER.info("Alert updated successfully: " + id);

            return Response.ok(alert).build();

        } catch (Exception e) {
            LOGGER.severe("Error updating alert: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to update alert: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Delete an alert
     * DELETE /api/alerts/{id}
     */
    @DELETE
    @Path("/{id}")
    public Response deleteAlert(@PathParam("id") String id) {
        try {
            if (id == null || id.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Alert ID is required"))
                        .build();
            }

            // Check if alert exists
            Alert alert = alertService.findById(id);
            if (alert == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Alert not found with ID: " + id))
                        .build();
            }

            alertService.deleteAlert(id);
            LOGGER.info("Alert deleted successfully: " + id);

            return Response.ok()
                    .entity(new SuccessResponse("Alert deleted successfully"))
                    .build();

        } catch (Exception e) {
            LOGGER.severe("Error deleting alert: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to delete alert: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Test endpoint - Create a test alert to verify notifications
     * GET /api/alerts/test
     */
    @GET
    @Path("/test")
    public Response createTestAlert() {
        try {
            LOGGER.info("🧪 Creating TEST alert to verify notification system...");
            
            // Create a test reading
            Reading testReading = new Reading();
            testReading.setSensorId("TEST_SENSOR");
            testReading.setTemperature(24.0);
            testReading.setHumidity(58.0);
            testReading.setCo2(2500.0); // High CO2 to trigger alert
            testReading.setPm25(52.0);
            testReading.setVoc(0.35);
            testReading.setTimestamp(java.time.Instant.now());
            
            // Create test alert
            Alert testAlert = new Alert();
            testAlert.setType("TEST_ALERT");
            testAlert.setSeverity("WARNING");
            testAlert.setMessage("🧪 TEST: This is a test alert to verify the notification system is working!");
            testAlert.setSensorId("TEST_SENSOR");
            testAlert.setReading(testReading);
            
            // Save alert (this will trigger notifications)
            alertService.saveAlert(testAlert);
            
            LOGGER.info("✅ Test alert created with ID: " + testAlert.getId());
            
            return Response.ok()
                    .entity(new SuccessResponse("Test alert created! Check your email. Alert ID: " + testAlert.getId()))
                    .build();
                    
        } catch (Exception e) {
            LOGGER.severe("❌ Error creating test alert: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to create test alert: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get unresolved alerts only
     * GET /api/alerts/unresolved
     */
    @GET
    @Path("/unresolved")
    public Response getUnresolvedAlerts() {
        try {
            List<Alert> allAlerts = alertService.getAllAlerts();
            List<Alert> unresolvedAlerts = allAlerts.stream()
                    .filter(alert -> !alert.isResolved())
                    .toList();

            LOGGER.info("Retrieved " + unresolvedAlerts.size() + " unresolved alerts");

            return Response.ok(unresolvedAlerts).build();

        } catch (Exception e) {
            LOGGER.severe("Error retrieving unresolved alerts: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve unresolved alerts: " + e.getMessage()))
                    .build();
        }
    }

    // --- Response DTOs ---

    /**
     * Error response wrapper
     */
    public static class ErrorResponse {
        public String error;
        public long timestamp;

        public ErrorResponse(String error) {
            this.error = error;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * Success response wrapper
     */
    public static class SuccessResponse {
        public String message;
        public long timestamp;

        public SuccessResponse(String message) {
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
    }
}