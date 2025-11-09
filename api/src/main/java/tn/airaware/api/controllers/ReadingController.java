package tn.airaware.api.controllers;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tn.airaware.api.entities.Reading;
import tn.airaware.api.services.ReadingService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * REST Controller for Reading management
 * Handles all sensor reading data - temperature, humidity, CO2, VOC, PM2.5, PM10
 */
@Path("/readings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReadingController {

    private static final Logger LOGGER = Logger.getLogger(ReadingController.class.getName());

    @Inject
    private ReadingService readingService;

    /**
     * Create a new reading (typically called by MQTT or IoT device)
     * POST /api/readings
     */
    @POST
    public Response createReading(Reading reading) {
        try {
            if (reading == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Reading data is required"))
                        .build();
            }

            // Validate required fields
            if (reading.getSensorId() == null || reading.getSensorId().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Sensor ID is required"))
                        .build();
            }

            // Validate sensor data ranges (optional but recommended)
            if (!isValidReading(reading)) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Invalid sensor data values"))
                        .build();
            }

            readingService.saveReading(reading);
            LOGGER.info("Reading created successfully for sensor: " + reading.getSensorId());

            return Response.status(Response.Status.CREATED)
                    .entity(reading)
                    .build();

        } catch (Exception e) {
            LOGGER.severe("Error creating reading: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to create reading: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get all readings
     * GET /api/readings
     */
    @GET
    public Response getAllReadings() {
        try {
            List<Reading> readings = readingService.getAllReadings();
            LOGGER.info("Retrieved " + readings.size() + " readings");

            return Response.ok(readings).build();

        } catch (Exception e) {
            LOGGER.severe("Error retrieving readings: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve readings: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get reading by ID
     * GET /api/readings/{id}
     */
    @GET
    @Path("/{id}")
    public Response getReadingById(@PathParam("id") String id) {
        try {
            if (id == null || id.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Reading ID is required"))
                        .build();
            }

            Reading reading = readingService.findById(id);

            if (reading == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Reading not found with ID: " + id))
                        .build();
            }

            return Response.ok(reading).build();

        } catch (Exception e) {
            LOGGER.severe("Error retrieving reading: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve reading: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get all readings for a specific sensor
     * GET /api/readings/sensor/{sensorId}
     */
    @GET
    @Path("/sensor/{sensorId}")
    public Response getReadingsBySensor(@PathParam("sensorId") String sensorId) {
        try {
            if (sensorId == null || sensorId.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Sensor ID is required"))
                        .build();
            }

            List<Reading> readings = readingService.getReadingsBySensor(sensorId);
            LOGGER.info("Retrieved " + readings.size() + " readings for sensor: " + sensorId);

            return Response.ok(readings).build();

        } catch (Exception e) {
            LOGGER.severe("Error retrieving readings by sensor: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve readings: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get latest reading for a specific sensor
     * GET /api/readings/sensor/{sensorId}/latest
     */
    @GET
    @Path("/sensor/{sensorId}/latest")
    public Response getLatestReadingBySensor(@PathParam("sensorId") String sensorId) {
        try {
            if (sensorId == null || sensorId.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Sensor ID is required"))
                        .build();
            }

            List<Reading> readings = readingService.getReadingsBySensor(sensorId);

            if (readings.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("No readings found for sensor: " + sensorId))
                        .build();
            }

            // Get the most recent reading
            Reading latestReading = readings.stream()
                    .max(Comparator.comparing(Reading::getTimestamp))
                    .orElse(null);

            LOGGER.info("Retrieved latest reading for sensor: " + sensorId);

            return Response.ok(latestReading).build();

        } catch (Exception e) {
            LOGGER.severe("Error retrieving latest reading: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve latest reading: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get readings within the last N hours
     * GET /api/readings/recent?hours=24
     */
    @GET
    @Path("/recent")
    public Response getRecentReadings(@QueryParam("hours") @DefaultValue("24") int hours) {
        try {
            if (hours <= 0 || hours > 720) { // Max 30 days
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Hours must be between 1 and 720"))
                        .build();
            }

            List<Reading> allReadings = readingService.getAllReadings();
            Instant cutoffTime = Instant.now().minus(hours, ChronoUnit.HOURS);

            List<Reading> recentReadings = allReadings.stream()
                    .filter(r -> r.getTimestamp() != null && r.getTimestamp().isAfter(cutoffTime))
                    .sorted(Comparator.comparing(Reading::getTimestamp).reversed())
                    .collect(Collectors.toList());

            LOGGER.info("Retrieved " + recentReadings.size() + " readings from last " + hours + " hours");

            return Response.ok(recentReadings).build();

        } catch (Exception e) {
            LOGGER.severe("Error retrieving recent readings: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to retrieve recent readings: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get statistics for a specific sensor
     * GET /api/readings/sensor/{sensorId}/stats
     */
    @GET
    @Path("/sensor/{sensorId}/stats")
    public Response getSensorStatistics(@PathParam("sensorId") String sensorId) {
        try {
            if (sensorId == null || sensorId.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Sensor ID is required"))
                        .build();
            }

            List<Reading> readings = readingService.getReadingsBySensor(sensorId);

            if (readings.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("No readings found for sensor: " + sensorId))
                        .build();
            }

            // Calculate statistics
            SensorStatistics stats = calculateStatistics(readings);
            LOGGER.info("Calculated statistics for sensor: " + sensorId);

            return Response.ok(stats).build();

        } catch (Exception e) {
            LOGGER.severe("Error calculating statistics: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to calculate statistics: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get air quality summary across all sensors
     * GET /api/readings/summary
     */
    @GET
    @Path("/summary")
    public Response getAirQualitySummary() {
        try {
            List<Reading> allReadings = readingService.getAllReadings();

            if (allReadings.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("No readings available"))
                        .build();
            }

            // Get latest reading per sensor
            Map<String, Reading> latestBySensor = allReadings.stream()
                    .collect(Collectors.groupingBy(
                            Reading::getSensorId,
                            Collectors.collectingAndThen(
                                    Collectors.maxBy(Comparator.comparing(Reading::getTimestamp)),
                                    opt -> opt.orElse(null)
                            )
                    ));

            AirQualitySummary summary = new AirQualitySummary();
            summary.totalSensors = latestBySensor.size();
            summary.latestReadings = new ArrayList<>(latestBySensor.values());
            summary.timestamp = Instant.now();

            LOGGER.info("Generated air quality summary for " + summary.totalSensors + " sensors");

            return Response.ok(summary).build();

        } catch (Exception e) {
            LOGGER.severe("Error generating summary: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to generate summary: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Update an existing reading
     * PUT /api/readings/{id}
     */
    @PUT
    @Path("/{id}")
    public Response updateReading(@PathParam("id") String id, Reading reading) {
        try {
            if (id == null || id.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Reading ID is required"))
                        .build();
            }

            if (reading == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Reading data is required"))
                        .build();
            }

            // Check if reading exists
            Reading existingReading = readingService.findById(id);
            if (existingReading == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Reading not found with ID: " + id))
                        .build();
            }

            reading.setId(id);
            readingService.saveReading(reading);
            LOGGER.info("Reading updated successfully: " + id);

            return Response.ok(reading).build();

        } catch (Exception e) {
            LOGGER.severe("Error updating reading: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to update reading: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Delete all readings for a specific sensor
     * DELETE /api/readings/sensor/{sensorId}
     */
    @DELETE
    @Path("/sensor/{sensorId}")
    public Response deleteReadingsBySensor(@PathParam("sensorId") String sensorId) {
        try {
            if (sensorId == null || sensorId.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Sensor ID is required"))
                        .build();
            }

            readingService.deleteReadingsBySensor(sensorId);
            LOGGER.info("All readings deleted for sensor: " + sensorId);

            return Response.ok()
                    .entity(new SuccessResponse("All readings deleted for sensor: " + sensorId))
                    .build();

        } catch (Exception e) {
            LOGGER.severe("Error deleting readings: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to delete readings: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Delete all readings (use with caution!)
     * DELETE /api/readings/all
     */
    @DELETE
    @Path("/all")
    public Response deleteAllReadings() {
        try {
            readingService.deleteAllReadings();
            LOGGER.warning("All readings deleted from database");

            return Response.ok()
                    .entity(new SuccessResponse("All readings deleted successfully"))
                    .build();

        } catch (Exception e) {
            LOGGER.severe("Error deleting all readings: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to delete all readings: " + e.getMessage()))
                    .build();
        }
    }

    // --- Helper Methods ---

    /**
     * Validate reading values are within reasonable ranges
     */
    private boolean isValidReading(Reading reading) {
        // Temperature: -50°C to 70°C
        if (reading.getTemperature() < -50 || reading.getTemperature() > 70) {
            return false;
        }

        // Humidity: 0% to 100%
        if (reading.getHumidity() < 0 || reading.getHumidity() > 100) {
            return false;
        }

        // CO2: 0 to 10000 ppm (typical indoor range)
        if (reading.getCo2() < 0 || reading.getCo2() > 10000) {
            return false;
        }

        // VOC: 0 to 10000 ppb
        if (reading.getVoc() < 0 || reading.getVoc() > 10000) {
            return false;
        }

        // PM2.5 and PM10: 0 to 1000 µg/m³
        if (reading.getPm25() < 0 || reading.getPm25() > 1000) {
            return false;
        }

        if (reading.getPm10() < 0 || reading.getPm10() > 1000) {
            return false;
        }

        return true;
    }

    /**
     * Calculate statistics from a list of readings
     */
    private SensorStatistics calculateStatistics(List<Reading> readings) {
        SensorStatistics stats = new SensorStatistics();
        stats.totalReadings = readings.size();

        if (readings.isEmpty()) {
            return stats;
        }

        // Calculate averages
        stats.avgTemperature = readings.stream().mapToDouble(Reading::getTemperature).average().orElse(0);
        stats.avgHumidity = readings.stream().mapToDouble(Reading::getHumidity).average().orElse(0);
        stats.avgCo2 = readings.stream().mapToDouble(Reading::getCo2).average().orElse(0);
        stats.avgVoc = readings.stream().mapToDouble(Reading::getVoc).average().orElse(0);
        stats.avgPm25 = readings.stream().mapToDouble(Reading::getPm25).average().orElse(0);
        stats.avgPm10 = readings.stream().mapToDouble(Reading::getPm10).average().orElse(0);

        // Calculate min/max
        stats.minTemperature = readings.stream().mapToDouble(Reading::getTemperature).min().orElse(0);
        stats.maxTemperature = readings.stream().mapToDouble(Reading::getTemperature).max().orElse(0);
        stats.minCo2 = readings.stream().mapToDouble(Reading::getCo2).min().orElse(0);
        stats.maxCo2 = readings.stream().mapToDouble(Reading::getCo2).max().orElse(0);
        stats.minPm25 = readings.stream().mapToDouble(Reading::getPm25).min().orElse(0);
        stats.maxPm25 = readings.stream().mapToDouble(Reading::getPm25).max().orElse(0);

        // Time range
        stats.firstReading = readings.stream().min(Comparator.comparing(Reading::getTimestamp))
                .map(Reading::getTimestamp).orElse(null);
        stats.lastReading = readings.stream().max(Comparator.comparing(Reading::getTimestamp))
                .map(Reading::getTimestamp).orElse(null);

        return stats;
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

    public static class SensorStatistics {
        public int totalReadings;
        public double avgTemperature;
        public double avgHumidity;
        public double avgCo2;
        public double avgVoc;
        public double avgPm25;
        public double avgPm10;
        public double minTemperature;
        public double maxTemperature;
        public double minCo2;
        public double maxCo2;
        public double minPm25;
        public double maxPm25;
        public Instant firstReading;
        public Instant lastReading;
    }

    public static class AirQualitySummary {
        public int totalSensors;
        public List<Reading> latestReadings;
        public Instant timestamp;
    }
}