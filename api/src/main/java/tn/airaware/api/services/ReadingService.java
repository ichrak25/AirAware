package tn.airaware.api.services;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.types.ObjectId;
import tn.airaware.api.entities.Coordinates;
import tn.airaware.api.entities.Reading;
import tn.airaware.api.config.ApiDatabase;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service class that manages environmental readings (temperature, CO2, etc.)
 * FIXED: Proper ObjectId handling - ObjectId cannot be cast to String directly
 */
@ApplicationScoped
public class ReadingService {

    private static final Logger LOGGER = Logger.getLogger(ReadingService.class.getName());

    @Inject
    @ApiDatabase
    private MongoDatabase database;

    private MongoCollection<Document> collection() {
        return database.getCollection("readings");
    }

    /** Save or update a reading */
    public void saveReading(Reading reading) {
        if (reading.getTimestamp() == null) {
            reading.setTimestamp(Instant.now());
        }

        Document doc = new Document()
                .append("_id", reading.getId())
                .append("sensorId", reading.getSensorId())
                .append("temperature", reading.getTemperature())
                .append("humidity", reading.getHumidity())
                .append("co2", reading.getCo2())
                .append("voc", reading.getVoc())
                .append("pm25", reading.getPm25())
                .append("pm10", reading.getPm10())
                .append("timestamp", reading.getTimestamp().toString());

        // Embedded location
        if (reading.getLocation() != null) {
            Coordinates c = reading.getLocation();
            Document loc = new Document()
                    .append("latitude", c.getLatitude())
                    .append("longitude", c.getLongitude())
                    .append("altitude", c.getAltitude());
            doc.append("location", loc);
        }

        collection().replaceOne(Filters.eq("_id", reading.getId()), doc, new ReplaceOptions().upsert(true));
    }

    /** Find a reading by ID */
    public Reading findById(String id) {
        Document doc = collection().find(Filters.eq("_id", id)).first();
        return doc != null ? toReading(doc) : null;
    }

