package tn.airaware.api.controllers;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tn.airaware.api.entities.Alert;
import tn.airaware.api.repositories.AlertRepository;

import java.time.Instant;
import java.util.List;

@Path("/alerts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class AlertController {

    @Inject
    AlertRepository alertRepository;

    // 🔹 1. Get all alerts
    @GET
    public List<Alert> getAllAlerts() {
        return alertRepository.findAll();
    }

    // 🔹 2. Get alerts by sensor
    @GET
    @Path("/sensor/{sensorId}")
    public List<Alert> getBySensor(@PathParam("sensorId") String sensorId) {
        return alertRepository.findBySensorId(sensorId);
    }

    // 🔹 3. Get alerts by severity
    @GET
    @Path("/severity/{level}")
    public List<Alert> getBySeverity(@PathParam("level") String severity) {
        return alertRepository.findBySeverity(severity.toUpperCase());
    }

    // 🔹 4. Get resolved or unresolved alerts
    @GET
    @Path("/resolved/{status}")
    public List<Alert> getByResolved(@PathParam("status") boolean resolved) {
        return alertRepository.findByResolved(resolved);
    }

    // 🔹 5. Get single alert
    @GET
    @Path("/{id}")
    public Alert getAlertById(@PathParam("id") String id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new WebApplicationException("Alert not found", 404));
    }

    // 🔹 6. Create new alert
    @POST
    public Response createAlert(Alert alert) {
        if (alert.getSensorId() == null || alert.getSensorId().isEmpty()) {
            throw new WebApplicationException("sensorId is required", 400);
        }

        alert.setTriggeredAt(Instant.now());
        alertRepository.save(alert);
        return Response.status(Response.Status.CREATED).entity(alert).build();
    }

    // 🔹 7. Resolve alert
    @PUT
    @Path("/{id}/resolve")
    public Response resolveAlert(@PathParam("id") String id) {
        return alertRepository.findById(id)
                .map(alert -> {
                    alert.setResolved(true);
                    alertRepository.save(alert);
                    return Response.ok(alert).build();
                })
                .orElseThrow(() -> new WebApplicationException("Alert not found", 404));
    }

    // 🔹 8. Delete an alert
    @DELETE
    @Path("/{id}")
    public Response deleteAlert(@PathParam("id") String id) {
        alertRepository.deleteById(id);
        return Response.noContent().build();
    }
}
