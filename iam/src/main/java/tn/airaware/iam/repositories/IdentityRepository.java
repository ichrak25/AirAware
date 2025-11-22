package tn.airaware.iam.repositories;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;
import tn.airaware.iam.entities.Identity;

import java.util.Optional;

@Repository
public interface IdentityRepository extends CrudRepository<Identity, String> {
    // findById is already provided by CrudRepository - no need to redeclare
    Optional<Identity> findByEmail(String email);
    Optional<Identity> findByUsername(String username);
    // findAll() is already provided by CrudRepository
}