package tn.airaware.api.controllers;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tn.airaware.api.entities.Sensor;
import tn.airaware.api.repositories.SensorRepository;

import java.time.Instant;
import java.util.List;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class SensorController {

    @Inject
    SensorRepository sensorRepository;

    // 🔹 1. Get all sensors
    @GET
    public List<Sensor> getAllSensors() {
        return sensorRepository.findAll();
    }

    // 🔹 2. Get by ID
    @GET
    @Path("/{id}")
    public Sensor getSensorById(@PathParam("id") String id) {
        return sensorRepository.findById(id)
                .orElseThrow(() -> new WebApplicationException("Sensor not found", 404));
    }

    // 🔹 3. Create new sensor
    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor.getDeviceId() == null || sensor.getDeviceId().isEmpty()) {
            throw new WebApplicationException("deviceId is required", 400);
        }

        sensor.setLastUpdate(Instant.now());
        sensorRepository.save(sensor);
        return Response.status(Response.Status.CREATED).entity(sensor).build();
    }

    // 🔹 4. Update status
    @PUT
    @Path("/{id}/status")
    public Response updateSensorStatus(@PathParam("id") String id, @QueryParam("status") String status) {
        return sensorRepository.findById(id).map(sensor -> {
            sensor.setStatus(status);
            sensor.setLastUpdate(Instant.now());
            sensorRepository.save(sensor);
            return Response.ok(sensor).build();
        }).orElseThrow(() -> new WebApplicationException("Sensor not found", 404));
    }

    // 🔹 5. Delete
    @DELETE
    @Path("/{id}")
    public Response deleteSensor(@PathParam("id") String id) {
        sensorRepository.deleteById(id);
        return Response.noContent().build();
    }

    // 🔹 6. Find sensors by status
    @GET
    @Path("/status/{status}")
    public List<Sensor> findByStatus(@PathParam("status") String status) {
        return sensorRepository.findByStatus(status);
    }

    // 🔹 7. Find sensors by tenant organization
    @GET
    @Path("/tenant/{organizationName}")
    public List<Sensor> findByTenant(@PathParam("organizationName") String orgName) {
        return sensorRepository.findByTenantOrganizationName(orgName);
    }
}
