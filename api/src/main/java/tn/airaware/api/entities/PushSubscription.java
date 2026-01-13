package tn.airaware.api.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;
import java.time.LocalDateTime;

/**
 * Entity representing a Web Push subscription.
 * Stores the subscription details needed to send push notifications
 * to a user's browser/device even when the app is closed.
 */
@Entity
public class PushSubscription {

    @Id
    private String id;

    /**
     * The unique push subscription endpoint URL
     * This is the URL to send push messages to
     */
    @Column
    private String endpoint;

    /**
     * The p256dh key for encryption (Base64 encoded)
     */
    @Column
    private String p256dh;

    /**
     * The auth secret for encryption (Base64 encoded)
     */
    @Column
    private String auth;

    /**
     * User ID if authenticated (optional)
     */
    @Column
    private String userId;

    /**
     * User agent string for device identification
     */
    @Column
    private String userAgent;

    /**
     * Platform information
     */
    @Column
    private String platform;

    /**
     * When the subscription was created
     */
    @Column
    private LocalDateTime createdAt;

    /**
     * When the subscription was last used
     */
    @Column
    private LocalDateTime lastUsedAt;

    /**
     * Whether the subscription is still active
     */
    @Column
    private boolean active;

    /**
     * Number of successful pushes sent
     */
    @Column
    private int successCount;

    /**
     * Number of failed push attempts
     */
    @Column
    private int failureCount;

    // Constructors
    public PushSubscription() {
        this.active = true;
        this.successCount = 0;
        this.failureCount = 0;
    }

    public PushSubscription(String endpoint, String p256dh, String auth) {
        this();
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getP256dh() {
        return p256dh;
    }

    public void setP256dh(String p256dh) {
        this.p256dh = p256dh;
    }

    public String getAuth() {
        return auth;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    // Utility methods
    public void incrementSuccessCount() {
        this.successCount++;
        this.lastUsedAt = LocalDateTime.now();
    }

    public void incrementFailureCount() {
        this.failureCount++;
        // Deactivate after too many failures
        if (this.failureCount >= 5) {
            this.active = false;
        }
    }

    @Override
    public String toString() {
        return "PushSubscription{" +
                "id='" + id + '\'' +
                ", endpoint='" + (endpoint != null ? endpoint.substring(0, Math.min(50, endpoint.length())) + "..." : "null") + '\'' +
                ", userId='" + userId + '\'' +
                ", platform='" + platform + '\'' +
                ", active=" + active +
                ", createdAt=" + createdAt +
                '}';
    }
}
