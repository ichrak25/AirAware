package tn.airaware.api.services;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service to fetch user emails from the IAM database
 * Used for sending alert notifications to all registered users
 */
@ApplicationScoped
public class UserEmailService {

    private static final Logger LOGGER = Logger.getLogger(UserEmailService.class.getName());

    @Inject
    @ConfigProperty(name = "iam.mongodb.uri", defaultValue = "mongodb://localhost:27017")
    private String mongoUri;

    @Inject
    @ConfigProperty(name = "iam.mongodb.database", defaultValue = "AirAwareIAM")
    private String databaseName;

    private MongoClient mongoClient;
    private MongoDatabase database;

    @PostConstruct
    public void init() {
        try {
            this.mongoClient = MongoClients.create(mongoUri);
            this.database = mongoClient.getDatabase(databaseName);
            LOGGER.info("✅ UserEmailService connected to IAM database: " + databaseName);
        } catch (Exception e) {
            LOGGER.severe("❌ Failed to connect to IAM database: " + e.getMessage());
        }
    }

    @PreDestroy
    public void cleanup() {
        if (mongoClient != null) {
            mongoClient.close();
            LOGGER.info("UserEmailService MongoDB connection closed");
        }
    }

    /**
     * Get all email addresses of activated users
     * @return List of email addresses
     */
    public List<String> getAllUserEmails() {
        List<String> emails = new ArrayList<>();
        
        if (database == null) {
            LOGGER.warning("Database connection not available");
            return emails;
        }

        try {
            MongoCollection<Document> identities = database.getCollection("identities");
            
            // Only get activated accounts with valid emails
            for (Document doc : identities.find(Filters.eq("accountActivated", true))) {
                String email = doc.getString("email");
                if (email != null && !email.trim().isEmpty() && email.contains("@")) {
                    emails.add(email.trim());
                }
            }
            
            LOGGER.info("📧 Found " + emails.size() + " user emails for notifications");
        } catch (Exception e) {
            LOGGER.severe("Failed to fetch user emails: " + e.getMessage());
        }
        
        return emails;
    }

    /**
     * Get emails of users with specific roles (for targeted notifications)
     * @param roleFilter Role bitmask to filter by (null for all users)
     * @return List of email addresses
     */
    public List<String> getUserEmailsByRole(Long roleFilter) {
        List<String> emails = new ArrayList<>();
        
        if (database == null) {
            LOGGER.warning("Database connection not available");
            return emails;
        }

        try {
            MongoCollection<Document> identities = database.getCollection("identities");
            
            for (Document doc : identities.find(Filters.eq("accountActivated", true))) {
                // Check role if filter is provided
                if (roleFilter != null) {
                    Long userRoles = doc.getLong("roles");
                    if (userRoles == null || (userRoles & roleFilter) == 0) {
                        continue; // Skip users without matching role
                    }
                }
                
                String email = doc.getString("email");
                if (email != null && !email.trim().isEmpty() && email.contains("@")) {
                    emails.add(email.trim());
                }
            }
            
            LOGGER.info("📧 Found " + emails.size() + " user emails for role filter: " + roleFilter);
        } catch (Exception e) {
            LOGGER.severe("Failed to fetch user emails by role: " + e.getMessage());
        }
        
        return emails;
    }

    /**
     * Check if the service is connected and operational
     */
    public boolean isConnected() {
        return database != null;
    }
}
