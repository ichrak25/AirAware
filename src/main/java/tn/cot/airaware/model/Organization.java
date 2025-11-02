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
public class Organization implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private ObjectId id;
    private String organizationName;
    private OrganizationType type;
    
    // Contact information
    private ContactInfo contactInfo;
    
    // Subscription details
    private SubscriptionPlan subscriptionPlan;
    private LocalDateTime subscriptionStartDate;
    private LocalDateTime subscriptionEndDate;
    private Boolean isActive;
    
    // Associated resources
    private List<String> deviceIds;
    private List<String> userIds;
    private List<String> locationIds;
    
    // Settings
    private OrganizationSettings settings;
    
    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String address;
        private String city;
        private String country;
        private String postalCode;
        private String phone;
        private String email;
        private String website;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrganizationSettings implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private Integer maxDevices;
        private Integer maxUsers;
        private Boolean multiLocationEnabled;
        private Boolean advancedAnalyticsEnabled;
        private Boolean customBrandingEnabled;
        private Integer dataRetentionDays;
    }
    
    public enum OrganizationType {
        RESIDENTIAL,
        CORPORATE,
        EDUCATIONAL,
        HEALTHCARE,
        INDUSTRIAL,
        HOSPITALITY,
        PUBLIC_INSTITUTION,
        RESEARCH
    }
    
    public enum SubscriptionPlan {
        FREE,
        BASIC,
        PROFESSIONAL,
        ENTERPRISE,
        CUSTOM
    }
}