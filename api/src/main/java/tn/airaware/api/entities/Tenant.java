package tn.airaware.api.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;
import java.io.Serializable;
import java.util.UUID;

/**
 * Tenant entity - represents an organization/owner of AirAware devices
 * NOTE: Does NOT extend Identity anymore - that was causing confusion
 */
@Entity("tenants")
public class Tenant implements Serializable {

    @Id
    @Column("_id")
    private String id;

    @Column("organizationName")
    private String organizationName;

    @Column("contactEmail")
    private String contactEmail;

    @Column("contactPhone")
    private String contactPhone;

    @Column("address")
    private String address;

    @Column("country")
    private String country;

    @Column("active")
    private boolean active = true;

    // ==================== Constructors ====================

    public Tenant() {
        this.id = UUID.randomUUID().toString();
        this.active = true;
    }

    public Tenant(String organizationName, String contactEmail, String contactPhone,
                  String address, String country) {
        this();
        this.organizationName = organizationName;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.address = address;
        this.country = country;
    }

    // ==================== Getters & Setters ====================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // ==================== toString ====================

    @Override
    public String toString() {
        return "Tenant{" +
                "id='" + id + '\'' +
                ", organizationName='" + organizationName + '\'' +
                ", contactEmail='" + contactEmail + '\'' +
                ", contactPhone='" + contactPhone + '\'' +
                ", address='" + address + '\'' +
                ", country='" + country + '\'' +
                ", active=" + active +
                '}';
    }
}