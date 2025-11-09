package tn.airaware.api.repositories;

import jakarta.nosql.mapping.Query;
import jakarta.nosql.mapping.Repository;
import tn.airaware.api.entities.Tenant;

import java.util.List;

/**
 * Repository for Tenant entity — manages organizations/owners.
 */
@jakarta.data.repository.Repository
public interface TenantRepository extends Repository<Tenant, String> {
    List<Tenant> findByCountry(String country);
    Tenant findByOrganizationName(String organizationName);

    @Query("SELECT * FROM tenant")
    List<Tenant> findAll();
}
