package tn.airaware.iam.repositories;

import tn.airaware.iam.entities.OAuthClient;

import java.util.List;
import java.util.Optional;

/**
 * OAuth Client Repository Interface
 * Direct MongoDB implementation - no JNoSQL to avoid DocumentTemplate conflicts
 */
public interface OAuthClientRepository {

    Optional<OAuthClient> findByClientId(String clientId);

    Optional<OAuthClient> findByOrganizationName(String organizationName);

    List<OAuthClient> findByActive(boolean active);

    OAuthClient save(OAuthClient client);

    Optional<OAuthClient> findById(String id);

    void delete(OAuthClient client);

    void deleteById(String id);

    boolean existsById(String id);

    long count();
}