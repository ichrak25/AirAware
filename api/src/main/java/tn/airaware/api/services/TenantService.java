package tn.airaware.api.services;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import tn.airaware.api.entities.Tenant;

import java.util.ArrayList;
import java.util.List;

/**
 * Service to manage Tenant entities (organizations using AirAware sensors).
 */
@ApplicationScoped
public class TenantService {

    @Inject
    private MongoDatabase database;

    private MongoCollection<Document> collection() {
        return database.getCollection("tenants");
    }

    /** Create or update a tenant */
    public void saveTenant(Tenant tenant) {
        if (tenant.getOrganizationName() == null || tenant.getOrganizationName().isEmpty()) {
            throw new IllegalArgumentException("Organization name is required");
        }

        Document doc = new Document()
                .append("_id", tenant.getId())
                .append("organizationName", tenant.getOrganizationName())
                .append("contactEmail", tenant.getContactEmail())
                .append("contactPhone", tenant.getContactPhone())
                .append("address", tenant.getAddress())
                .append("country", tenant.getCountry())
                .append("active", tenant.isActive());

        collection().replaceOne(Filters.eq("_id", tenant.getId()), doc, new ReplaceOptions().upsert(true));
    }

    /** Find tenant by ID */
    public Tenant findById(String id) {
        Document doc = collection().find(Filters.eq("_id", id)).first();
        return doc != null ? toTenant(doc) : null;
    }

    /** Find tenant by organization name */
    public Tenant findByOrganizationName(String orgName) {
        Document doc = collection().find(Filters.eq("organizationName", orgName)).first();
        return doc != null ? toTenant(doc) : null;
    }

    /** List all tenants */
    public List<Tenant> getAllTenants() {
        List<Tenant> tenants = new ArrayList<>();
        for (Document doc : collection().find()) {
            tenants.add(toTenant(doc));
        }
        return tenants;
    }

    /** Deactivate a tenant (set active = false) */
    public void deactivateTenant(String id) {
        collection().updateOne(Filters.eq("_id", id),
                new Document("$set", new Document("active", false)));
    }

    /** Delete tenant by ID */
    public void deleteTenant(String id) {
        collection().deleteOne(Filters.eq("_id", id));
    }

    /** Convert MongoDB document to Tenant entity */
    private Tenant toTenant(Document doc) {
        Tenant tenant = new Tenant();
        tenant.setId(doc.getString("_id"));
        tenant.setOrganizationName(doc.getString("organizationName"));
        tenant.setContactEmail(doc.getString("contactEmail"));
        tenant.setContactPhone(doc.getString("contactPhone"));
        tenant.setAddress(doc.getString("address"));
        tenant.setCountry(doc.getString("country"));
        tenant.setActive(Boolean.TRUE.equals(doc.getBoolean("active")));
        return tenant;
    }
}
