package tn.airaware.api.services;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tn.airaware.api.entities.Alert;
import tn.airaware.api.entities.PushSubscription;
import tn.airaware.api.repositories.PushSubscriptionRepository;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Service for sending Web Push notifications.
 * Implements the Web Push protocol with VAPID authentication.
 * 
 * This allows sending notifications to users even when the app is closed,
 * on both desktop (PC) and mobile devices.
 */
@ApplicationScoped
public class WebPushService {

    private static final Logger LOGGER = Logger.getLogger(WebPushService.class.getName());

    // VAPID keys - should be set via environment variables in production
    private String vapidPublicKey;
    private String vapidPrivateKey;
    private String vapidSubject;

    // EC key pair for signing
    private ECPrivateKey privateKey;
    private ECPublicKey publicKey;

    @Inject
    private PushSubscriptionRepository subscriptionRepository;

    @PostConstruct
    public void init() {
        LOGGER.info("🔔 Initializing Web Push Service...");
        
        // Load VAPID keys from environment or generate defaults for development
        vapidPublicKey = getEnv("VAPID_PUBLIC_KEY", 
            "BEl62iUYgUivxIkv69yViEuiBIa-Ib9-SkvMeAtA3LFgDzkrxZJjSgSnfckjBJuBkr3qBUYIHBQFLXYp5Nksh8U");
        vapidPrivateKey = getEnv("VAPID_PRIVATE_KEY", 
            "UUxI4O8-FbRouAevSmBQ6o18hgE4nSG3qwvJTfKc-ls");
        vapidSubject = getEnv("VAPID_SUBJECT", "mailto:alerts@airaware.tn");

        try {
            // Parse the VAPID keys into EC key objects
            initializeKeys();
            LOGGER.info("✅ Web Push Service initialized successfully");
            LOGGER.info("📧 VAPID Subject: " + vapidSubject);
        } catch (Exception e) {
            LOGGER.severe("❌ Failed to initialize Web Push Service: " + e.getMessage());
        }
    }

    private void initializeKeys() throws Exception {
        // Decode the base64url encoded keys
        byte[] publicKeyBytes = base64UrlDecode(vapidPublicKey);
        byte[] privateKeyBytes = base64UrlDecode(vapidPrivateKey);

        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        
        // The public key should be 65 bytes (uncompressed point format)
        ECParameterSpec ecSpec = getP256Params();
        
        // Create public key from uncompressed point
        ECPoint point = decodePoint(publicKeyBytes);
        ECPublicKeySpec pubKeySpec = new ECPublicKeySpec(point, ecSpec);
        publicKey = (ECPublicKey) keyFactory.generatePublic(pubKeySpec);
        
        // Create private key
        ECPrivateKeySpec privKeySpec = new ECPrivateKeySpec(
            new java.math.BigInteger(1, privateKeyBytes), ecSpec);
        privateKey = (ECPrivateKey) keyFactory.generatePrivate(privKeySpec);
    }

    /**
     * Get the VAPID public key for client subscription
     */
    public String getVapidPublicKey() {
        return vapidPublicKey;
    }

    /**
     * Save a new push subscription
     */
    public PushSubscription saveSubscription(PushSubscription subscription) {
        // Check if subscription with same endpoint exists
        Optional<PushSubscription> existing = subscriptionRepository.findByEndpoint(subscription.getEndpoint());
        
        if (existing.isPresent()) {
            // Update existing subscription
            PushSubscription existingSub = existing.get();
            existingSub.setP256dh(subscription.getP256dh());
            existingSub.setAuth(subscription.getAuth());
            existingSub.setUserAgent(subscription.getUserAgent());
            existingSub.setPlatform(subscription.getPlatform());
            existingSub.setActive(true);
            existingSub.setFailureCount(0); // Reset failures on re-subscribe
            return subscriptionRepository.save(existingSub);
        }
        
        // Generate ID and save new subscription
        subscription.setId(UUID.randomUUID().toString());
        subscription.setCreatedAt(LocalDateTime.now());
        return subscriptionRepository.save(subscription);
    }

    /**
     * Remove a subscription by endpoint
     */
    public void removeSubscription(String endpoint) {
        subscriptionRepository.findByEndpoint(endpoint).ifPresent(sub -> {
            sub.setActive(false);
            subscriptionRepository.save(sub);
        });
    }

