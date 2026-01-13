package tn.airaware.api.services;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.types.ObjectId;
import tn.airaware.api.entities.Alert;
import tn.airaware.api.entities.Reading;
import tn.airaware.api.config.ApiDatabase;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class AlertService {

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(AlertService.class.getName());

    @Inject
    @ApiDatabase
    private MongoDatabase database;

    @Inject
    private NotificationService notificationService;

    private MongoCollection<Document> collection() {
        return database.getCollection("alerts");
    }

    /** Create or update an alert */
    public void saveAlert(Alert alert) {
        saveAlert(alert, true); // Default: send notifications
    }

    /** Create or update an alert with option to send notifications */
    public void saveAlert(Alert alert, boolean sendNotification) {
        if (alert.getTriggeredAt() == null) {
            alert.setTriggeredAt(Instant.now());
        }

        // Check if this is a new alert (for notification purposes)
        boolean isNewAlert = findById(alert.getId()) == null;

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

        // Send notifications for new alerts only (not updates)
        if (isNewAlert && sendNotification && !alert.isResolved()) {
            try {
                LOGGER.info("📤 Triggering notifications for new alert: " + alert.getType() + " [" + alert.getSeverity() + "]");
                notificationService.sendAlertNotification(alert);
            } catch (Exception e) {
                LOGGER.warning("Failed to send alert notification: " + e.getMessage());
            }
        }
    }

    /** Retrieve an alert by ID */
    public Alert findById(String id) {
        // Try to find by string ID first
        Document doc = collection().find(Filters.eq("_id", id)).first();

        // If not found, try finding by ObjectId (in case the ID is a valid ObjectId hex string)
        if (doc == null && ObjectId.isValid(id)) {
            doc = collection().find(Filters.eq("_id", new ObjectId(id))).first();
        }

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

    /** Mark an alert as resolved with optional notes */
    public void resolveAlert(String id, String resolutionNotes) {
        Document updateFields = new Document("resolved", true)
                .append("resolvedAt", Instant.now().toString());
        
        if (resolutionNotes != null && !resolutionNotes.trim().isEmpty()) {
            updateFields.append("resolutionNotes", resolutionNotes.trim());
        }

        // Try string ID first
        long modified = collection().updateOne(
                Filters.eq("_id", id),
                new Document("$set", updateFields)
        ).getModifiedCount();

        // If not found and valid ObjectId, try with ObjectId
        if (modified == 0 && ObjectId.isValid(id)) {
            collection().updateOne(
                    Filters.eq("_id", new ObjectId(id)),
                    new Document("$set", updateFields)
            );
        }
    }

    /** Mark an alert as resolved (without notes - backward compatible) */
    public void resolveAlert(String id) {
        resolveAlert(id, null);
    }

    /** Delete an alert */
    public void deleteAlert(String id) {
        // Try string ID first
        long deleted = collection().deleteOne(Filters.eq("_id", id)).getDeletedCount();

        // If not found and valid ObjectId, try with ObjectId
        if (deleted == 0 && ObjectId.isValid(id)) {
            collection().deleteOne(Filters.eq("_id", new ObjectId(id)));
        }
    }

    /** Helper: Convert MongoDB Document → Alert object */
    private Alert toAlert(Document doc) {
        Alert alert = new Alert();

        // ✅ FIX: Handle both ObjectId and String _id types
        Object idObj = doc.get("_id");
        if (idObj instanceof ObjectId) {
            alert.setId(((ObjectId) idObj).toHexString());
        } else if (idObj != null) {
            alert.setId(idObj.toString());
        }

        alert.setType(doc.getString("type"));
        alert.setSeverity(doc.getString("severity"));
        alert.setMessage(doc.getString("message"));

        // ✅ FIX: Handle triggeredAt as both String and Date
        Object triggeredAtObj = doc.get("triggeredAt");
        if (triggeredAtObj instanceof String) {
            try {
                alert.setTriggeredAt(Instant.parse((String) triggeredAtObj));
            } catch (Exception e) {
                alert.setTriggeredAt(Instant.now());
            }
        } else if (triggeredAtObj instanceof java.util.Date) {
            alert.setTriggeredAt(((java.util.Date) triggeredAtObj).toInstant());
        }

        alert.setSensorId(doc.getString("sensorId"));
        alert.setResolved(doc.getBoolean("resolved", false));
        alert.setResolutionNotes(doc.getString("resolutionNotes"));

        // Handle resolvedAt as both String and Date
        Object resolvedAtObj = doc.get("resolvedAt");
        if (resolvedAtObj instanceof String) {
            try {
                alert.setResolvedAt(Instant.parse((String) resolvedAtObj));
            } catch (Exception e) {
                // Ignore parse errors
            }
        } else if (resolvedAtObj instanceof java.util.Date) {
            alert.setResolvedAt(((java.util.Date) resolvedAtObj).toInstant());
        }

        // Map embedded reading
        Document readingDoc = doc.get("reading", Document.class);
        if (readingDoc != null) {
            Reading reading = new Reading();
            reading.setSensorId(readingDoc.getString("sensorId"));

            Object timestampObj = readingDoc.get("timestamp");
            if (timestampObj instanceof String) {
                try {
                    reading.setTimestamp(Instant.parse((String) timestampObj));
                } catch (Exception e) {
                    // Ignore parse errors
                }
            } else if (timestampObj instanceof java.util.Date) {
                reading.setTimestamp(((java.util.Date) timestampObj).toInstant());
            }

            // ✅ FIX: Handle numeric types safely (Integer, Long, Double)
            reading.setTemperature(getDoubleValue(readingDoc, "temperature"));
            reading.setHumidity(getDoubleValue(readingDoc, "humidity"));
            reading.setCo2(getDoubleValue(readingDoc, "co2"));
            reading.setPm25(getDoubleValue(readingDoc, "pm25"));
            reading.setVoc(getDoubleValue(readingDoc, "voc"));

            alert.setReading(reading);
        }

        return alert;
    }

    /** Helper: Safely get a double value from a document (handles Integer, Long, Double) */
    private Double getDoubleValue(Document doc, String key) {
        Object value = doc.get(key);
        if (value == null) {
            return null;
        } else if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        } else if (value instanceof Long) {
            return ((Long) value).doubleValue();
        } else {
            try {
                return Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}