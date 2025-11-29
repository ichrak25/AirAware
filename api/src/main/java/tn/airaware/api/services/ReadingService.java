package tn.airaware.api.services;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import tn.airaware.api.entities.Coordinates;
import tn.airaware.api.entities.Reading;
import tn.airaware.api.config.ApiDatabase; // Add this import

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class that manages environmental readings (temperature, CO2, etc.)
 */
@ApplicationScoped
public class ReadingService {

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
        for (Document doc : collection().find()) {
            readings.add(toReading(doc));
        }
        return readings;
    }

    /** Find readings by sensor ID */
    public List<Reading> getReadingsBySensor(String sensorId) {
        List<Reading> readings = new ArrayList<>();
        for (Document doc : collection().find(Filters.eq("sensorId", sensorId))) {
            readings.add(toReading(doc));
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

    /** Map Mongo Document → Reading object */
    private Reading toReading(Document doc) {
        Reading r = new Reading();
        r.setId(doc.getString("_id"));
        r.setSensorId(doc.getString("sensorId"));
        r.setTemperature(doc.getDouble("temperature"));
        r.setHumidity(doc.getDouble("humidity"));
        r.setCo2(doc.getDouble("co2"));
        r.setVoc(doc.getDouble("voc"));
        r.setPm25(doc.getDouble("pm25"));
        r.setPm10(doc.getDouble("pm10"));

        if (doc.getString("timestamp") != null) {
            r.setTimestamp(Instant.parse(doc.getString("timestamp")));
        }

        Document loc = doc.get("location", Document.class);
        if (loc != null) {
            Coordinates c = new Coordinates();
            c.setLatitude(loc.getDouble("latitude"));
            c.setLongitude(loc.getDouble("longitude"));
            c.setAltitude(loc.getDouble("altitude"));
            r.setLocation(c);
        }

        return r;
    }
}
