package tn.airaware.api.repositories;

import jakarta.data.repository.Repository;
import tn.airaware.api.entities.Reading;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReadingRepository extends jakarta.nosql.mapping.Repository<Reading, String> {

    List<Reading> findAll();
    Optional<Reading> findById(String id);
    void deleteById(String id);
    List<Reading> findBySensorId(String sensorId);
    List<Reading> findByDateRange(Instant start, Instant end);
}