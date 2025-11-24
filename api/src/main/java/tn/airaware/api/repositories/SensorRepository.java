package tn.airaware.api.repositories;

import jakarta.data.repository.Repository;
import jakarta.nosql.mapping.Query;
import tn.airaware.api.entities.Sensor;
import java.util.List;


/**
 * Repository for Sensor entity — handles sensor metadata and lookup.
 */
@Repository
public interface SensorRepository extends jakarta.nosql.mapping.Repository<Sensor, String> {
    List<Sensor> findByStatus(String status);
    List<Sensor> findByTenantOrganizationName(String organizationName);
    @Query("SELECT * FROM sensors")
    List<Sensor> findAll();
}
