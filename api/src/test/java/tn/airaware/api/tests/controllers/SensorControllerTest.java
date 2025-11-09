package tn.airaware.api.tests.controllers;

import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import tn.airaware.api.tests.BaseIntegrationTest;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for SensorController
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SensorControllerTest extends BaseIntegrationTest {

    private static String createdSensorId;
    private static final String DEVICE_ID = "BME680-001";

    @Test
    @Order(1)
    @DisplayName("Should register a new sensor")
    public void testRegisterSensor() {
        Map<String, Object> sensor = createSensorPayload(
                DEVICE_ID,
                "BME680",
                "Living Room Air Quality Sensor"
        );

        Response response = givenJson()
                .body(sensor)
                .when()
                .post("/sensors")
                .then()
                .statusCode(201)
                .body("deviceId", equalTo(DEVICE_ID))
                .body("model", equalTo("BME680"))
                .body("description", equalTo("Living Room Air Quality Sensor"))
                .body("status", equalTo("ACTIVE"))
                .body("id", notNullValue())
                .extract()
                .response();

        createdSensorId = response.jsonPath().getString("id");
        System.out.println("✅ Created sensor with ID: " + createdSensorId);
    }

    @Test
    @Order(2)
    @DisplayName("Should fail to register sensor without device ID")
    public void testRegisterSensorWithoutDeviceId() {
        Map<String, Object> invalidSensor = new HashMap<>();
        invalidSensor.put("model", "BME680");
        invalidSensor.put("description", "Test sensor");

        givenJson()
                .body(invalidSensor)
                .when()
                .post("/sensors")
                .then()
                .statusCode(400)
                .body("error", containsString("Device ID is required"));
    }

    @Test
    @Order(3)
    @DisplayName("Should fail to register sensor without model")
    public void testRegisterSensorWithoutModel() {
        Map<String, Object> invalidSensor = new HashMap<>();
        invalidSensor.put("deviceId", "TEST-001");
        invalidSensor.put("description", "Test sensor");

        givenJson()
                .body(invalidSensor)
                .when()
                .post("/sensors")
                .then()
                .statusCode(400)
                .body("error", containsString("model is required"));
    }

    @Test
    @Order(4)
    @DisplayName("Should retrieve all sensors")
    public void testGetAllSensors() {
        givenJson()
                .when()
                .get("/sensors")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)))
                .body("[0].deviceId", notNullValue());
    }

    @Test
    @Order(5)
    @DisplayName("Should retrieve sensor by ID")
    public void testGetSensorById() {
        givenJson()
                .when()
                .get("/sensors/" + createdSensorId)
                .then()
                .statusCode(200)
                .body("id", equalTo(createdSensorId))
                .body("deviceId", equalTo(DEVICE_ID))
                .body("model", equalTo("BME680"));
    }

    @Test
    @Order(6)
    @DisplayName("Should return 404 for non-existent sensor")
    public void testGetNonExistentSensor() {
        givenJson()
                .when()
                .get("/sensors/non-existent-id")
                .then()
                .statusCode(404)
                .body("error", containsString("not found"));
    }

    @Test
    @Order(7)
    @DisplayName("Should retrieve active sensors")
    public void testGetActiveSensors() {
        givenJson()
                .when()
                .get("/sensors/active")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)))
                .body("[0].status", equalTo("ACTIVE"));
    }

    @Test
    @Order(8)
    @DisplayName("Should retrieve sensors by status")
    public void testGetSensorsByStatus() {
        givenJson()
                .when()
                .get("/sensors/status/ACTIVE")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)));
    }

    @Test
    @Order(9)
    @DisplayName("Should fail with invalid status")
    public void testGetSensorsByInvalidStatus() {
        givenJson()
                .when()
                .get("/sensors/status/INVALID_STATUS")
                .then()
                .statusCode(400)
                .body("error", containsString("Invalid status"));
    }

    @Test
    @Order(10)
    @DisplayName("Should get sensor health")
    public void testGetSensorHealth() {
        givenJson()
                .when()
                .get("/sensors/" + createdSensorId + "/health")
                .then()
                .statusCode(200)
                .body("sensor.id", equalTo(createdSensorId))
                .body("isOnline", notNullValue())
                .body("isResponsive", notNullValue())
                .body("totalReadings", greaterThanOrEqualTo(0));
    }

    @Test
    @Order(11)
    @DisplayName("Should get sensors overview")
    public void testGetSensorsOverview() {
        givenJson()
                .when()
                .get("/sensors/overview")
                .then()
                .statusCode(200)
                .body("totalSensors", greaterThanOrEqualTo(1))
                .body("activeSensors", greaterThanOrEqualTo(0))
                .body("inactiveSensors", greaterThanOrEqualTo(0))
                .body("timestamp", notNullValue());
    }

    @Test
    @Order(12)
    @DisplayName("Should update sensor information")
    public void testUpdateSensor() {
        Map<String, Object> updatedSensor = createSensorPayload(
                DEVICE_ID,
                "BME680 v2",
                "Updated Living Room Sensor"
        );

        givenJson()
                .body(updatedSensor)
                .when()
                .put("/sensors/" + createdSensorId)
                .then()
                .statusCode(200)
                .body("id", equalTo(createdSensorId))
                .body("model", equalTo("BME680 v2"))
                .body("description", equalTo("Updated Living Room Sensor"));
    }

    @Test
    @Order(13)
    @DisplayName("Should update sensor status")
    public void testUpdateSensorStatus() {
        Map<String, String> statusUpdate = new HashMap<>();
        statusUpdate.put("status", "MAINTENANCE");

        givenJson()
                .body(statusUpdate)
                .when()
                .put("/sensors/" + createdSensorId + "/status")
                .then()
                .statusCode(200)
                .body("status", equalTo("MAINTENANCE"));

        // Verify status was updated
        givenJson()
                .when()
                .get("/sensors/" + createdSensorId)
                .then()
                .statusCode(200)
                .body("status", equalTo("MAINTENANCE"));
    }

    @Test
    @Order(14)
    @DisplayName("Should fail to update with invalid status")
    public void testUpdateSensorWithInvalidStatus() {
        Map<String, String> invalidStatus = new HashMap<>();
        invalidStatus.put("status", "INVALID");

        givenJson()
                .body(invalidStatus)
                .when()
                .put("/sensors/" + createdSensorId + "/status")
                .then()
                .statusCode(400)
                .body("error", containsString("Invalid status"));
    }

    @Test
    @Order(15)
    @DisplayName("Should deactivate sensor")
    public void testDeactivateSensor() {
        givenJson()
                .when()
                .put("/sensors/" + createdSensorId + "/deactivate")
                .then()
                .statusCode(200);

        // Verify sensor is deactivated
        givenJson()
                .when()
                .get("/sensors/" + createdSensorId)
                .then()
                .statusCode(200)
                .body("status", equalTo("INACTIVE"));
    }

    @Test
    @Order(16)
    @DisplayName("Should register sensor with location")
    public void testRegisterSensorWithLocation() {
        Map<String, Object> sensor = createSensorPayload(
                "BME680-002",
                "BME680",
                "Bedroom Sensor"
        );

        Map<String, Object> location = new HashMap<>();
        location.put("latitude", 36.8065);
        location.put("longitude", 10.1815);
        sensor.put("location", location);

        givenJson()
                .body(sensor)
                .when()
                .post("/sensors")
                .then()
                .statusCode(201)
                .body("location.latitude", equalTo(36.8065f))
                .body("location.longitude", equalTo(10.1815f));
    }

    @Test
    @Order(17)
    @DisplayName("Should register sensor with tenant")
    public void testRegisterSensorWithTenant() {
        Map<String, Object> sensor = createSensorPayload(
                "BME680-003",
                "BME680",
                "Office Sensor"
        );

        Map<String, Object> tenant = new HashMap<>();
        tenant.put("organizationName", "Test Corp");
        tenant.put("contactEmail", "admin@testcorp.com");
        tenant.put("contactPhone", "+1234567890");
        sensor.put("tenant", tenant);

        givenJson()
                .body(sensor)
                .when()
                .post("/sensors")
                .then()
                .statusCode(201)
                .body("tenant.organizationName", equalTo("Test Corp"))
                .body("tenant.contactEmail", equalTo("admin@testcorp.com"));
    }

    @Test
    @Order(18)
    @DisplayName("Should retrieve sensors by tenant")
    public void testGetSensorsByTenant() {
        givenJson()
                .when()
                .get("/sensors/tenant/Test Corp")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)))
                .body("[0].tenant.organizationName", equalTo("Test Corp"));
    }

    @Test
    @Order(19)
    @DisplayName("Should handle multiple sensors")
    public void testMultipleSensors() {
        // Register multiple sensors
        for (int i = 4; i <= 6; i++) {
            Map<String, Object> sensor = createSensorPayload(
                    "SENSOR-" + i,
                    "Generic Model",
                    "Test Sensor " + i
            );
            givenJson()
                    .body(sensor)
                    .when()
                    .post("/sensors")
                    .then()
                    .statusCode(201);
        }

        // Verify all sensors are retrieved
        givenJson()
                .when()
                .get("/sensors")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(6)));
    }

    // Helper methods
    private Map<String, Object> createSensorPayload(String deviceId, String model, String description) {
        Map<String, Object> sensor = new HashMap<>();
        sensor.put("deviceId", deviceId);
        sensor.put("model", model);
        sensor.put("description", description);
        sensor.put("status", "ACTIVE");
        return sensor;
    }
}