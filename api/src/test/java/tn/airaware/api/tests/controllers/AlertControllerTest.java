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
 * Integration tests for AlertController
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AlertControllerTest extends BaseIntegrationTest {

    private static String createdAlertId;
    private static final String SENSOR_ID = "test-sensor-001";

    @Test
    @Order(1)
    @DisplayName("Should create a new alert")
    public void testCreateAlert() {
        Map<String, Object> alert = createAlertPayload(
                "CO2_HIGH",
                "WARNING",
                "CO2 levels exceeded safe threshold",
                SENSOR_ID
        );

        Response response = givenJson()
                .body(alert)
                .when()
                .post("/alerts")
                .then()
                .statusCode(201)
                .body("type", equalTo("CO2_HIGH"))
                .body("severity", equalTo("WARNING"))
                .body("message", equalTo("CO2 levels exceeded safe threshold"))
                .body("sensorId", equalTo(SENSOR_ID))
                .body("resolved", equalTo(false))
                .body("id", notNullValue())
                .extract()
                .response();

        createdAlertId = response.jsonPath().getString("id");
        System.out.println("✅ Created alert with ID: " + createdAlertId);
    }

    @Test
    @Order(2)
    @DisplayName("Should fail to create alert without required fields")
    public void testCreateAlertWithoutRequiredFields() {
        Map<String, Object> invalidAlert = new HashMap<>();
        invalidAlert.put("message", "Test alert");

        givenJson()
                .body(invalidAlert)
                .when()
                .post("/alerts")
                .then()
                .statusCode(400)
                .body("error", containsString("required"));
    }

    @Test
    @Order(3)
    @DisplayName("Should retrieve all alerts")
    public void testGetAllAlerts() {
        givenJson()
                .when()
                .get("/alerts")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)))
                .body("[0].type", notNullValue());
    }

    @Test
    @Order(4)
    @DisplayName("Should retrieve alert by ID")
    public void testGetAlertById() {
        givenJson()
                .when()
                .get("/alerts/" + createdAlertId)
                .then()
                .statusCode(200)
                .body("id", equalTo(createdAlertId))
                .body("type", equalTo("CO2_HIGH"))
                .body("severity", equalTo("WARNING"))
                .body("sensorId", equalTo(SENSOR_ID));
    }

    @Test
    @Order(5)
    @DisplayName("Should return 404 for non-existent alert")
    public void testGetNonExistentAlert() {
        givenJson()
                .when()
                .get("/alerts/non-existent-id")
                .then()
                .statusCode(404)
                .body("error", containsString("not found"));
    }

    @Test
    @Order(6)
    @DisplayName("Should retrieve alerts by severity")
    public void testGetAlertsBySeverity() {
        givenJson()
                .when()
                .get("/alerts/severity/WARNING")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)))
                .body("[0].severity", equalTo("WARNING"));
    }

    @Test
    @Order(7)
    @DisplayName("Should retrieve alerts by sensor ID")
    public void testGetAlertsBySensor() {
        givenJson()
                .when()
                .get("/alerts/sensor/" + SENSOR_ID)
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)))
                .body("[0].sensorId", equalTo(SENSOR_ID));
    }

    @Test
    @Order(8)
    @DisplayName("Should retrieve unresolved alerts")
    public void testGetUnresolvedAlerts() {
        givenJson()
                .when()
                .get("/alerts/unresolved")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)))
                .body("[0].resolved", equalTo(false));
    }

    @Test
    @Order(9)
    @DisplayName("Should update an alert")
    public void testUpdateAlert() {
        Map<String, Object> updatedAlert = createAlertPayload(
                "CO2_CRITICAL",
                "CRITICAL",
                "CO2 levels critically high",
                SENSOR_ID
        );

        givenJson()
                .body(updatedAlert)
                .when()
                .put("/alerts/" + createdAlertId)
                .then()
                .statusCode(200)
                .body("id", equalTo(createdAlertId))
                .body("type", equalTo("CO2_CRITICAL"))
                .body("severity", equalTo("CRITICAL"));
    }

    @Test
    @Order(10)
    @DisplayName("Should resolve an alert")
    public void testResolveAlert() {
        givenJson()
                .when()
                .put("/alerts/" + createdAlertId + "/resolve")
                .then()
                .statusCode(200);

        // Verify alert is resolved
        givenJson()
                .when()
                .get("/alerts/" + createdAlertId)
                .then()
                .statusCode(200)
                .body("resolved", equalTo(true));
    }

    @Test
    @Order(11)
    @DisplayName("Should delete an alert")
    public void testDeleteAlert() {
        givenJson()
                .when()
                .delete("/alerts/" + createdAlertId)
                .then()
                .statusCode(200)
                .body("message", containsString("deleted successfully"));

        // Verify alert is deleted
        givenJson()
                .when()
                .get("/alerts/" + createdAlertId)
                .then()
                .statusCode(404);
    }

    @Test
    @Order(12)
    @DisplayName("Should handle multiple alerts for different sensors")
    public void testMultipleSensorAlerts() {
        // Create alerts for different sensors
        createAlert("PM25_HIGH", "WARNING", "sensor-002");
        createAlert("VOC_HIGH", "INFO", "sensor-003");

        givenJson()
                .when()
                .get("/alerts")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(2)));
    }

    // Helper methods
    private Map<String, Object> createAlertPayload(String type, String severity, String message, String sensorId) {
        Map<String, Object> alert = new HashMap<>();
        alert.put("type", type);
        alert.put("severity", severity);
        alert.put("message", message);
        alert.put("sensorId", sensorId);
        alert.put("triggeredAt", Instant.now().toString());
        alert.put("resolved", false);

        // Add sample reading data
        Map<String, Object> reading = new HashMap<>();
        reading.put("sensorId", sensorId);
        reading.put("temperature", 22.5);
        reading.put("humidity", 45.0);
        reading.put("co2", 850.0);
        reading.put("voc", 120.0);
        reading.put("pm25", 15.0);
        reading.put("timestamp", Instant.now().toString());
        alert.put("reading", reading);

        return alert;
    }

    private void createAlert(String type, String severity, String sensorId) {
        Map<String, Object> alert = createAlertPayload(type, severity, "Test alert", sensorId);
        givenJson()
                .body(alert)
                .when()
                .post("/alerts")
                .then()
                .statusCode(201);
    }
}