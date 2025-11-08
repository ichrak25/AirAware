package tn.airaware.api.repositories;

import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import tn.airaware.api.entities.Reading;

import java.time.Instant;
import java.util.List;

/**
 * Repository for Reading entity — manages air quality measurements.
 */
@Repository
public interface ReadingRepository extends jakarta.nosql.mapping.Repository<Reading, String> {

    /**
     * Returns all readings.
     */
    @jakarta.nosql.mapping.Query("SELECT * FROM reading")
    List<Reading> findAll();

    /**
     * Finds readings by sensor ID.
     * @param sensorId The ID of the sensor.
     * @return List of readings from that sensor.
     */
    List<Reading> findBySensorId(String sensorId);

    /**
     * Finds readings within a specific time range.
     * @param start Start timestamp (inclusive)
     * @param end End timestamp (inclusive)
     * @return List of readings in that time range.
     */
    @jakarta.nosql.mapping.Query("SELECT * FROM readings WHERE timestamp >= @start AND timestamp <= @end")
    List<Reading> findByDateRange(Instant start, Instant end);
}
