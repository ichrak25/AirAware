package tn.airaware.core.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;
import tn.airaware.core.security.Argon2Utils;

import java.io.Serializable;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

/**
 * Unified Identity entity for AirAware platform
 * Used by both API and IAM modules for user management
 */
@Entity("identities")
public class Identity implements Serializable, Principal {

    @Id
    @Column("_id")
    private String id;

    @Column("username")
    private String username;

    @Column("email")
    private String email;

    @Column("password")
    private String password;

    @Column("creationDate")
    private Instant creationDate;

    @Column("role")
    private Long roles;

    @Column("scopes")
    private String scopes;

    @Column("isAccountActivated")
    private boolean isAccountActivated;

    // ==================== Constructors ====================

    public Identity() {
        this.id = UUID.randomUUID().toString();
        this.creationDate = Instant.now();
        this.isAccountActivated = false;
    }

    public Identity(String username, String email, String password, Long roles) {
        this();
        this.username = username;
        this.email = email;
        this.password = password;
        this.roles = roles;
    }

    // ==================== Getters & Setters ====================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Instant getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Instant creationDate) {
        this.creationDate = creationDate;
    }

    public Long getRoles() {
        return roles;
    }

    public void setRoles(Long roles) {
        this.roles = roles;
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public boolean isAccountActivated() {
        return isAccountActivated;
    }

    public void setAccountActivated(boolean accountActivated) {
        this.isAccountActivated = accountActivated;
    }

    // ==================== Principal Interface ====================

    @Override
    public String getName() {
        return username;
    }

    // ==================== Password Hashing ====================

    /**
     * Hash a plain text password using Argon2
     */
    public void hashPassword(String plainPassword, Argon2Utils argon2Utils) {
        this.password = argon2Utils.hash(plainPassword.toCharArray());
    }

    // ==================== toString ====================

    @Override
    public String toString() {
        return "Identity{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", creationDate=" + creationDate +
                ", roles=" + roles +
                ", scopes='" + scopes + '\'' +
                ", isAccountActivated=" + isAccountActivated +
                '}';
    }
}