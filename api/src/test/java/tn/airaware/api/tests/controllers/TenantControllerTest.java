package tn.airaware.api.tests.controllers;

import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import tn.airaware.api.tests.BaseIntegrationTest;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for TenantController
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TenantControllerTest extends BaseIntegrationTest {

    private static String createdTenantId;
    private static final String ORG_NAME = "Smart Building Corp";

    @Test
    @Order(1)
    @DisplayName("Should create a new tenant")
    public void testCreateTenant() {
        Map<String, Object> tenant = createTenantPayload(
                ORG_NAME,
                "admin@smartbuilding.com",
                "+216-71-123-456",
                "123 Tech Street, Tunis",
                "Tunisia"
        );

        Response response = givenJson()
                .body(tenant)
                .when()
                .post("/tenants")
                .then()
                .statusCode(201)
                .body("organizationName", equalTo(ORG_NAME))
                .body("contactEmail", equalTo("admin@smartbuilding.com"))
                .body("contactPhone", equalTo("+216-71-123-456"))
                .body("address", equalTo("123 Tech Street, Tunis"))
                .body("country", equalTo("Tunisia"))
                .body("active", equalTo(true))
                .body("id", notNullValue())
                .extract()
                .response();

        createdTenantId = response.jsonPath().getString("id");
        System.out.println("✅ Created tenant with ID: " + createdTenantId);
    }

    @Test
    @Order(2)
    @DisplayName("Should fail to create tenant without organization name")
    public void testCreateTenantWithoutOrgName() {
        Map<String, Object> invalidTenant = new HashMap<>();
        invalidTenant.put("contactEmail", "test@example.com");

        givenJson()
                .body(invalidTenant)
                .when()
                .post("/tenants")
                .then()
                .statusCode(400)
                .body("error", containsString("Organization name is required"));
    }

    @Test
    @Order(3)
    @DisplayName("Should fail to create tenant without email")
    public void testCreateTenantWithoutEmail() {
        Map<String, Object> invalidTenant = new HashMap<>();
        invalidTenant.put("organizationName", "Test Corp");

        givenJson()
                .body(invalidTenant)
                .when()
                .post("/tenants")
                .then()
                .statusCode(400)
                .body("error", containsString("Contact email is required"));
    }

    @Test
    @Order(4)
    @DisplayName("Should fail to create tenant with invalid email")
    public void testCreateTenantWithInvalidEmail() {
        Map<String, Object> invalidTenant = createTenantPayload(
                "Invalid Email Corp",
                "invalid-email",
                "+1234567890",
                "Test Address",
                "Country"
        );

        givenJson()
                .body(invalidTenant)
                .when()
                .post("/tenants")
                .then()
                .statusCode(400)
                .body("error", containsString("Invalid email format"));
    }

    @Test
    @Order(5)
    @DisplayName("Should fail to create tenant with invalid phone")
    public void testCreateTenantWithInvalidPhone() {
        Map<String, Object> invalidTenant = createTenantPayload(
                "Invalid Phone Corp",
                "admin@test.com",
                "abc",
                "Test Address",
                "Country"
        );

        givenJson()
                .body(invalidTenant)
                .when()
                .post("/tenants")
                .then()
                .statusCode(400)
                .body("error", containsString("Invalid phone number format"));
    }

    @Test
    @Order(6)
    @DisplayName("Should fail to create duplicate tenant")
    public void testCreateDuplicateTenant() {
        Map<String, Object> duplicateTenant = createTenantPayload(
                ORG_NAME,
                "another@example.com",
                "+1234567890",
                "Another Address",
                "Country"
        );

        givenJson()
                .body(duplicateTenant)
                .when()
                .post("/tenants")
                .then()
                .statusCode(409)
                .body("error", containsString("Organization name already exists"));
    }

    @Test
    @Order(7)
    @DisplayName("Should retrieve all tenants")
    public void testGetAllTenants() {
        givenJson()
                .when()
                .get("/tenants")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)))
                .body("[0].organizationName", notNullValue());
    }

    @Test
    @Order(8)
    @DisplayName("Should retrieve tenant by ID")
    public void testGetTenantById() {
        givenJson()
                .when()
                .get("/tenants/" + createdTenantId)
                .then()
                .statusCode(200)
                .body("id", equalTo(createdTenantId))
                .body("organizationName", equalTo(ORG_NAME))
                .body("contactEmail", equalTo("admin@smartbuilding.com"));
    }

    @Test
    @Order(9)
    @DisplayName("Should return 404 for non-existent tenant")
    public void testGetNonExistentTenant() {
        givenJson()
                .when()
                .get("/tenants/non-existent-id")
                .then()
                .statusCode(404)
                .body("error", containsString("not found"));
    }

    @Test
    @Order(10)
    @DisplayName("Should retrieve tenant by organization name")
    public void testGetTenantByOrganization() {
        givenJson()
                .when()
                .get("/tenants/organization/" + ORG_NAME)
                .then()
                .statusCode(200)
                .body("organizationName", equalTo(ORG_NAME))
                .body("id", equalTo(createdTenantId));
    }

    @Test
    @Order(11)
    @DisplayName("Should retrieve active tenants")
    public void testGetActiveTenants() {
        givenJson()
                .when()
                .get("/tenants/active")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)))
                .body("[0].active", equalTo(true));
    }

    @Test
    @Order(12)
    @DisplayName("Should retrieve tenants by country")
    public void testGetTenantsByCountry() {
        givenJson()
                .when()
                .get("/tenants/country/Tunisia")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)))
                .body("[0].country", equalTo("Tunisia"));
    }

    @Test
    @Order(13)
    @DisplayName("Should retrieve tenant profile")
    public void testGetTenantProfile() {
        givenJson()
                .when()
                .get("/tenants/" + createdTenantId + "/profile")
                .then()
                .statusCode(200)
                .body("tenant.id", equalTo(createdTenantId))
                .body("totalSensors", greaterThanOrEqualTo(0))
                .body("activeSensors", greaterThanOrEqualTo(0))
                .body("inactiveSensors", greaterThanOrEqualTo(0));
    }

    @Test
    @Order(14)
    @DisplayName("Should retrieve tenant statistics")
    public void testGetTenantStatistics() {
        givenJson()
                .when()
                .get("/tenants/statistics")
                .then()
                .statusCode(200)
                .body("totalTenants", greaterThanOrEqualTo(1))
                .body("activeTenants", greaterThanOrEqualTo(0))
                .body("inactiveTenants", greaterThanOrEqualTo(0))
                .body("tenantsByCountry", notNullValue());
    }

    @Test
    @Order(15)
    @DisplayName("Should update tenant information")
    public void testUpdateTenant() {
        Map<String, Object> updatedTenant = createTenantPayload(
                ORG_NAME,
                "newemail@smartbuilding.com",
                "+216-71-999-888",
                "456 New Address, Tunis",
                "Tunisia"
        );

        givenJson()
                .body(updatedTenant)
                .when()
                .put("/tenants/" + createdTenantId)
                .then()
                .statusCode(200)
                .body("id", equalTo(createdTenantId))
                .body("organizationName", equalTo(ORG_NAME))
                .body("contactEmail", equalTo("newemail@smartbuilding.com"))
                .body("contactPhone", equalTo("+216-71-999-888"));
    }

    @Test
    @Order(16)
    @DisplayName("Should fail to update with invalid email")
    public void testUpdateTenantWithInvalidEmail() {
        Map<String, Object> invalidTenant = createTenantPayload(
                ORG_NAME,
                "invalid-email",
                "+1234567890",
                "Address",
                "Country"
        );

        givenJson()
                .body(invalidTenant)
                .when()
                .put("/tenants/" + createdTenantId)
                .then()
                .statusCode(400)
                .body("error", containsString("Invalid email format"));
    }

    @Test
    @Order(17)
    @DisplayName("Should deactivate tenant")
    public void testDeactivateTenant() {
        givenJson()
                .when()
                .put("/tenants/" + createdTenantId + "/deactivate")
                .then()
                .statusCode(200);

        // Verify tenant is deactivated
        givenJson()
                .when()
                .get("/tenants/" + createdTenantId)
                .then()
                .statusCode(200)
                .body("active", equalTo(false));
    }

    @Test
    @Order(18)
    @DisplayName("Should activate tenant")
    public void testActivateTenant() {
        givenJson()
                .when()
                .put("/tenants/" + createdTenantId + "/activate")
                .then()
                .statusCode(200);

        // Verify tenant is activated
        givenJson()
                .when()
                .get("/tenants/" + createdTenantId)
                .then()
                .statusCode(200)
                .body("active", equalTo(true));
    }

    @Test
    @Order(19)
    @DisplayName("Should delete tenant without sensors")
    public void testDeleteTenant() {
        // Create a tenant without sensors
        Map<String, Object> tenantToDelete = createTenantPayload(
                "Tenant To Delete",
                "delete@example.com",
                "+1234567890",
                "Test Address",
                "Test Country"
        );

        Response response = givenJson()
                .body(tenantToDelete)
                .when()
                .post("/tenants")
                .then()
                .statusCode(201)
                .extract()
                .response();

        String tenantId = response.jsonPath().getString("id");

        // Delete the tenant
        givenJson()
                .when()
                .delete("/tenants/" + tenantId)
                .then()
                .statusCode(200)
                .body("message", containsString("deleted successfully"));

        // Verify tenant is deleted
        givenJson()
                .when()
                .get("/tenants/" + tenantId)
                .then()
                .statusCode(404);
    }

    @Test
    @Order(20)
    @DisplayName("Should handle multiple tenants from different countries")
    public void testMultipleTenants() {
        // Create tenants from different countries
        createTenant("French Corp", "admin@french.com", "France");
        createTenant("German Corp", "admin@german.com", "Germany");
        createTenant("Italian Corp", "admin@italian.com", "Italy");

        // Verify tenants are created
        givenJson()
                .when()
                .get("/tenants")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(4)));

        // Verify statistics include different countries
        givenJson()
                .when()
                .get("/tenants/statistics")
                .then()
                .statusCode(200)
                .body("tenantsByCountry.size()", greaterThanOrEqualTo(4));
    }

    // Helper methods
    private Map<String, Object> createTenantPayload(String orgName, String email, String phone,
                                                    String address, String country) {
        Map<String, Object> tenant = new HashMap<>();
        tenant.put("organizationName", orgName);
        tenant.put("contactEmail", email);
        tenant.put("contactPhone", phone);
        tenant.put("address", address);
        tenant.put("country", country);
        tenant.put("active", true);
        return tenant;
    }

    private void createTenant(String orgName, String email, String country) {
        Map<String, Object> tenant = createTenantPayload(
                orgName,
                email,
                "+1234567890",
                "Test Address",
                country
        );
        givenJson()
                .body(tenant)
                .when()
                .post("/tenants")
                .then()
                .statusCode(201);
    }
}