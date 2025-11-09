package tn.airaware.api.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MongoProvider {

    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "AirAwareDB";

    private final MongoClient client;
    private final MongoDatabase database;

    public MongoProvider() {
        this.client = MongoClients.create(CONNECTION_STRING);
        this.database = client.getDatabase(DATABASE_NAME);
    }

    public MongoDatabase getDatabase() {
        return database;
    }
}
