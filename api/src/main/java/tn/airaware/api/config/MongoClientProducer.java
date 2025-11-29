package tn.airaware.api.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.DatabaseType;

@ApplicationScoped
public class MongoClientProducer {

    private final MongoClient client = MongoClients.create("mongodb://localhost:27017");

    @Produces
    @ApiDatabase
    @Database(DatabaseType.DOCUMENT)  // Add this
    public MongoDatabase produceDatabase() {
        return client.getDatabase("AirAwareDB");  // Your API database name
    }
}