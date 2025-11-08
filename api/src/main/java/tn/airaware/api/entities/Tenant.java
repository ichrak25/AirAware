package tn.airaware.api.entities;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import java.io.Serializable;

/**
 * Tenant entity — represents an organization or owner of AirAware devices.
 * Extends Identity to inherit ID, credentials, and metadata fields.
 */
@Entity("tenants")
public class Tenant extends Identity implements Serializable {

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

    // --- Constructors ---
    public Tenant() {
        super();
    }

    public Tenant(String organizationName, String contactEmail, String contactPhone, String address, String country) {
        super();
        this.organizationName = organizationName;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.address = address;
        this.country = country;
        this.active = true;
    }

    // --- Getters & Setters ---
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

    // --- toString ---
    @Override
    public String toString() {
        return "Tenant{" +
                "organizationName='" + organizationName + '\'' +
                ", contactEmail='" + contactEmail + '\'' +
                ", contactPhone='" + contactPhone + '\'' +
                ", address='" + address + '\'' +
                ", country='" + country + '\'' +
                ", active=" + active +
                '}';
    }
}
