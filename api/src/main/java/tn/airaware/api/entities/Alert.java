package tn.airaware.api.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

/**
 * Alert entity — represents a warning triggered when air quality exceeds a defined threshold.
 */
@Setter
@Getter
@Entity("alerts")
public class Alert extends Identity implements Serializable {

    // --- Getters & Setters ---
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
