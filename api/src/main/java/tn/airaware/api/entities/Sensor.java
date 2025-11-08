package tn.airaware.api.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;

import java.io.Serializable;
import java.time.Instant;

/**
 * Represents an AirAware sensor device.
 * Stores metadata, location, and ownership information.
 */
@Entity("sensors")
public class Sensor extends Identity implements Serializable {

    @Column("deviceId")
    private String deviceId;

    @Column("model")
    private String model;

    @Column("description")
    private String description;

    @Column("status")
    private String status; // e.g., ACTIVE, INACTIVE, OFFLINE

    @Column("lastUpdate")
    private Instant lastUpdate;

    @Column("location")
    private Coordinates location;

    @Column("tenant")
    private Tenant tenant;

    // --- Constructors ---
    public Sensor() {
        super();
        this.status = "ACTIVE";
        this.lastUpdate = Instant.now();
    }

    public Sensor(String deviceId, String model, String description, Coordinates location, Tenant tenant) {
        this();
        this.deviceId = deviceId;
        this.model = model;
        this.description = description;
        this.location = location;
        this.tenant = tenant;
    }

    // --- Getters & Setters ---
    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Instant lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public Coordinates getLocation() {
        return location;
    }

    public void setLocation(Coordinates location) {
        this.location = location;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    // --- toString ---
    @Override
    public String toString() {
        return "Sensor{" +
                "deviceId='" + deviceId + '\'' +
                ", model='" + model + '\'' +
                ", description='" + description + '\'' +
                ", status='" + status + '\'' +
                ", lastUpdate=" + lastUpdate +
                ", location=" + location +
                ", tenant=" + (tenant != null ? tenant.getOrganizationName() : "null") +
                '}';
    }


}
