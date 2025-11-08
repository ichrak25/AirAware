package tn.airaware.api.repositories;

import jakarta.data.repository.Repository;
import tn.airaware.api.entities.Identity;
import tn.airaware.api.entities.Sensor;
import java.util.List;

/**
 * Repository for Sensor entity — handles sensor metadata and lookup.
 */
@Repository
public interface SensorRepository extends jakarta.nosql.mapping.Repository<Identity, String> {
    List<Sensor> findByStatus(String status);
    List<Sensor> findByTenantOrganizationName(String organizationName);
}
