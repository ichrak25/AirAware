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
public class Alert implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private ObjectId id;
    private String deviceId;
    private String userId;
    private LocalDateTime timestamp;
    
    // Alert classification
    private AlertType alertType;
    private AlertSeverity severity;
    private AlertStatus status;
    
    // Alert details
    private String title;
    private String message;
    private String recommendation;
    
    // Triggered parameter
    private String triggeredParameter; // CO2, VOC, PM2.5, etc.
    private Double measuredValue;
    private Double thresholdValue;
    private String unit;
    
    // Location context
    private Location location;
    
    // ML prediction data (if applicable)
    private Boolean isPredictive;
    private Double confidenceScore;
    
    // User interaction
    private LocalDateTime acknowledgedAt;
    private String acknowledgedBy;
    private String userNotes;
    
    // Notification tracking
    private NotificationStatus notificationStatus;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Location implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String roomName;
        private String buildingName;
        private Double latitude;
        private Double longitude;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationStatus implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private Boolean emailSent;
        private Boolean smsSent;
        private Boolean pushSent;
        private LocalDateTime lastNotificationTime;
        private Integer retryCount;
    }
    
    public enum AlertType {
        THRESHOLD_EXCEEDED,
        ANOMALY_DETECTED,
        PREDICTIVE_WARNING,
        SENSOR_MALFUNCTION,
        DEVICE_OFFLINE,
        CALIBRATION_REQUIRED
    }
    
    public enum AlertSeverity {
        INFO,
        WARNING,
        CRITICAL,
        EMERGENCY
    }
    
    public enum AlertStatus {
        ACTIVE,
        ACKNOWLEDGED,
        RESOLVED,
        DISMISSED
    }
}