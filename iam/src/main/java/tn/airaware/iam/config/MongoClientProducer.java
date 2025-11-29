package tn.airaware.iam.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.util.logging.Logger;

/**
 * MongoDB Producer for IAM Module
 * Produces IAM-specific database connections with @IamDatabase qualifier
 * Uses direct MongoDB - no JNoSQL to avoid DocumentTemplate conflicts
 */
@ApplicationScoped
public class MongoClientProducer {

    private static final Logger LOGGER = Logger.getLogger(MongoClientProducer.class.getName());
    private static final String DATABASE_NAME = "AirAwareIAM";
    private static final String MONGODB_URI = "mongodb://localhost:27017";

    private MongoClient mongoClient;

    @PostConstruct
    public void init() {
        LOGGER.info("Initializing MongoDB client for IAM module");
        this.mongoClient = MongoClients.create(MONGODB_URI);
    }

    @Produces
    @ApplicationScoped
    @IamDatabase
    public MongoDatabase produceMongoDatabase() {
        LOGGER.info("Producing MongoDB database: " + DATABASE_NAME);
        return mongoClient.getDatabase(DATABASE_NAME);
    }

    @PreDestroy
    public void cleanup() {
        if (mongoClient != null) {
            LOGGER.info("Closing MongoDB client for IAM module");
            mongoClient.close();
        }
    }
}