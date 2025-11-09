package tn.airaware.api.tests;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests
 * Sets up Testcontainers with MongoDB and REST Assured configuration
 */
@Testcontainers
public abstract class BaseIntegrationTest {

    // MongoDB container - shared across all tests
    @Container
    protected static final MongoDBContainer mongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:7.0")
    ).withExposedPorts(27017);

    // Base URL for API endpoints
    protected static final String BASE_URL = "http://localhost:8080";

    @BeforeAll
    public static void setupClass() {
        // Start MongoDB container
        mongoDBContainer.start();

        // Configure REST Assured
        RestAssured.baseURI = BASE_URL;
        RestAssured.port = 8080;
        RestAssured.basePath = "/api-1.0-SNAPSHOT/api";

        // Set MongoDB connection for the application
        System.setProperty("mongodb.connection.string", mongoDBContainer.getReplicaSetUrl());
        System.setProperty("mongodb.database", "airaware_test");

        System.out.println("✅ MongoDB Test Container started: " + mongoDBContainer.getReplicaSetUrl());
    }

    @BeforeEach
    public void setup() {
        // Reset REST Assured configuration before each test
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured.requestSpecification = null;
    }

    /**
     * Helper method to get the MongoDB connection string
     */
    protected String getMongoConnectionString() {
        return mongoDBContainer.getReplicaSetUrl();
    }

    /**
     * Helper method to create a valid JSON request
     */
    protected io.restassured.specification.RequestSpecification givenJson() {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    /**
     * Helper method to log request/response for debugging
     */
    protected io.restassured.specification.RequestSpecification givenWithLogging() {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .log().all();
    }
}