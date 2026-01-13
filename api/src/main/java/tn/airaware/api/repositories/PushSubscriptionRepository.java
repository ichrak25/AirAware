package tn.airaware.api.repositories;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;
import tn.airaware.api.entities.PushSubscription;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing Web Push subscriptions.
 */
@Repository
public interface PushSubscriptionRepository extends CrudRepository<PushSubscription, String> {

    /**
     * Find subscription by endpoint URL
     */
    Optional<PushSubscription> findByEndpoint(String endpoint);

    /**
     * Find all subscriptions for a user
     */
    List<PushSubscription> findByUserId(String userId);

    /**
     * Find all active subscriptions
     */
    List<PushSubscription> findByActiveTrue();

    /**
     * Find active subscriptions for a specific user
     */
    List<PushSubscription> findByUserIdAndActiveTrue(String userId);
}
