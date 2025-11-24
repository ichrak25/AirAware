package tn.airaware.api.services;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import tn.airaware.api.entities.Coordinates;
import tn.airaware.api.entities.Sensor;
import tn.airaware.api.entities.Tenant;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class SensorService {

    @Inject
    private MongoDatabase database;

    private MongoCollection<Document> collection() {
        return database.getCollection("sensors");
    }

    /** Register or update a sensor */
    public void registerSensor(Sensor sensor) {
        sensor.setLastUpdate(Instant.now());

        Document doc = new Document()
                .append("_id", sensor.getId()) // from Identity parent
                .append("deviceId", sensor.getDeviceId())
                .append("model", sensor.getModel())
                .append("description", sensor.getDescription())
                .append("status", sensor.getStatus())
                .append("lastUpdate", sensor.getLastUpdate().toString());

        // Embed coordinates (if available)
        if (sensor.getLocation() != null) {
            tn.airaware.core.entities.Coordinates loc = sensor.getLocation();
            doc.append("location", new Document()
                    .append("latitude", loc.getLatitude())
                    .append("longitude", loc.getLongitude()));
        }

        // Embed tenant info (if available)
        if (sensor.getTenant() != null) {
            Tenant tenant = sensor.getTenant();
            doc.append("tenant", new Document()
                    .append("organizationName", tenant.getOrganizationName())
                    .append("contactEmail", tenant.getContactEmail())
                    .append("contactPhone", tenant.getContactPhone()));
        }

        // Upsert (insert or replace)
        collection().replaceOne(
                Filters.eq("_id", sensor.getId()),
                doc,
                new ReplaceOptions().upsert(true)
        );
    }

    /** Find a sensor by ID */
    public Sensor findById(String id) {
        Document doc = collection().find(Filters.eq("_id", id)).first();
        return doc != null ? toSensor(doc) : null;
    }

    /** Get all sensors */
    public List<Sensor> getAllSensors() {
        List<Sensor> sensors = new ArrayList<>();
        for (Document doc : collection().find()) {
            sensors.add(toSensor(doc));
        }
        return sensors;
    }

    /** Get all active sensors */
    public List<Sensor> getActiveSensors() {
        List<Sensor> sensors = new ArrayList<>();
        for (Document doc : collection().find(Filters.eq("status", "ACTIVE"))) {
            sensors.add(toSensor(doc));
        }
        return sensors;
    }

    /** Deactivate a sensor by ID */
    public void deactivateSensor(String id) {
        collection().updateOne(
                Filters.eq("_id", id),
                new Document("$set", new Document("status", "INACTIVE"))
        );
    }

    /** Map MongoDB Document → Sensor object */
    private Sensor toSensor(Document doc) {
        Sensor sensor = new Sensor();
        sensor.setId(doc.getString("_id"));
        sensor.setDeviceId(doc.getString("deviceId"));
        sensor.setModel(doc.getString("model"));
        sensor.setDescription(doc.getString("description"));
        sensor.setStatus(doc.getString("status"));
        if (doc.getString("lastUpdate") != null) {
            sensor.setLastUpdate(Instant.parse(doc.getString("lastUpdate")));
        }

        // Location mapping
        Document locDoc = doc.get("location", Document.class);
        if (locDoc != null) {
            tn.airaware.core.entities.Coordinates loc = new tn.airaware.core.entities.Coordinates();
            loc.setLatitude(locDoc.getDouble("latitude"));
            loc.setLongitude(locDoc.getDouble("longitude"));
            sensor.setLocation(loc);
        }

        // Tenant mapping
        Document tenantDoc = doc.get("tenant", Document.class);
        if (tenantDoc != null) {
            Tenant tenant = new Tenant();
            tenant.setOrganizationName(tenantDoc.getString("organizationName"));
            tenant.setContactEmail(tenantDoc.getString("contactEmail"));
            tenant.setContactPhone(tenantDoc.getString("contactPhone"));
            sensor.setTenant(tenant);
        }

        return sensor;
    }
}
