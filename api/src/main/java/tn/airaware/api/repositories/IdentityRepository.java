package tn.airaware.api.repositories;

import jakarta.nosql.mapping.Repository;
import tn.airaware.api.entities.Identity;

/**
 * Repository for Identity entity — manages user and authentication data.
 */

public interface IdentityRepository extends Repository<Identity, String> {
    Identity findByUsername(String username);
    Identity findByEmail(String email);
}
