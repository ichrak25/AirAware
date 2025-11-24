package tn.airaware.iam.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;

import java.io.Serializable;
import java.util.UUID;

/**
 * OAuth Client entity for AirAware
 * Represents registered OAuth 2.0 clients (typically organizations/tenants)
 */
@Entity("oauth_clients")
public class OAuthClient implements Serializable {

    @Id
    @Column("_id")
    private String id;

    @Column("client_id")
    private String clientId;  // The actual OAuth client_id (usually organization name)

    @Column("client_secret")
    private String clientSecret;

    @Column("redirect_uri")
    private String redirectUri;

    @Column("allowed_roles")
    private Long allowedRoles;

    @Column("required_scopes")
    private String requiredScopes;

    @Column("supported_grant_types")
    private String supportedGrantTypes;

    @Column("active")
    private boolean active = true;  // ✅ Now properly mapped

    @Column("organization_name")
    private String organizationName;  // Link to Tenant

    // ==================== Constructors ====================

    public OAuthClient() {
        this.id = UUID.randomUUID().toString();
        this.active = true;
    }

    // ==================== Getters & Setters ====================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public Long getAllowedRoles() {
        return allowedRoles;
    }

    public void setAllowedRoles(Long allowedRoles) {
        this.allowedRoles = allowedRoles;
    }

    public String getRequiredScopes() {
        return requiredScopes;
    }

    public void setRequiredScopes(String requiredScopes) {
        this.requiredScopes = requiredScopes;
    }

    public String getSupportedGrantTypes() {
        return supportedGrantTypes;
    }

    public void setSupportedGrantTypes(String supportedGrantTypes) {
        this.supportedGrantTypes = supportedGrantTypes;
    }

    public boolean isActive() {
        return active;  // ✅ Now returns actual value
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    @Override
    public String toString() {
        return "OAuthClient{" +
                "id='" + id + '\'' +
                ", clientId='" + clientId + '\'' +
                ", redirectUri='" + redirectUri + '\'' +
                ", organizationName='" + organizationName + '\'' +
                ", active=" + active +
                ", supportedGrantTypes='" + supportedGrantTypes + '\'' +
                '}';
    }
}