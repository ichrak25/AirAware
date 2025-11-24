package tn.airaware.iam.repositories;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;
import tn.airaware.core.entities.Identity;

import java.util.Optional;

/**
 * Identity Repository - manages user accounts
 * Now uses the unified Identity from core package
 */
@Repository
public interface IdentityRepository extends CrudRepository<Identity, String> {

    Optional<Identity> findByEmail(String email);

    Optional<Identity> findByUsername(String username);
}