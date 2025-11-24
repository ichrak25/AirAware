package tn.airaware.iam.config;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import tn.airaware.iam.entities.OAuthClient;
import tn.airaware.iam.enums.Role;
import tn.airaware.iam.repositories.OAuthClientRepository;
import tn.airaware.core.security.Argon2Utils;

import java.util.logging.Logger;

/**
 * Minimal Database Initializer - Fail-Safe for Critical Components
 * Only creates OAuth clients if they don't exist
 * 
 * This bean runs on every application startup and ensures that
 * critical OAuth clients exist. It's a fail-safe mechanism in case
 * the MongoDB initialization script wasn't run.
 */
@Startup
@Singleton
public class DatabaseInitializer {

    private static final Logger LOGGER = Logger.getLogger(DatabaseInitializer.class.getName());

    @Inject
    private OAuthClientRepository oauthClientRepository;

    @Inject
    private Argon2Utils argon2Utils;

    @PostConstruct
    public void init() {
        LOGGER.info("===========================================");
        LOGGER.info("🔍 AirAware Database Initialization Check");
        LOGGER.info("===========================================");
        
        try {
            ensureOAuthClientsExist();
            LOGGER.info("✅ Database initialization check completed");
        } catch (Exception e) {
            LOGGER.severe("❌ Database initialization check failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        LOGGER.info("===========================================");
    }

    /**
     * Ensure critical OAuth clients exist
     * This is a fail-safe in case the MongoDB script wasn't run
     */
    private void ensureOAuthClientsExist() {
        // Check Web Client
        if (oauthClientRepository.findByClientId("airaware-web-client").isEmpty()) {
            LOGGER.warning("⚠️  OAuth client 'airaware-web-client' not found. Creating...");
            createWebClient();
        } else {
            LOGGER.info("✅ OAuth client 'airaware-web-client' exists");
        }

        // Check Mobile Client
        if (oauthClientRepository.findByClientId("airaware-mobile-app").isEmpty()) {
            LOGGER.warning("⚠️  OAuth client 'airaware-mobile-app' not found. Creating...");
            createMobileClient();
        } else {
            LOGGER.info("✅ OAuth client 'airaware-mobile-app' exists");
        }

        // Check API Client
        if (oauthClientRepository.findByClientId("airaware-api-client").isEmpty()) {
            LOGGER.warning("⚠️  OAuth client 'airaware-api-client' not found. Creating...");
            createApiClient();
        } else {
            LOGGER.info("✅ OAuth client 'airaware-api-client' exists");
        }
    }

    /**
     * Create Web OAuth Client
     */
    private void createWebClient() {
        OAuthClient webClient = new OAuthClient();
        webClient.setClientId("airaware-web-client");
        webClient.setClientSecret(argon2Utils.hash("airaware-web-secret-2025".toCharArray()));
        webClient.setRedirectUri("http://localhost:3000/callback");
        webClient.setOrganizationName("AirAware Platform");
        
        // Allow multiple roles (SYSTEM_ADMIN, TENANT_ADMIN, SENSOR_OPERATOR, DATA_ANALYST, ALERT_MANAGER, VIEWER)
        webClient.setAllowedRoles(Role.combine(
            Role.SYSTEM_ADMIN,
            Role.TENANT_ADMIN,
            Role.SENSOR_OPERATOR,
            Role.DATA_ANALYST,
            Role.ALERT_MANAGER,
            Role.VIEWER
        ));
        
        webClient.setRequiredScopes("sensor:read sensor:write alert:read alert:write dashboard:view");
        webClient.setSupportedGrantTypes("authorization_code,refresh_token");
        webClient.setActive(true);
        
        oauthClientRepository.save(webClient);
        LOGGER.info("✅ Created OAuth client: airaware-web-client");
    }

    /**
     * Create Mobile OAuth Client
     */
    private void createMobileClient() {
        OAuthClient mobileClient = new OAuthClient();
        mobileClient.setClientId("airaware-mobile-app");
        mobileClient.setClientSecret(argon2Utils.hash("airaware-mobile-secret-2025".toCharArray()));
        mobileClient.setRedirectUri("airaware://callback");
        mobileClient.setOrganizationName("AirAware Mobile");
        
        // Allow limited roles (VIEWER, DATA_ANALYST)
        mobileClient.setAllowedRoles(Role.combine(Role.VIEWER, Role.DATA_ANALYST));
        
        mobileClient.setRequiredScopes("sensor:read alert:read dashboard:view");
        mobileClient.setSupportedGrantTypes("authorization_code,refresh_token");
        mobileClient.setActive(true);
        
        oauthClientRepository.save(mobileClient);
        LOGGER.info("✅ Created OAuth client: airaware-mobile-app");
    }

    /**
     * Create API OAuth Client
     */
    private void createApiClient() {
        OAuthClient apiClient = new OAuthClient();
        apiClient.setClientId("airaware-api-client");
        apiClient.setClientSecret(argon2Utils.hash("airaware-api-secret-2025".toCharArray()));
        apiClient.setRedirectUri("http://localhost:8080/api/callback");
        apiClient.setOrganizationName("AirAware API");
        
        // API client role
        apiClient.setAllowedRoles(Role.API_CLIENT.getValue());
        
        apiClient.setRequiredScopes("sensor:read sensor:write alert:read");
        apiClient.setSupportedGrantTypes("authorization_code,refresh_token");
        apiClient.setActive(true);
        
        oauthClientRepository.save(apiClient);
        LOGGER.info("✅ Created OAuth client: airaware-api-client");
    }
}