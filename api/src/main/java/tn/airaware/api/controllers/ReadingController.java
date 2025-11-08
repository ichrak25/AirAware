package tn.airaware.api.controllers;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tn.airaware.api.entities.Reading;
import tn.airaware.api.repositories.ReadingRepository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Path("/readings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class ReadingController {

    @Inject
    ReadingRepository readingRepository;

    // 🔹 1. Get all readings
    @GET
    public List<Reading> getAllReadings() {
        return readingRepository.findAll();
    }

    // 🔹 2. Get readings by sensor ID
    @GET
    @Path("/sensor/{sensorId}")
    public List<Reading> getReadingsBySensor(@PathParam("sensorId") String sensorId) {
        return readingRepository.findBySensorId(sensorId);
    }

    // 🔹 3. Get readings in a date range
    // Example: /api/readings/range?start=2025-11-07T00:00:00Z&end=2025-11-08T23:59:59Z
    @GET
    @Path("/range")
    public List<Reading> getReadingsByRange(@QueryParam("start") String start, @QueryParam("end") String end) {
        Instant startTime = (start != null) ? Instant.parse(start) : Instant.EPOCH;
        Instant endTime = (end != null) ? Instant.parse(end) : Instant.now();
        return readingRepository.findByDateRange(startTime, endTime);
    }

    // 🔹 4. Get latest reading (across all sensors)
    @GET
    @Path("/latest")
    public Reading getLatestReading() {
        return readingRepository.findAll().stream()
                .max(Comparator.comparing(Reading::getTimestamp))
                .orElseThrow(() -> new WebApplicationException("No readings found", 404));
    }

    // 🔹 5. Save a new reading (manual insert or MQTT simulation)
    @POST
    public Response createReading(Reading reading) {
        if (reading.getSensorId() == null || reading.getSensorId().isEmpty()) {
            throw new WebApplicationException("sensorId is required", 400);
        }
        reading.setTimestamp(Instant.now());
        readingRepository.save(reading);
        return Response.status(Response.Status.CREATED).entity(reading).build();
    }

    // 🔹 6. Delete all readings for a sensor (optional)
    @DELETE
    @Path("/sensor/{sensorId}")
    public Response deleteBySensor(@PathParam("sensorId") String sensorId) {
        List<Reading> readings = readingRepository.findBySensorId(sensorId);
        readings.forEach(r -> readingRepository.deleteById(r.getId()));
        return Response.noContent().build();
    }
}
