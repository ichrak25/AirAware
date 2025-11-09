package tn.airaware.api.repositories;

import jakarta.data.repository.Repository;
import jakarta.nosql.mapping.Query;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.DatabaseType;
import tn.airaware.api.entities.Alert;

import java.util.List;

/**
 * Repository for Alert entity — handles alert history and monitoring.
 */
@Repository
@Database(DatabaseType.DOCUMENT)
public interface AlertRepository extends jakarta.nosql.mapping.Repository<Alert, String> {

    List<Alert> findBySensorId(String sensorId);

    List<Alert> findByResolved(boolean resolved);

    List<Alert> findBySeverity(String severity);
    @Query("SELECT * FROM alert")
    List<Alert> findAll();

    // findAll() is already provided by the parent interface
}