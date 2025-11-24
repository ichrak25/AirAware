package tn.airaware.api.repositories;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;
import tn.airaware.api.entities.Tenant;

import java.util.List;

/**
 * Repository for Tenant (organization) management
 * Used by the API module for sensor ownership
 */
@Repository
public interface TenantRepository extends CrudRepository<Tenant, String> {

    Tenant findByOrganizationName(String organizationName);

    List<Tenant> findByCountry(String country);

    List<Tenant> findByActive(boolean active);
}