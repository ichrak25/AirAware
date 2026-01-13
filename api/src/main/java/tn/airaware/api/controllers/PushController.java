package tn.airaware.api.controllers;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tn.airaware.api.entities.PushSubscription;
import tn.airaware.api.services.WebPushService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.logging.Logger;

/**
 * REST Controller for Web Push notification subscriptions.
 * Handles subscription management and push notification sending.
 */
@Path("/push")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PushController {

    private static final Logger LOGGER = Logger.getLogger(PushController.class.getName());

    @Inject
    private WebPushService webPushService;

    /**
     * Get the VAPID public key for push subscription
     * Client needs this to subscribe to push notifications
     */
    @GET
    @Path("/vapid-public-key")
    public Response getVapidPublicKey() {
        try {
            String publicKey = webPushService.getVapidPublicKey();
            return Response.ok(Map.of(
                "publicKey", publicKey,
                "success", true
            )).build();
        } catch (Exception e) {
            LOGGER.severe("Failed to get VAPID public key: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get VAPID key", "success", false))
                    .build();
        }
    }

    /**
     * Subscribe to push notifications
     * Stores the subscription for later use
     */
    @POST
    @Path("/subscribe")
    public Response subscribe(SubscriptionRequest request) {
        try {
            LOGGER.info("📱 New push subscription request received");

            if (request.subscription == null || request.subscription.endpoint == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Invalid subscription data", "success", false))
                        .build();
            }

            // Create subscription entity
            PushSubscription subscription = new PushSubscription();
            subscription.setEndpoint(request.subscription.endpoint);
            
            if (request.subscription.keys != null) {
                subscription.setP256dh(request.subscription.keys.p256dh);
                subscription.setAuth(request.subscription.keys.auth);
            }
            
            subscription.setUserAgent(request.userAgent);
            subscription.setPlatform(request.platform);
            subscription.setCreatedAt(LocalDateTime.now());
            subscription.setActive(true);

            // Save subscription
            PushSubscription saved = webPushService.saveSubscription(subscription);

            LOGGER.info("✅ Push subscription saved: " + saved.getId());

            return Response.ok(Map.of(
                "success", true,
                "message", "Subscription saved successfully",
                "subscriptionId", saved.getId()
            )).build();

        } catch (Exception e) {
            LOGGER.severe("Failed to save push subscription: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to save subscription", "success", false))
                    .build();
        }
    }

    /**
     * Unsubscribe from push notifications
     */
    @POST
    @Path("/unsubscribe")
    public Response unsubscribe(UnsubscribeRequest request) {
        try {
            LOGGER.info("📱 Push unsubscribe request received");

            if (request.endpoint == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Endpoint required", "success", false))
                        .build();
            }

            webPushService.removeSubscription(request.endpoint);

            LOGGER.info("✅ Push subscription removed");

            return Response.ok(Map.of(
                "success", true,
                "message", "Subscription removed successfully"
            )).build();

        } catch (Exception e) {
            LOGGER.severe("Failed to remove push subscription: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to remove subscription", "success", false))
                    .build();
        }
    }

    /**
     * Send a test push notification
     */
    @POST
    @Path("/test")
    public Response sendTestPush() {
        try {
            LOGGER.info("🧪 Test push notification requested");

            int sent = webPushService.sendTestNotification();

            return Response.ok(Map.of(
                "success", true,
                "message", "Test notification sent to " + sent + " subscribers",
                "subscriberCount", sent
            )).build();

        } catch (Exception e) {
            LOGGER.severe("Failed to send test push: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to send test notification: " + e.getMessage(), "success", false))
                    .build();
        }
    }

    /**
     * Simple GET endpoint to test push (easier to test from browser)
     */
    @GET
    @Path("/test")
    public Response sendTestPushGet() {
        return sendTestPush();
    }

    /**
     * Get subscription statistics
     */
    @GET
    @Path("/stats")
    public Response getStats() {
        try {
            Map<String, Object> stats = webPushService.getSubscriptionStats();
            return Response.ok(stats).build();
        } catch (Exception e) {
            LOGGER.severe("Failed to get push stats: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Failed to get stats"))
                    .build();
        }
    }

    // ==================== Request DTOs ====================

    public static class SubscriptionRequest {
        public SubscriptionData subscription;
        public String userAgent;
        public String platform;
        public String timestamp;
    }

    public static class SubscriptionData {
        public String endpoint;
        public Long expirationTime;
        public SubscriptionKeys keys;
    }

    public static class SubscriptionKeys {
        public String p256dh;
        public String auth;
    }

    public static class UnsubscribeRequest {
        public String endpoint;
    }
}
