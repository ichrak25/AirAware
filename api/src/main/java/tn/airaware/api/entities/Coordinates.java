package tn.airaware.api.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Embeddable;
import java.io.Serializable;

/**
 * Coordinates entity for AirAware sensors and stations.
 * Defines geographic position attributes.
 */
@Embeddable
public class Coordinates implements Serializable {

    @Column("latitude")
    private double latitude;

    @Column("longitude")
    private double longitude;

    @Column("altitude")
    private Double altitude; // optional

    @Column("city")
    private String city;

    @Column("country")
    private String country;

    // --- Constructors ---
    public Coordinates() {
    }

    public Coordinates(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Coordinates(double latitude, double longitude, Double altitude, String city, String country) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.city = city;
        this.country = country;
    }

    // --- Getters & Setters ---
    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Double getAltitude() {
        return altitude;
    }

    public void setAltitude(Double altitude) {
        this.altitude = altitude;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    // --- toString ---
    @Override
    public String toString() {
        return "Coordinates{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", altitude=" + altitude +
                ", city='" + city + '\'' +
                ", country='" + country + '\'' +
                '}';
    }
}
