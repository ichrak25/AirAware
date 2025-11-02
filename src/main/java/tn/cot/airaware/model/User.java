package tn.cot.airaware.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private ObjectId id;
    private String email;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private String phoneNumber;

    // User role and permissions
    private UserRole role;
    private List<String> permissions;

    // Profile information
    private UserProfile profile;

    // Alert preferences
    private AlertPreferences alertPreferences;

    // Associated devices
    private List<String> deviceIds;

    // Organization membership
    private String organizationId;

    // Account metadata
    private LocalDateTime registrationDate;
    private LocalDateTime lastLoginDate;
    private Boolean emailVerified;
    private Boolean accountActive;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserProfile implements Serializable {
        private static final long serialVersionUID = 1L;

        private String address;
        private String city;
        private String country;
        private String timezone;
        private String languagePreference;

        // Health-related preferences
        private List<String> healthConditions; // e.g., asthma, allergies
        private String sensitivityLevel; // LOW, MEDIUM, HIGH
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertPreferences implements Serializable {
        private static final long serialVersionUID = 1L;

        private Boolean emailEnabled;
        private Boolean smsEnabled;
        private Boolean pushNotificationEnabled;

        // Threshold customization
        private Map<String, ThresholdConfig> customThresholds;

        // Alert schedule
        private Boolean quietHoursEnabled;
        private String quietHoursStart; // e.g., "22:00"
        private String quietHoursEnd; // e.g., "07:00"
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThresholdConfig implements Serializable {
        private static final long serialVersionUID = 1L;

        private Double warningLevel;
        private Double criticalLevel;
        private Boolean alertEnabled;
    }

    public enum UserRole {
        ADMIN,
        USER,
        ORGANIZATION_ADMIN,
        TECHNICIAN,
        VIEWER
    }
}