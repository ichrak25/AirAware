package tn.airaware.api.tests.controllers;

import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import tn.airaware.api.tests.BaseIntegrationTest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for ReadingController
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReadingControllerTest extends BaseIntegrationTest {

    private static String createdReadingId;
    private static final String SENSOR_ID = "test-sensor-reading-001";

    @Test
    @Order(1)
    @DisplayName("Should create a new reading")
    public void testCreateReading() {
        Map<String, Object> reading = createReadingPayload(SENSOR_ID, 22.5, 45.0, 450.0, 120.0, 12.0, 18.0);

        Response response = givenJson()
                .body(reading)
                .when()
                .post("/readings")
                .then()
                .statusCode(201)
                .body("sensorId", equalTo(SENSOR_ID))
                .body("temperature", equalTo(22.5f))
                .body("humidity", equalTo(45.0f))
                .body("co2", equalTo(450.0f))
                .body("voc", equalTo(120.0f))
                .body("pm25", equalTo(12.0f))
                .body("pm10", equalTo(18.0f))
                .body("id", notNullValue())
                .body("timestamp", notNullValue())
                .extract()
                .response();

        createdReadingId = response.jsonPath().getString("id");
        System.out.println("✅ Created reading with ID: " + createdReadingId);
    }

    @Test
    @Order(2)
    @DisplayName("Should fail to create reading without sensor ID")
    public void testCreateReadingWithoutSensorId() {
        Map<String, Object> invalidReading = new HashMap<>();
        invalidReading.put("temperature", 22.5);
        invalidReading.put("humidity", 45.0);

        givenJson()
                .body(invalidReading)
                .when()
                .post("/readings")
                .then()
                .statusCode(400)
                .body("error", containsString("Sensor ID is required"));
    }

    @Test
    @Order(3)
    @DisplayName("Should fail to create reading with invalid values")
    public void testCreateReadingWithInvalidValues() {
        Map<String, Object> invalidReading = createReadingPayload(SENSOR_ID, -100.0, 150.0, 450.0, 120.0, 12.0, 18.0);

        givenJson()
                .body(invalidReading)
                .when()
                .post("/readings")
                .then()
                .statusCode(400)
                .body("error", containsString("Invalid sensor data values"));
    }

    @Test
    @Order(4)
    @DisplayName("Should retrieve all readings")
    public void testGetAllReadings() {
        givenJson()
                .when()
                .get("/readings")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)))
                .body("[0].sensorId", notNullValue());
    }

    @Test
    @Order(5)
    @DisplayName("Should retrieve reading by ID")
    public void testGetReadingById() {
        givenJson()
                .when()
                .get("/readings/" + createdReadingId)
                .then()
                .statusCode(200)
                .body("id", equalTo(createdReadingId))
                .body("sensorId", equalTo(SENSOR_ID))
                .body("temperature", equalTo(22.5f));
    }

    @Test
    @Order(6)
    @DisplayName("Should return 404 for non-existent reading")
    public void testGetNonExistentReading() {
        givenJson()
                .when()
                .get("/readings/non-existent-id")
                .then()
                .statusCode(404)
                .body("error", containsString("not found"));
    }

    @Test
    @Order(7)
    @DisplayName("Should retrieve readings by sensor")
    public void testGetReadingsBySensor() {
        // Create multiple readings for the same sensor
        createReading(SENSOR_ID, 23.0, 46.0, 460.0, 125.0, 13.0, 19.0);
        createReading(SENSOR_ID, 23.5, 47.0, 470.0, 130.0, 14.0, 20.0);

        givenJson()
                .when()
                .get("/readings/sensor/" + SENSOR_ID)
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(3)))
                .body("[0].sensorId", equalTo(SENSOR_ID));
    }

    @Test
    @Order(8)
    @DisplayName("Should retrieve latest reading for sensor")
    public void testGetLatestReadingBySensor() {
        givenJson()
                .when()
                .get("/readings/sensor/" + SENSOR_ID + "/latest")
                .then()
                .statusCode(200)
                .body("sensorId", equalTo(SENSOR_ID))
                .body("timestamp", notNullValue());
    }

    @Test
    @Order(9)
    @DisplayName("Should retrieve recent readings")
    public void testGetRecentReadings() {
        givenJson()
                .queryParam("hours", 24)
                .when()
                .get("/readings/recent")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)));
    }

    @Test
    @Order(10)
    @DisplayName("Should fail with invalid hours parameter")
    public void testGetRecentReadingsWithInvalidHours() {
        givenJson()
                .queryParam("hours", 1000)
                .when()
                .get("/readings/recent")
                .then()
                .statusCode(400)
                .body("error", containsString("must be between 1 and 720"));
    }

    @Test
    @Order(11)
    @DisplayName("Should retrieve sensor statistics")
    public void testGetSensorStatistics() {
        givenJson()
                .when()
                .get("/readings/sensor/" + SENSOR_ID + "/stats")
                .then()
                .statusCode(200)
                .body("totalReadings", greaterThanOrEqualTo(3))
                .body("avgTemperature", notNullValue())
                .body("avgHumidity", notNullValue())
                .body("avgCo2", notNullValue())
                .body("minTemperature", notNullValue())
                .body("maxTemperature", notNullValue());
    }

    @Test
    @Order(12)
    @DisplayName("Should retrieve air quality summary")
    public void testGetAirQualitySummary() {
        // Create readings for different sensors
        createReading("sensor-summary-001", 21.0, 44.0, 440.0, 115.0, 11.0, 17.0);
        createReading("sensor-summary-002", 22.0, 45.0, 450.0, 120.0, 12.0, 18.0);

        givenJson()
                .when()
                .get("/readings/summary")
                .then()
                .statusCode(200)
                .body("totalSensors", greaterThanOrEqualTo(3))
                .body("latestReadings", notNullValue())
                .body("timestamp", notNullValue());
    }

    @Test
    @Order(13)
    @DisplayName("Should update a reading")
    public void testUpdateReading() {
        Map<String, Object> updatedReading = createReadingPayload(SENSOR_ID, 25.0, 50.0, 500.0, 150.0, 15.0, 22.0);

        givenJson()
                .body(updatedReading)
                .when()
                .put("/readings/" + createdReadingId)
                .then()
                .statusCode(200)
                .body("id", equalTo(createdReadingId))
                .body("temperature", equalTo(25.0f))
                .body("humidity", equalTo(50.0f));
    }

    @Test
    @Order(14)
    @DisplayName("Should validate reading ranges")
    public void testReadingValidation() {
        // Test with extreme CO2 values
        Map<String, Object> extremeReading = createReadingPayload(SENSOR_ID, 22.0, 45.0, 15000.0, 120.0, 12.0, 18.0);

        givenJson()
                .body(extremeReading)
                .when()
                .post("/readings")
                .then()
                .statusCode(400)
                .body("error", containsString("Invalid sensor data values"));
    }

    @Test
    @Order(15)
    @DisplayName("Should delete readings by sensor")
    public void testDeleteReadingsBySensor() {
        String testSensorId = "sensor-to-delete";
        createReading(testSensorId, 22.0, 45.0, 450.0, 120.0, 12.0, 18.0);

        givenJson()
                .when()
                .delete("/readings/sensor/" + testSensorId)
                .then()
                .statusCode(200)
                .body("message", containsString("deleted"));

        // Verify readings are deleted
        givenJson()
                .when()
                .get("/readings/sensor/" + testSensorId)
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    @Order(16)
    @DisplayName("Should handle readings with location data")
    public void testReadingWithLocation() {
        Map<String, Object> reading = createReadingPayload(SENSOR_ID, 22.5, 45.0, 450.0, 120.0, 12.0, 18.0);

        Map<String, Object> location = new HashMap<>();
        location.put("latitude", 36.8065);
        location.put("longitude", 10.1815);
        location.put("altitude", 0.0);
        reading.put("location", location);

        givenJson()
                .body(reading)
                .when()
                .post("/readings")
                .then()
                .statusCode(201)
                .body("location.latitude", equalTo(36.8065f))
                .body("location.longitude", equalTo(10.1815f));
    }

    // Helper methods
    private Map<String, Object> createReadingPayload(String sensorId, double temp, double humidity,
                                                     double co2, double voc, double pm25, double pm10) {
        Map<String, Object> reading = new HashMap<>();
        reading.put("sensorId", sensorId);
        reading.put("temperature", temp);
        reading.put("humidity", humidity);
        reading.put("co2", co2);
        reading.put("voc", voc);
        reading.put("pm25", pm25);
        reading.put("pm10", pm10);
        reading.put("timestamp", Instant.now().toString());
        return reading;
    }

    private void createReading(String sensorId, double temp, double humidity,
                               double co2, double voc, double pm25, double pm10) {
        Map<String, Object> reading = createReadingPayload(sensorId, temp, humidity, co2, voc, pm25, pm10);
        givenJson()
                .body(reading)
                .when()
                .post("/readings")
                .then()
                .statusCode(201);
    }
}