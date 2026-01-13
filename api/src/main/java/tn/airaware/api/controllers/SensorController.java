package tn.airaware.api.controllers;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tn.airaware.api.entities.Sensor;
import tn.airaware.api.services.ReadingService;
import tn.airaware.api.services.SensorService;
import tn.airaware.api.entities.Reading;
import tn.airaware.api.mqtt.MqttPublisherService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * REST Controller for Sensor management
 * Handles sensor registration, status monitoring, and device lifecycle
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorController {

    private static final Logger LOGGER = Logger.getLogger(SensorController.class.getName());

    @Inject
    private SensorService sensorService;

    @Inject
    private ReadingService readingService;

    @Inject
    private MqttPublisherService mqttPublisherService;

    /**
     * Register a new sensor
     * POST /api/sensors
     */
    @POST
    public Response registerSensor(Sensor sensor) {
        try {
            if (sensor == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Sensor data is required"))
                        .build();
            }

            // Validate required fields
            if (sensor.getDeviceId() == null || sensor.getDeviceId().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Device ID is required"))
                        .build();
            }

            if (sensor.getModel() == null || sensor.getModel().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Sensor model is required"))
                        .build();
            }

            // Set default status if not provided
            if (sensor.getStatus() == null || sensor.getStatus().isEmpty()) {
                sensor.setStatus("ACTIVE");
            }

            sensorService.registerSensor(sensor);
            LOGGER.info("Sensor registered successfully: " + sensor.getDeviceId());

            // Notify IoT devices about the new sensor via MQTT
            try {
                mqttPublisherService.notifySensorRegistered(sensor);
                LOGGER.info("MQTT notification sent for new sensor: " + sensor.getDeviceId());
            } catch (Exception mqttEx) {
                // Log but don't fail the registration if MQTT notification fails
                LOGGER.warning("Failed to send MQTT notification for sensor: " + mqttEx.getMessage());
            }

            return Response.status(Response.Status.CREATED)
                    .entity(sensor)
                    .build();

        } catch (Exception e) {
            LOGGER.severe("Error registering sensor: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to register sensor: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get all sensors
     * GET /api/sensors
     */
    @GET
    public Response getAllSensors() {
        try {
            List<Sensor> sensors = sensorService.getAllSensors();
            LOGGER.info("Retrieved " + sensors.size() + " sensors");

            return Response.ok(sensors).build();

        } catch (Exception e) {
            LOGGER.severe("Error retrieving sensors: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve sensors: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get sensor by ID
     * GET /api/sensors/{id}
     */
    @GET
    @Path("/{id}")
    public Response getSensorById(@PathParam("id") String id) {
        try {
            if (id == null || id.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Sensor ID is required"))
                        .build();
            }

            Sensor sensor = sensorService.findById(id);

            if (sensor == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Sensor not found with ID: " + id))
                        .build();
            }

            return Response.ok(sensor).build();

        } catch (Exception e) {
            LOGGER.severe("Error retrieving sensor: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve sensor: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get all active sensors
     * GET /api/sensors/active
     */
    @GET
    @Path("/active")
    public Response getActiveSensors() {
        try {
            List<Sensor> activeSensors = sensorService.getActiveSensors();
            LOGGER.info("Retrieved " + activeSensors.size() + " active sensors");

            return Response.ok(activeSensors).build();

        } catch (Exception e) {
            LOGGER.severe("Error retrieving active sensors: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve active sensors: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get sensors by status
     * GET /api/sensors/status/{status}
     */
    @GET
    @Path("/status/{status}")
    public Response getSensorsByStatus(@PathParam("status") String status) {
        try {
            if (status == null || status.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Status is required"))
                        .build();
            }

            // Validate status value
            if (!isValidStatus(status)) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Invalid status. Must be: ACTIVE, INACTIVE, OFFLINE, or MAINTENANCE"))
                        .build();
            }

            List<Sensor> allSensors = sensorService.getAllSensors();
            List<Sensor> filteredSensors = allSensors.stream()
                    .filter(s -> status.equalsIgnoreCase(s.getStatus()))
                    .collect(Collectors.toList());

            LOGGER.info("Retrieved " + filteredSensors.size() + " sensors with status: " + status);

            return Response.ok(filteredSensors).build();

        } catch (Exception e) {
            LOGGER.severe("Error retrieving sensors by status: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve sensors: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get sensor health status with latest reading
     * GET /api/sensors/{id}/health
     */
    @GET
    @Path("/{id}/health")
    public Response getSensorHealth(@PathParam("id") String id) {
        try {
            if (id == null || id.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Sensor ID is required"))
                        .build();
            }

            Sensor sensor = sensorService.findById(id);
            if (sensor == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Sensor not found with ID: " + id))
                        .build();
            }

            // Get latest reading for this sensor
            List<Reading> readings = readingService.getReadingsBySensor(sensor.getDeviceId());
            Reading latestReading = readings.isEmpty() ? null :
                    readings.stream()
                            .max((r1, r2) -> r1.getTimestamp().compareTo(r2.getTimestamp()))
                            .orElse(null);

            SensorHealth health = new SensorHealth();
            health.sensor = sensor;
            health.isOnline = "ACTIVE".equalsIgnoreCase(sensor.getStatus());
            health.lastReading = latestReading;

            if (latestReading != null) {
                Duration timeSinceLastReading = Duration.between(latestReading.getTimestamp(), Instant.now());
                health.minutesSinceLastReading = timeSinceLastReading.toMinutes();
                health.isResponsive = timeSinceLastReading.toMinutes() < 10; // Consider responsive if reading within 10 min
            } else {
                health.isResponsive = false;
                health.minutesSinceLastReading = -1;
            }

            health.totalReadings = readings.size();

            LOGGER.info("Generated health report for sensor: " + id);

            return Response.ok(health).build();

        } catch (Exception e) {
            LOGGER.severe("Error generating sensor health: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to generate health report: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get overview of all sensors with health status
     * GET /api/sensors/overview
     */
    @GET
    @Path("/overview")
    public Response getSensorsOverview() {
        try {
            List<Sensor> allSensors = sensorService.getAllSensors();

            SensorOverview overview = new SensorOverview();
            overview.totalSensors = allSensors.size();
            overview.activeSensors = (int) allSensors.stream()
                    .filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus()))
                    .count();
            overview.inactiveSensors = (int) allSensors.stream()
                    .filter(s -> "INACTIVE".equalsIgnoreCase(s.getStatus()))
                    .count();
            overview.offlineSensors = (int) allSensors.stream()
                    .filter(s -> "OFFLINE".equalsIgnoreCase(s.getStatus()))
                    .count();

            // Check responsiveness
            int responsiveSensors = 0;
            for (Sensor sensor : allSensors) {
                List<Reading> readings = readingService.getReadingsBySensor(sensor.getDeviceId());
                if (!readings.isEmpty()) {
                    Reading latest = readings.stream()
                            .max((r1, r2) -> r1.getTimestamp().compareTo(r2.getTimestamp()))
                            .orElse(null);
                    if (latest != null) {
                        Duration timeSince = Duration.between(latest.getTimestamp(), Instant.now());
                        if (timeSince.toMinutes() < 10) {
                            responsiveSensors++;
                        }
                    }
                }
            }
            overview.responsiveSensors = responsiveSensors;
            overview.timestamp = Instant.now();

            LOGGER.info("Generated sensors overview");

            return Response.ok(overview).build();

        } catch (Exception e) {
            LOGGER.severe("Error generating overview: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to generate overview: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Update sensor information
     * PUT /api/sensors/{id}
     */
    @PUT
    @Path("/{id}")
    public Response updateSensor(@PathParam("id") String id, Sensor sensor) {
        try {
            if (id == null || id.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Sensor ID is required"))
                        .build();
            }

            if (sensor == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Sensor data is required"))
                        .build();
            }

            // Check if sensor exists
            Sensor existingSensor = sensorService.findById(id);
            if (existingSensor == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Sensor not found with ID: " + id))
                        .build();
            }

            sensor.setId(id);
            sensorService.registerSensor(sensor);
            LOGGER.info("Sensor updated successfully: " + id);

            // Notify IoT devices about the sensor update via MQTT
            try {
                mqttPublisherService.notifySensorUpdated(sensor);
                LOGGER.info("MQTT notification sent for sensor update: " + id);
            } catch (Exception mqttEx) {
                LOGGER.warning("Failed to send MQTT notification for sensor update: " + mqttEx.getMessage());
            }

            return Response.ok(sensor).build();

        } catch (Exception e) {
            LOGGER.severe("Error updating sensor: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to update sensor: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Update sensor status
     * PUT /api/sensors/{id}/status
     */
    @PUT
    @Path("/{id}/status")
    public Response updateSensorStatus(@PathParam("id") String id, StatusUpdateRequest request) {
        try {
            if (id == null || id.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Sensor ID is required"))
                        .build();
            }

            if (request == null || request.status == null || request.status.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Status is required"))
                        .build();
            }

            // Validate status
            if (!isValidStatus(request.status)) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Invalid status. Must be: ACTIVE, INACTIVE, OFFLINE, or MAINTENANCE"))
                        .build();
            }

            Sensor sensor = sensorService.findById(id);
            if (sensor == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Sensor not found with ID: " + id))
                        .build();
            }

            sensor.setStatus(request.status);
            sensorService.registerSensor(sensor);
            LOGGER.info("Sensor status updated to " + request.status + " for sensor: " + id);

            // Notify IoT devices about the status change via MQTT
            try {
                mqttPublisherService.notifySensorUpdated(sensor);
                LOGGER.info("MQTT notification sent for sensor status update: " + id);
            } catch (Exception mqttEx) {
                LOGGER.warning("Failed to send MQTT notification for status update: " + mqttEx.getMessage());
            }

            return Response.ok(sensor).build();

        } catch (Exception e) {
            LOGGER.severe("Error updating sensor status: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to update sensor status: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Deactivate a sensor
     * PUT /api/sensors/{id}/deactivate
     */
    @PUT
    @Path("/{id}/deactivate")
    public Response deactivateSensor(@PathParam("id") String id) {
        try {
            if (id == null || id.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Sensor ID is required"))
                        .build();
            }

            Sensor sensor = sensorService.findById(id);
            if (sensor == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Sensor not found with ID: " + id))
                        .build();
            }

            sensorService.deactivateSensor(id);
            LOGGER.info("Sensor deactivated: " + id);

            // Fetch updated sensor
            Sensor updatedSensor = sensorService.findById(id);

            // Notify IoT devices to stop this sensor via MQTT
            try {
                mqttPublisherService.notifySensorDeleted(sensor.getDeviceId());
                LOGGER.info("MQTT notification sent for sensor deactivation: " + id);
            } catch (Exception mqttEx) {
                LOGGER.warning("Failed to send MQTT notification for sensor deactivation: " + mqttEx.getMessage());
            }

            return Response.ok(updatedSensor)
                    .entity(new SuccessResponse("Sensor deactivated successfully"))
                    .build();

        } catch (Exception e) {
            LOGGER.severe("Error deactivating sensor: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to deactivate sensor: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get sensors by tenant/organization
     * GET /api/sensors/tenant/{organizationName}
     */
    @GET
    @Path("/tenant/{organizationName}")
    public Response getSensorsByTenant(@PathParam("organizationName") String organizationName) {
        try {
            if (organizationName == null || organizationName.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Organization name is required"))
                        .build();
            }

            List<Sensor> allSensors = sensorService.getAllSensors();
            List<Sensor> tenantSensors = allSensors.stream()
                    .filter(s -> s.getTenant() != null &&
                            organizationName.equalsIgnoreCase(s.getTenant().getOrganizationName()))
                    .collect(Collectors.toList());

            LOGGER.info("Retrieved " + tenantSensors.size() + " sensors for tenant: " + organizationName);

            return Response.ok(tenantSensors).build();

        } catch (Exception e) {
            LOGGER.severe("Error retrieving sensors by tenant: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve sensors: " + e.getMessage()))
                    .build();
        }
    }

    // --- Helper Methods ---

    private boolean isValidStatus(String status) {
        return status.equalsIgnoreCase("ACTIVE") ||
                status.equalsIgnoreCase("INACTIVE") ||
                status.equalsIgnoreCase("OFFLINE") ||
                status.equalsIgnoreCase("MAINTENANCE");
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

    public static class StatusUpdateRequest {
        public String status;
    }

    public static class SensorHealth {
        public Sensor sensor;
        public boolean isOnline;
        public boolean isResponsive;
        public long minutesSinceLastReading;
        public Reading lastReading;
        public int totalReadings;
    }

    public static class SensorOverview {
        public int totalSensors;
        public int activeSensors;
        public int inactiveSensors;
        public int offlineSensors;
        public int responsiveSensors;
        public Instant timestamp;
    }
}