    /** Find all readings */
    public List<Reading> getAllReadings() {
        List<Reading> readings = new ArrayList<>();
        try {
            // Sort by timestamp descending for efficiency
            for (Document doc : collection().find().sort(Sorts.descending("timestamp"))) {
                try {
                    Reading reading = toReading(doc);
                    if (reading != null) {
                        readings.add(reading);
                    }
                } catch (Exception e) {
                    // FIXED: Use getDocumentId() helper instead of doc.getString("_id")
                    LOGGER.log(Level.WARNING, "Failed to parse reading document: " + getDocumentId(doc), e);
                    // Continue processing other documents
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching all readings", e);
            throw e;
        }
        return readings;
    }

    /** Find all readings with limit */
    public List<Reading> getAllReadings(int limit) {
        List<Reading> readings = new ArrayList<>();
        try {
            var cursor = collection().find()
                    .sort(Sorts.descending("timestamp"))
                    .limit(limit > 0 ? limit : Integer.MAX_VALUE);

            for (Document doc : cursor) {
                try {
                    Reading reading = toReading(doc);
                    if (reading != null) {
                        readings.add(reading);
                    }
                } catch (Exception e) {
                    // FIXED: Use getDocumentId() helper instead of doc.getString("_id")
                    LOGGER.log(Level.WARNING, "Failed to parse reading document: " + getDocumentId(doc), e);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching readings with limit", e);
            throw e;
        }
        return readings;
    }

    /** Find readings by sensor ID */
    public List<Reading> getReadingsBySensor(String sensorId) {
        List<Reading> readings = new ArrayList<>();
        try {
            for (Document doc : collection().find(Filters.eq("sensorId", sensorId))
                    .sort(Sorts.descending("timestamp"))) {
                try {
                    Reading reading = toReading(doc);
                    if (reading != null) {
                        readings.add(reading);
                    }
                } catch (Exception e) {
                    // FIXED: Use getDocumentId() helper instead of doc.getString("_id")
                    LOGGER.log(Level.WARNING, "Failed to parse reading document: " + getDocumentId(doc), e);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching readings for sensor: " + sensorId, e);
            throw e;
        }
        return readings;
    }

    /** Delete readings for a specific sensor */
    public void deleteReadingsBySensor(String sensorId) {
        collection().deleteMany(Filters.eq("sensorId", sensorId));
    }

    /** Delete all readings */
    public void deleteAllReadings() {
        collection().deleteMany(new Document());
    }

    /**
     * FIXED: Safely get document ID as String
     * Handles both ObjectId and String _id fields
     */
    private String getDocumentId(Document doc) {
        if (doc == null) {
            return "null";
        }

        Object idObj = doc.get("_id");

        if (idObj == null) {
            return "no-id";
        }

        if (idObj instanceof ObjectId) {
            return ((ObjectId) idObj).toHexString();
        }

        return idObj.toString();
    }

    /**
     * Map Mongo Document → Reading object
     * FIXED: Proper ObjectId to String conversion for _id field
     */
    private Reading toReading(Document doc) {
        if (doc == null) {
            return null;
        }

        Reading r = new Reading();

        try {
            // FIXED: Handle _id which can be ObjectId or String
            Object idObj = doc.get("_id");
            if (idObj instanceof ObjectId) {
                r.setId(((ObjectId) idObj).toHexString());
            } else if (idObj != null) {
                r.setId(idObj.toString());
            }

            r.setSensorId(doc.getString("sensorId"));

            // FIXED: Use null-safe getDouble with default values
            r.setTemperature(getDoubleValue(doc, "temperature", 0.0));
            r.setHumidity(getDoubleValue(doc, "humidity", 0.0));
            r.setCo2(getDoubleValue(doc, "co2", 0.0));
            r.setVoc(getDoubleValue(doc, "voc", 0.0));
            r.setPm25(getDoubleValue(doc, "pm25", 0.0));
            r.setPm10(getDoubleValue(doc, "pm10", 0.0));

            // Parse timestamp - handle both String and other formats
            Object timestampObj = doc.get("timestamp");
            if (timestampObj != null) {
                if (timestampObj instanceof String) {
                    try {
                        r.setTimestamp(Instant.parse((String) timestampObj));
                    } catch (Exception e) {
                        LOGGER.warning("Could not parse timestamp string: " + timestampObj);
                        r.setTimestamp(Instant.now());
                    }
                } else if (timestampObj instanceof java.util.Date) {
                    r.setTimestamp(((java.util.Date) timestampObj).toInstant());
                } else if (timestampObj instanceof Long) {
                    r.setTimestamp(Instant.ofEpochMilli((Long) timestampObj));
                } else {
                    LOGGER.warning("Unknown timestamp type: " + timestampObj.getClass().getName());
                    r.setTimestamp(Instant.now());
                }
            }

            // Parse location subdocument
            Document loc = doc.get("location", Document.class);
            if (loc != null) {
                Coordinates c = new Coordinates();
                c.setLatitude(getDoubleValue(loc, "latitude", 0.0));
                c.setLongitude(getDoubleValue(loc, "longitude", 0.0));
                c.setAltitude(getDoubleValue(loc, "altitude", 0.0));

                // Also get city and country if present
                if (loc.containsKey("city")) {
                    c.setCity(loc.getString("city"));
                }
                if (loc.containsKey("country")) {
                    c.setCountry(loc.getString("country"));
                }

                r.setLocation(c);
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error converting document to Reading", e);
            throw e;
        }

        return r;
    }

    /**
     * Safely extract a double value from a MongoDB document
     * Handles null values and different numeric types (Integer, Long, Double)
     */
    private double getDoubleValue(Document doc, String field, double defaultValue) {
        Object value = doc.get(field);

        if (value == null) {
            return defaultValue;
        }

        if (value instanceof Double) {
            return (Double) value;
        }

        if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        }

        if (value instanceof Long) {
            return ((Long) value).doubleValue();
        }

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                LOGGER.warning("Could not parse string as double for field " + field + ": " + value);
                return defaultValue;
            }
        }

        LOGGER.warning("Unexpected type for field " + field + ": " + value.getClass().getName());
        return defaultValue;
    }
}