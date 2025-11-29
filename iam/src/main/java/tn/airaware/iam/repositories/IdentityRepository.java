package tn.airaware.iam.repositories;

import tn.airaware.core.entities.Identity;

import java.util.Optional;

/**
 * Identity Repository Interface
 * Direct MongoDB implementation - no JNoSQL to avoid DocumentTemplate conflicts
 */
public interface IdentityRepository {

    Optional<Identity> findByEmail(String email);

    Optional<Identity> findByUsername(String username);

    Identity save(Identity identity);

    Optional<Identity> findById(String id);

    void delete(Identity identity);

    void deleteById(String id);

    boolean existsById(String id);

    long count();
}