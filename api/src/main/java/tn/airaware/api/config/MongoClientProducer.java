package tn.airaware.api.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class MongoClientProducer {

    private final MongoClient client = MongoClients.create("mongodb://localhost:27017");

    @Produces
    public MongoDatabase produceDatabase() {
        return client.getDatabase("AirAwareDB");
    }
}