    /**
     * Send push notification to all active subscribers
     */
    public void sendPushToAll(Alert alert) {
        List<PushSubscription> subscriptions = subscriptionRepository.findByActiveTrue();
        LOGGER.info("📤 Sending push notification to " + subscriptions.size() + " subscribers");

        String payload = createAlertPayload(alert);

        // Send asynchronously to not block
        CompletableFuture.runAsync(() -> {
            int success = 0;
            int failed = 0;

            for (PushSubscription sub : subscriptions) {
                try {
                    boolean sent = sendPushNotification(sub, payload);
                    if (sent) {
                        sub.incrementSuccessCount();
                        success++;
                    } else {
                        sub.incrementFailureCount();
                        failed++;
                    }
                    subscriptionRepository.save(sub);
                } catch (Exception e) {
                    LOGGER.warning("Failed to send push to " + sub.getId() + ": " + e.getMessage());
                    sub.incrementFailureCount();
                    subscriptionRepository.save(sub);
                    failed++;
                }
            }

            LOGGER.info("📊 Push results: " + success + " sent, " + failed + " failed");
        });
    }

    /**
     * Send a test notification to all subscribers
     */
    public int sendTestNotification() {
        List<PushSubscription> subscriptions = subscriptionRepository.findByActiveTrue();
        LOGGER.info("🧪 Sending test notification to " + subscriptions.size() + " subscribers");

        String payload = """
            {
                "title": "🧪 AirAware Test",
                "body": "Push notifications are working! You'll receive alerts even when the app is closed.",
                "icon": "/pwa-192x192.png",
                "badge": "/pwa-72x72.png",
                "tag": "test-notification",
                "severity": "INFO",
                "url": "/settings",
                "timestamp": %d
            }
            """.formatted(System.currentTimeMillis());

        int sent = 0;
        for (PushSubscription sub : subscriptions) {
            try {
                if (sendPushNotification(sub, payload)) {
                    sent++;
                }
            } catch (Exception e) {
                LOGGER.warning("Test push failed for " + sub.getId() + ": " + e.getMessage());
            }
        }

        return sent;
    }

    /**
     * Send a push notification to a specific subscription
     */
    private boolean sendPushNotification(PushSubscription subscription, String payload) throws Exception {
        if (subscription.getEndpoint() == null || subscription.getP256dh() == null || subscription.getAuth() == null) {
            LOGGER.warning("Invalid subscription data for " + subscription.getId());
            return false;
        }

        try {
            // Encrypt the payload
            byte[] encryptedPayload = encryptPayload(
                payload.getBytes(StandardCharsets.UTF_8),
                subscription.getP256dh(),
                subscription.getAuth()
            );

            // Create VAPID JWT token
            String vapidToken = createVapidToken(subscription.getEndpoint());

            // Send HTTP POST to push service
            URL url = new URL(subscription.getEndpoint());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            // Set headers
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setRequestProperty("Content-Encoding", "aes128gcm");
            conn.setRequestProperty("TTL", "86400"); // 24 hours
            conn.setRequestProperty("Urgency", "high");
            conn.setRequestProperty("Authorization", "vapid t=" + vapidToken + ", k=" + vapidPublicKey);

            // Send encrypted payload
            try (OutputStream os = conn.getOutputStream()) {
                os.write(encryptedPayload);
            }

            int responseCode = conn.getResponseCode();
            
            if (responseCode == 201 || responseCode == 200) {
                LOGGER.fine("✅ Push sent successfully to " + subscription.getId());
                return true;
            } else if (responseCode == 404 || responseCode == 410) {
                // Subscription is no longer valid
                LOGGER.info("⚠️ Subscription expired/invalid: " + subscription.getId());
                subscription.setActive(false);
                return false;
            } else {
                LOGGER.warning("❌ Push failed with code " + responseCode + " for " + subscription.getId());
                return false;
            }

        } catch (Exception e) {
            LOGGER.warning("❌ Push exception for " + subscription.getId() + ": " + e.getMessage());
            throw e;
        }
    }

    /**
     * Create the JSON payload for an alert notification
     */
    private String createAlertPayload(Alert alert) {
        String emoji = switch (alert.getSeverity()) {
            case "CRITICAL" -> "🚨";
            case "WARNING" -> "⚠️";
            default -> "ℹ️";
        };

        return """
            {
                "title": "%s AirAware Alert: %s",
                "body": "%s",
                "icon": "/pwa-192x192.png",
                "badge": "/pwa-72x72.png",
                "tag": "airaware-%s",
                "severity": "%s",
                "alertId": "%s",
                "sensorId": "%s",
                "url": "/alerts",
                "timestamp": %d
            }
            """.formatted(
                emoji,
                alert.getType(),
                alert.getMessage().replace("\"", "\\\""),
                alert.getSeverity().toLowerCase(),
                alert.getSeverity(),
                alert.getId() != null ? alert.getId() : "",
                alert.getSensorId() != null ? alert.getSensorId() : "",
                System.currentTimeMillis()
            );
    }

