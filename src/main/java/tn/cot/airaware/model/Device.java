package tn.cot.airaware.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Device implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private ObjectId id;
    private String deviceId; // Unique hardware identifier
    private String deviceName;
    private String deviceType; // RASPBERRY_PI, ESP32, etc.
    private String firmwareVersion;
    
    // Location information
    private Location location;
    
    // Device status
    private DeviceStatus status;
    private LocalDateTime lastActiveTime;
    private LocalDateTime registrationDate;
    
    // Associated sensors
    private List<SensorInfo> sensors;
    
    // Configuration
    private DeviceConfiguration configuration;
    
    // Owner information
    private String userId;
    private String organizationId;
    
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
        private String address;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SensorInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String sensorType; // BME680, SGP30, PMS5003, MH-Z19
        private String model;
        private LocalDateTime lastCalibrationDate;
        private LocalDateTime nextCalibrationDate;
        private SensorStatus sensorStatus;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceConfiguration implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private Integer readingIntervalSeconds; // e.g., 30, 300
        private Boolean autoCalibrationEnabled;
        private String communicationProtocol; // MQTT, HTTP
        private Boolean localStorageEnabled;
    }
    
    public enum DeviceStatus {
        ACTIVE,
        INACTIVE,
        MAINTENANCE,
        ERROR,
        CALIBRATING
    }
    
    public enum SensorStatus {
        OPERATIONAL,
        CALIBRATING,
        DEGRADED,
        FAULTY
    }
}