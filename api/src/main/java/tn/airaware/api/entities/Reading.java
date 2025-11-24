package tn.airaware.api.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import java.io.Serializable;
import tn.airaware.core.entities.Identity;
import java.time.Instant;

/**
 * Reading entity — represents a single air quality measurement captured by a sensor.
 */
@Entity("readings")
public class Reading extends Identity implements Serializable {

    @Column("sensorId")
    private String sensorId;  // Reference to the sensor that produced this reading

    @Column("temperature")
    private double temperature;

    @Column("humidity")
    private double humidity;

    @Column("co2")
    private double co2;  // Carbon dioxide in ppm

    @Column("voc")
    private double voc;  // Volatile Organic Compounds

    @Column("pm25")
    private double pm25; // Particulate Matter 2.5 µm

    @Column("pm10")
    private double pm10; // Particulate Matter 10 µm

    @Column("timestamp")
    private Instant timestamp;

    @Column("location")
    private Coordinates location; // Optional - could duplicate sensor location

    // --- Constructors ---
    public Reading() {
        super();
        this.timestamp = Instant.now();
    }

    public Reading(String sensorId, double temperature, double humidity, double co2,
                   double voc, double pm25, double pm10, Coordinates location) {
        this();
        this.sensorId = sensorId;
        this.temperature = temperature;
        this.humidity = humidity;
        this.co2 = co2;
        this.voc = voc;
        this.pm25 = pm25;
        this.pm10 = pm10;
        this.location = location;
    }

    // --- Getters & Setters ---
    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getHumidity() {
        return humidity;
    }

    public void setHumidity(double humidity) {
        this.humidity = humidity;
    }

    public double getCo2() {
        return co2;
    }

    public void setCo2(double co2) {
        this.co2 = co2;
    }

    public double getVoc() {
        return voc;
    }

    public void setVoc(double voc) {
        this.voc = voc;
    }

    public double getPm25() {
        return pm25;
    }

    public void setPm25(double pm25) {
        this.pm25 = pm25;
    }

    public double getPm10() {
        return pm10;
    }

    public void setPm10(double pm10) {
        this.pm10 = pm10;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Coordinates getLocation() {
        return location;
    }

    public void setLocation(Coordinates location) {
        this.location = location;
    }

    // --- toString ---
    @Override
    public String toString() {
        return "Reading{" +
                "sensorId='" + sensorId + '\'' +
                ", temperature=" + temperature +
                ", humidity=" + humidity +
                ", co2=" + co2 +
                ", voc=" + voc +
                ", pm25=" + pm25 +
                ", pm10=" + pm10 +
                ", timestamp=" + timestamp +
                ", location=" + location +
                '}';
    }
}
