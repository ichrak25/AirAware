package tn.airaware.api.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import java.io.Serializable;
import java.time.Instant;

/**
 * Alert entity – represents a warning triggered when air quality exceeds a defined threshold.
 */
@Entity("alerts")
public class Alert extends Identity implements Serializable {

    @Column("type")
    private String type; // e.g. CO2_HIGH, PM25_HIGH, VOC_ALERT

    @Column("severity")
    private String severity; // INFO, WARNING, CRITICAL

    @Column("message")
    private String message;

    @Column("triggeredAt")
    private Instant triggeredAt;

    @Column("sensorId")
    private String sensorId;

    @Column("reading")
    private Reading reading; // The reading that caused this alert

    @Column("resolved")
    private boolean resolved = false;

    // --- Constructors ---
    public Alert() {
        super();
        this.triggeredAt = Instant.now();
    }

    public Alert(String type, String severity, String message, String sensorId, Reading reading) {
        this();
        this.type = type;
        this.severity = severity;
        this.message = message;
        this.sensorId = sensorId;
        this.reading = reading;
    }

    // --- Getters & Setters ---
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getTriggeredAt() {
        return triggeredAt;
    }

    public void setTriggeredAt(Instant triggeredAt) {
        this.triggeredAt = triggeredAt;
    }

    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    public Reading getReading() {
        return reading;
    }

    public void setReading(Reading reading) {
        this.reading = reading;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    // --- toString ---
    @Override
    public String toString() {
        return "Alert{" +
                "type='" + type + '\'' +
                ", severity='" + severity + '\'' +
                ", message='" + message + '\'' +
                ", triggeredAt=" + triggeredAt +
                ", sensorId='" + sensorId + '\'' +
                ", resolved=" + resolved +
                ", reading=" + (reading != null ? reading.toString() : "null") +
                '}';
    }
}