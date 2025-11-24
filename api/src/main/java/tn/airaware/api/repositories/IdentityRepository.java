package tn.airaware.api.repositories;

import jakarta.nosql.mapping.Repository;
import tn.airaware.core.entities.Identity;

/**
 * Identity Repository for API module
 * Uses unified Identity from core package
 */
public interface IdentityRepository extends Repository<Identity, String> {
    Identity findByUsername(String username);
    Identity findByEmail(String email);
}