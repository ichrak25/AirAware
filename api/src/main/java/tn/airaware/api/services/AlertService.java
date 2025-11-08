package tn.airaware.api.services;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import tn.airaware.api.entities.Alert;
import tn.airaware.api.entities.Reading;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class AlertService {

    @Inject
    private MongoDatabase database;

    private MongoCollection<Document> collection() {
        return database.getCollection("alerts");
    }

    /** Create or update an alert */
    public void saveAlert(Alert alert) {
        if (alert.getTriggeredAt() == null) {
            alert.setTriggeredAt(Instant.now());
        }

        Document doc = new Document()
                .append("_id", alert.getId())
                .append("type", alert.getType())
                .append("severity", alert.getSeverity())
                .append("message", alert.getMessage())
                .append("triggeredAt", alert.getTriggeredAt().toString())
                .append("sensorId", alert.getSensorId())
                .append("resolved", alert.isResolved());

        // Include the reading that triggered the alert (nested document)
        if (alert.getReading() != null) {
            Reading r = alert.getReading();
            Document readingDoc = new Document()
                    .append("sensorId", r.getSensorId())
                    .append("timestamp", r.getTimestamp() != null ? r.getTimestamp().toString() : null)
                    .append("temperature", r.getTemperature())
                    .append("humidity", r.getHumidity())
                    .append("co2", r.getCo2())
                    .append("pm25", r.getPm25())
                    .append("voc", r.getVoc());
            doc.append("reading", readingDoc);
        }

        // Insert or update existing alert
        collection().replaceOne(Filters.eq("_id", alert.getId()), doc, new ReplaceOptions().upsert(true));
    }

    /** Retrieve an alert by ID */
    public Alert findById(String id) {
        Document doc = collection().find(Filters.eq("_id", id)).first();
        return doc != null ? toAlert(doc) : null;
    }

    /** Retrieve all alerts */
    public List<Alert> getAllAlerts() {
        List<Alert> alerts = new ArrayList<>();
        for (Document d : collection().find()) {
            alerts.add(toAlert(d));
        }
        return alerts;
    }

    /** Retrieve alerts by severity (INFO, WARNING, CRITICAL, etc.) */
    public List<Alert> getAlertsBySeverity(String severity) {
        List<Alert> alerts = new ArrayList<>();
        for (Document d : collection().find(Filters.eq("severity", severity))) {
            alerts.add(toAlert(d));
        }
        return alerts;
    }

    /** Retrieve alerts for a specific sensor */
    public List<Alert> getAlertsBySensor(String sensorId) {
        List<Alert> alerts = new ArrayList<>();
        for (Document d : collection().find(Filters.eq("sensorId", sensorId))) {
            alerts.add(toAlert(d));
        }
        return alerts;
    }

    /** Mark an alert as resolved */
    public void resolveAlert(String id) {
        collection().updateOne(Filters.eq("_id", id),
                new Document("$set", new Document("resolved", true)));
    }

    /** Delete an alert */
    public void deleteAlert(String id) {
        collection().deleteOne(Filters.eq("_id", id));
    }

    /** Helper: Convert MongoDB Document → Alert object */
    private Alert toAlert(Document doc) {
        Alert alert = new Alert();
        alert.setId(doc.getString("_id"));
        alert.setType(doc.getString("type"));
        alert.setSeverity(doc.getString("severity"));
        alert.setMessage(doc.getString("message"));
        if (doc.getString("triggeredAt") != null) {
            alert.setTriggeredAt(Instant.parse(doc.getString("triggeredAt")));
        }
        alert.setSensorId(doc.getString("sensorId"));
        alert.setResolved(doc.getBoolean("resolved", false));

        // Map embedded reading
        Document readingDoc = doc.get("reading", Document.class);
        if (readingDoc != null) {
            Reading reading = new Reading();
            reading.setSensorId(readingDoc.getString("sensorId"));
            if (readingDoc.getString("timestamp") != null) {
                reading.setTimestamp(Instant.parse(readingDoc.getString("timestamp")));
            }
            reading.setTemperature(readingDoc.getDouble("temperature"));
            reading.setHumidity(readingDoc.getDouble("humidity"));
            reading.setCo2(readingDoc.getDouble("co2"));
            reading.setPm25(readingDoc.getDouble("pm25"));
            reading.setVoc(readingDoc.getDouble("voc"));
            alert.setReading(reading);
        }

        return alert;
    }
}