    /**
     * Get subscription statistics
     */
    public Map<String, Object> getSubscriptionStats() {
        // Get all subscriptions and count manually since count() may not be available
        List<PushSubscription> allSubscriptions = new ArrayList<>();
        subscriptionRepository.findAll().forEach(allSubscriptions::add);
        
        long total = allSubscriptions.size();
        long active = allSubscriptions.stream().filter(PushSubscription::isActive).count();
        
        return Map.of(
            "total", total,
            "active", active,
            "inactive", total - active
        );
    }

    // ==================== Encryption Methods ====================

    /**
     * Encrypt payload using AES-128-GCM with ECDH key agreement
     * This is required by the Web Push protocol
     */
    private byte[] encryptPayload(byte[] payload, String clientPublicKeyBase64, String authSecretBase64) throws Exception {
        // Decode client public key and auth secret
        byte[] clientPublicKeyBytes = base64UrlDecode(clientPublicKeyBase64);
        byte[] authSecret = base64UrlDecode(authSecretBase64);

        // Generate ephemeral ECDH key pair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());
        KeyPair ephemeralKeyPair = keyGen.generateKeyPair();
        ECPublicKey ephemeralPublicKey = (ECPublicKey) ephemeralKeyPair.getPublic();
        ECPrivateKey ephemeralPrivateKey = (ECPrivateKey) ephemeralKeyPair.getPrivate();

        // Decode client public key
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        ECPoint clientPoint = decodePoint(clientPublicKeyBytes);
        ECPublicKeySpec clientKeySpec = new ECPublicKeySpec(clientPoint, getP256Params());
        ECPublicKey clientPublicKey = (ECPublicKey) keyFactory.generatePublic(clientKeySpec);

        // Perform ECDH key agreement
        KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
        keyAgreement.init(ephemeralPrivateKey);
        keyAgreement.doPhase(clientPublicKey, true);
        byte[] sharedSecret = keyAgreement.generateSecret();

        // Derive encryption key using HKDF
        byte[] ephemeralPublicKeyBytes = encodePoint(ephemeralPublicKey.getW());
        
        // PRK = HKDF-Extract(auth_secret, shared_secret)
        byte[] prk = hkdfExtract(authSecret, sharedSecret);
        
        // info = "WebPush: info" || 0x00 || client_public_key || server_public_key
        byte[] info = createInfo("WebPush: info", clientPublicKeyBytes, ephemeralPublicKeyBytes);
        byte[] ikm = hkdfExpand(prk, info, 32);
        
        // Derive content encryption key and nonce
        byte[] cekInfo = "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.UTF_8);
        byte[] nonceInfo = "Content-Encoding: nonce\0".getBytes(StandardCharsets.UTF_8);
        
        byte[] prk2 = hkdfExtract(new byte[0], ikm);
        byte[] cek = hkdfExpand(prk2, cekInfo, 16);
        byte[] nonce = hkdfExpand(prk2, nonceInfo, 12);

        // Add padding and delimiter
        byte[] paddedPayload = new byte[payload.length + 2];
        paddedPayload[0] = 2; // Padding delimiter
        paddedPayload[1] = 0; // No padding
        System.arraycopy(payload, 0, paddedPayload, 2, payload.length);

        // Encrypt with AES-128-GCM
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, nonce);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cek, "AES"), gcmSpec);
        byte[] ciphertext = cipher.doFinal(paddedPayload);

        // Build the encrypted message with header
        // salt (16) + rs (4) + idlen (1) + keyid (65) + ciphertext
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        
        ByteBuffer result = ByteBuffer.allocate(16 + 4 + 1 + 65 + ciphertext.length);
        result.put(salt);
        result.putInt(4096); // Record size
        result.put((byte) 65); // Key ID length
        result.put(ephemeralPublicKeyBytes);
        result.put(ciphertext);

        return result.array();
    }

    /**
     * Create VAPID JWT token for authorization
     */
    private String createVapidToken(String endpoint) throws Exception {
        // Extract audience from endpoint URL
        URL url = new URL(endpoint);
        String audience = url.getProtocol() + "://" + url.getHost();

        // Create JWT header
        String header = base64UrlEncode("{\"typ\":\"JWT\",\"alg\":\"ES256\"}".getBytes());

        // Create JWT payload
        long now = System.currentTimeMillis() / 1000;
        String payloadJson = String.format(
            "{\"aud\":\"%s\",\"exp\":%d,\"sub\":\"%s\"}",
            audience, now + 86400, vapidSubject
        );
        String payload = base64UrlEncode(payloadJson.getBytes());

        // Sign with ECDSA
        String signatureInput = header + "." + payload;
        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign(privateKey);
        signature.update(signatureInput.getBytes(StandardCharsets.UTF_8));
        byte[] sig = signature.sign();
        
        // Convert DER signature to raw format
        byte[] rawSig = derToRaw(sig);
        String signatureStr = base64UrlEncode(rawSig);

        return signatureInput + "." + signatureStr;
    }

    // ==================== Crypto Helper Methods ====================

    private ECParameterSpec getP256Params() throws Exception {
        AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
        parameters.init(new ECGenParameterSpec("secp256r1"));
        return parameters.getParameterSpec(ECParameterSpec.class);
    }

    private ECPoint decodePoint(byte[] data) {
        if (data.length != 65 || data[0] != 0x04) {
            throw new IllegalArgumentException("Invalid uncompressed point");
        }
        byte[] x = new byte[32];
        byte[] y = new byte[32];
        System.arraycopy(data, 1, x, 0, 32);
        System.arraycopy(data, 33, y, 0, 32);
        return new ECPoint(new java.math.BigInteger(1, x), new java.math.BigInteger(1, y));
    }

    private byte[] encodePoint(ECPoint point) {
        byte[] x = point.getAffineX().toByteArray();
        byte[] y = point.getAffineY().toByteArray();
        
        byte[] result = new byte[65];
        result[0] = 0x04;
        
        // Ensure 32 bytes for each coordinate
        int xOffset = x.length > 32 ? x.length - 32 : 0;
        int xLen = Math.min(32, x.length);
        System.arraycopy(x, xOffset, result, 1 + (32 - xLen), xLen);
        
        int yOffset = y.length > 32 ? y.length - 32 : 0;
        int yLen = Math.min(32, y.length);
        System.arraycopy(y, yOffset, result, 33 + (32 - yLen), yLen);
        
        return result;
    }

    private byte[] hkdfExtract(byte[] salt, byte[] ikm) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(salt.length > 0 ? salt : new byte[32], "HmacSHA256"));
        return mac.doFinal(ikm);
    }

    private byte[] hkdfExpand(byte[] prk, byte[] info, int length) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(prk, "HmacSHA256"));
        
        byte[] result = new byte[length];
        byte[] t = new byte[0];
        int offset = 0;
        int i = 1;
        
        while (offset < length) {
            mac.update(t);
            mac.update(info);
            mac.update((byte) i++);
            t = mac.doFinal();
            System.arraycopy(t, 0, result, offset, Math.min(t.length, length - offset));
            offset += t.length;
        }
        
        return result;
    }

    private byte[] createInfo(String type, byte[] clientKey, byte[] serverKey) {
        byte[] typeBytes = type.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(typeBytes.length + 1 + clientKey.length + serverKey.length);
        buffer.put(typeBytes);
        buffer.put((byte) 0);
        buffer.put(clientKey);
        buffer.put(serverKey);
        return buffer.array();
    }

    private byte[] derToRaw(byte[] der) {
        // Convert DER-encoded ECDSA signature to raw R || S format
        int rLen = der[3];
        int rOffset = 4 + (der[4] == 0 ? 1 : 0);
        rLen = rLen - (der[4] == 0 ? 1 : 0);
        
        int sOffset = 4 + der[3] + 2;
        int sLen = der[sOffset - 1];
        sOffset += (der[sOffset] == 0 ? 1 : 0);
        sLen = sLen - (der[sOffset - rLen - 2] == 0 ? 1 : 0);
        
        byte[] raw = new byte[64];
        System.arraycopy(der, rOffset, raw, 32 - rLen, rLen);
        System.arraycopy(der, sOffset, raw, 64 - sLen, sLen);
        return raw;
    }

    private String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private byte[] base64UrlDecode(String data) {
        return Base64.getUrlDecoder().decode(data);
    }

    private String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }
}
