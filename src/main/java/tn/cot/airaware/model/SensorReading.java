package tn.cot.airaware.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorReading implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private ObjectId id;
    private String deviceId;
    private LocalDateTime timestamp;
    
    // Environmental parameters
    private Double temperature; // Celsius
    private Double humidity; // Percentage
    private Double pressure; // hPa
    
    // Gas concentrations
    private Double co2; // ppm (parts per million)
    private Double co; // ppm
    private Double voc; // ppb (parts per billion)
    
    // Particulate matter
    private Double pm25; // μg/m³
    private Double pm10; // μg/m³
    
    // Location data
    private Location location;
    
    // Air Quality Index
    private Integer aqi;
    private String aqiCategory; // Good, Moderate, Unhealthy, etc.
    
    // Metadata
    private String sensorStatus; // ACTIVE, CALIBRATING, ERROR
    private Boolean anomalyDetected;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Location implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String roomName;
        private String buildingName;
        private Double latitude;
        private Double longitude;
        private String floor;
    }
}