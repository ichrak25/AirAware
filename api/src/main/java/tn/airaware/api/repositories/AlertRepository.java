package tn.airaware.api.repositories;

import jakarta.data.repository.Repository;
import tn.airaware.api.entities.Alert;
import tn.airaware.api.entities.Identity;

import java.util.List;

/**
 * Repository for Alert entity — handles alert history and monitoring.
 */
@Repository
public interface AlertRepository extends jakarta.nosql.mapping.Repository<Identity, String> {
    List<Alert> findBySensorId(String sensorId);
    List<Alert> findByResolved(boolean resolved);
    List<Alert> findBySeverity(String severity);
}
