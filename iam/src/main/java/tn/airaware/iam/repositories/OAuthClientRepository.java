package tn.airaware.iam.repositories;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;
import tn.airaware.iam.entities.OAuthClient;

import java.util.Optional;
import java.util.List;

/**
 * Repository for OAuth Client management
 * Handles OAuth 2.0 client registration and lookup
 */
@Repository
public interface OAuthClientRepository extends CrudRepository<OAuthClient, String> {

    /**
     * Find OAuth client by client ID
     */
    Optional<OAuthClient> findByClientId(String clientId);

    /**
     * Find OAuth client by organization name
     */
    Optional<OAuthClient> findByOrganizationName(String organizationName);

    /**
     * Find active OAuth clients only
     */
    List<OAuthClient> findByActive(boolean active);
}