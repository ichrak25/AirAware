package tn.cot.airaware.service;

import com.mongodb.client.MongoCollection;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.Document;
import tn.cot.airaware.config.MongoDBConfig;
import tn.cot.airaware.model.User;

@Path("/users")
public class UserResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response createUser(User user) {
        try {
            MongoCollection<Document> collection = new MongoDBConfig().getDatabase().getCollection("users");

            Document doc = new Document()
                    .append("name", user.getFirstName())
                    .append("email", user.getEmail());

            collection.insertOne(doc);

            return Response.status(Response.Status.CREATED)
                           .entity("User inserted successfully!")
                           .build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Error inserting user")
                           .build();
        }
    }
}
