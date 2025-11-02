package tn.cot.airaware.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import java.util.concurrent.TimeUnit;

@Singleton
@Startup
public class MongoDBConfig {

    private MongoClient mongoClient;
    private MongoDatabase database;

    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "airaware";

    @PostConstruct
    public void init() {
        ConnectionString connString = new ConnectionString(CONNECTION_STRING);
        
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connString)
                .applyToConnectionPoolSettings(builder -> 
                    builder.maxConnectionIdleTime(60000, TimeUnit.MILLISECONDS)
                           .maxSize(100)
                           .minSize(10))
                .applyToSocketSettings(builder -> 
                    builder.connectTimeout(10000, TimeUnit.MILLISECONDS)
                           .readTimeout(10000, TimeUnit.MILLISECONDS))
                .build();

        mongoClient = MongoClients.create(settings);
        database = mongoClient.getDatabase(DATABASE_NAME);
        
        System.out.println("MongoDB connection established successfully");
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    @PreDestroy
    public void cleanup() {
        if (mongoClient != null) {
            mongoClient.close();
            System.out.println("MongoDB connection closed");
        }
    }
}