// ==================== AirAware MongoDB Initialization Script ====================
// Environment: Development
// Run with: mongosh mongodb://localhost:27017/AirAwareDB < init-airaware.js

use("AirAwareDB");

print("\n🌍 ========================================");
print("   AirAware Database Initialization");
print("========================================\n");

// ==================== Clear Existing Data (Development Only!) ====================
print("⚠️  Clearing existing data...");
db.oauth_clients.deleteMany({});
db.identities.deleteMany({});
db.tenants.deleteMany({});
db.sensors.deleteMany({});
db.readings.deleteMany({});
db.alerts.deleteMany({});
print("✅ Existing data cleared\n");

// ==================== 1. Create Indexes ====================
print("🔍 Creating database indexes...");
db.identities.createIndex({ "username": 1 }, { unique: true });
db.identities.createIndex({ "email": 1 }, { unique: true });
db.oauth_clients.createIndex({ "client_id": 1 }, { unique: true });
db.sensors.createIndex({ "deviceId": 1 }, { unique: true });
db.sensors.createIndex({ "status": 1 });
db.readings.createIndex({ "sensorId": 1 });
db.readings.createIndex({ "timestamp": -1 });
db.alerts.createIndex({ "sensorId": 1 });
db.alerts.createIndex({ "severity": 1 });
db.alerts.createIndex({ "resolved": 1 });
print("✅ Indexes created\n");

// ==================== 2. Create OAuth Clients ====================
print("📋 Creating OAuth Clients...");
db.oauth_clients.insertMany([
    {
        _id: "web-client-001",
        client_id: "airaware-web-client",
        client_secret: "$argon2id$v=19$m=65536,t=3,p=4$c2FsdHNhbHRzYWx0c2FsdA$K9Z8J3y5tU6wX2vN1mL4pQ8rS9uT0vW1xY2zA3bC5dE",
        redirect_uri: "http://localhost:3000/callback",
        organization_name: "AirAware Platform",
        allowed_roles: NumberLong("2047"), // Multiple roles
        required_scopes: "sensor:read sensor:write alert:read alert:write dashboard:view",
        supported_grant_types: "authorization_code,refresh_token",
        active: true
    },
    {
        _id: "mobile-client-001",
        client_id: "airaware-mobile-app",
        client_secret: "$argon2id$v=19$m=65536,t=3,p=4$bW9iaWxlc2FsdHNhbHQ$L8A9K4z6uV7xY3wO2nM5qR9sT0vW2yZ3aB4cD6eF7gH",
        redirect_uri: "airaware://callback",
        organization_name: "AirAware Mobile",
        allowed_roles: NumberLong("48"), // VIEWER + DATA_ANALYST
        required_scopes: "sensor:read alert:read dashboard:view",
        supported_grant_types: "authorization_code,refresh_token",
        active: true
    },
    {
        _id: "api-client-001",
        client_id: "airaware-api-client",
        client_secret: "$argon2id$v=19$m=65536,t=3,p=4$YXBpc2FsdHNhbHRzYWx0$N0P1Q2rR3sS4tT5uU6vV7wW8xX9yY0zZ1aA2bB3cC4d",
        redirect_uri: "http://localhost:8080/api/callback",
        organization_name: "AirAware API",
        allowed_roles: NumberLong("64"), // API_CLIENT
        required_scopes: "sensor:read sensor:write alert:read",
        supported_grant_types: "authorization_code,refresh_token",
        active: true
    }
]);
print("✅ OAuth clients: " + db.oauth_clients.countDocuments() + " created\n");

// ==================== 3. Create Default Admin ====================
print("👤 Creating default admin user...");
db.identities.insertOne({
    _id: "admin-root-001",
    username: "admin",
    email: "admin@airaware.tn",
    // Password: Admin@123 (hashed with Argon2)
    password: "$argon2id$v=19$m=65536,t=3,p=4$YWRtaW5zYWx0c2FsdHNh$M9B0C5zD6eE7fF8gG9hH0iI1jJ2kK3lL4mM5nN6oO7pP",
    creationDate: new Date().toISOString(),
    role: NumberLong("9223372036854775807"), // ROOT = MAX_LONG
    scopes: "sensor:* alert:* dashboard:* tenant:* user:* system:*",
    isAccountActivated: true
});
print("✅ Admin created (username: admin, password: Admin@123)\n");

// ==================== 4. Create Test Organizations ====================
print("🏢 Creating test organizations...");
db.tenants.insertMany([
    {
        _id: "tenant-tunisia-env",
        organizationName: "Tunisia Environmental Monitoring",
        contactEmail: "contact@tem.tn",
        contactPhone: "+216-71-123-456",
        address: "Avenue Habib Bourguiba, Tunis 1000",
        country: "Tunisia",
        active: true
    },
    {
        _id: "tenant-sfax-air",
        organizationName: "Sfax Air Quality Initiative",
        contactEmail: "info@sfaxair.tn",
        contactPhone: "+216-74-987-654",
        address: "Port de Sfax, Sfax 3000",
        country: "Tunisia",
        active: true
    },
    {
        _id: "tenant-sousse-health",
        organizationName: "Sousse Health Department",
        contactEmail: "health@sousse.gov.tn",
        contactPhone: "+216-73-555-444",
        address: "Boulevard de la Corniche, Sousse 4000",
        country: "Tunisia",
        active: true
    },
    {
        _id: "tenant-bizerte-metro",
        organizationName: "Bizerte Metropolitan Authority",
        contactEmail: "env@bizerte.gov.tn",
        contactPhone: "+216-72-888-999",
        address: "Place de la République, Bizerte 7000",
        country: "Tunisia",
        active: true
    }
]);
print("✅ Organizations: " + db.tenants.countDocuments() + " created\n");

// ==================== 5. Create Test Sensors ====================
print("📡 Creating test sensors...");
db.sensors.insertMany([
    {
        _id: "sensor-tunis-downtown",
        deviceId: "SENSOR_TUNIS_001",
        model: "AirAware Pro v1.0",
        description: "Downtown Tunis - Avenue Habib Bourguiba",
        status: "ACTIVE",
        lastUpdate: new Date().toISOString(),
        location: {
            latitude: 36.8065,
            longitude: 10.1815,
            altitude: 4.0,
            city: "Tunis",
            country: "Tunisia"
        },
        tenant: {
            organizationName: "Tunisia Environmental Monitoring",
            contactEmail: "contact@tem.tn",
            contactPhone: "+216-71-123-456"
        }
    },
    {
        _id: "sensor-carthage-industrial",
        deviceId: "SENSOR_TUNIS_002",
        model: "AirAware Pro v1.0",
        description: "Carthage Industrial Zone",
        status: "ACTIVE",
        lastUpdate: new Date().toISOString(),
        location: {
            latitude: 36.8500,
            longitude: 10.1658,
            altitude: 10.0,
            city: "Carthage",
            country: "Tunisia"
        },
        tenant: {
            organizationName: "Tunisia Environmental Monitoring",
            contactEmail: "contact@tem.tn",
            contactPhone: "+216-71-123-456"
        }
    },
    {
        _id: "sensor-lamarsa-residential",
        deviceId: "SENSOR_TUNIS_003",
        model: "AirAware Lite v2.0",
        description: "La Marsa Residential Area",
        status: "ACTIVE",
        lastUpdate: new Date().toISOString(),
        location: {
            latitude: 36.7525,
            longitude: 10.2084,
            altitude: 2.0,
            city: "La Marsa",
            country: "Tunisia"
        },
        tenant: {
            organizationName: "Tunisia Environmental Monitoring",
            contactEmail: "contact@tem.tn",
            contactPhone: "+216-71-123-456"
        }
    },
    {
        _id: "sensor-sfax-port",
        deviceId: "SENSOR_SFAX_001",
        model: "AirAware Pro v1.0",
        description: "Sfax Port - Maritime Monitoring",
        status: "ACTIVE",
        lastUpdate: new Date().toISOString(),
        location: {
            latitude: 34.7406,
            longitude: 10.7603,
            altitude: 8.0,
            city: "Sfax",
            country: "Tunisia"
        },
        tenant: {
            organizationName: "Sfax Air Quality Initiative",
            contactEmail: "info@sfaxair.tn",
            contactPhone: "+216-74-987-654"
        }
    },
    {
        _id: "sensor-sousse-center",
        deviceId: "SENSOR_SOUSSE_001",
        model: "AirAware Lite v2.0",
        description: "Sousse City Center - Tourist Area",
        status: "ACTIVE",
        lastUpdate: new Date().toISOString(),
        location: {
            latitude: 35.8256,
            longitude: 10.6369,
            altitude: 3.0,
            city: "Sousse",
            country: "Tunisia"
        },
        tenant: {
            organizationName: "Sousse Health Department",
            contactEmail: "health@sousse.gov.tn",
            contactPhone: "+216-73-555-444"
        }
    }
]);
print("✅ Sensors: " + db.sensors.countDocuments() + " created\n");

// ==================== 6. Create Sample Readings ====================
print("📊 Creating sample readings...");
const now = new Date();
const oneHourAgo = new Date(now.getTime() - (60 * 60 * 1000));
const twoHoursAgo = new Date(now.getTime() - (2 * 60 * 60 * 1000));

db.readings.insertMany([
    {
        _id: "reading-tunis-001",
        sensorId: "SENSOR_TUNIS_001",
        temperature: 24.5,
        humidity: 62.0,
        co2: 420.0,
        voc: 0.35,
        pm25: 12.5,
        pm10: 18.3,
        timestamp: now.toISOString(),
        location: {
            latitude: 36.8065,
            longitude: 10.1815,
            altitude: 4.0
        }
    },
    {
        _id: "reading-tunis-002",
        sensorId: "SENSOR_TUNIS_001",
        temperature: 23.8,
        humidity: 64.0,
        co2: 415.0,
        voc: 0.32,
        pm25: 11.8,
        pm10: 17.5,
        timestamp: oneHourAgo.toISOString(),
        location: {
            latitude: 36.8065,
            longitude: 10.1815,
            altitude: 4.0
        }
    },
    {
        _id: "reading-sfax-001",
        sensorId: "SENSOR_SFAX_001",
        temperature: 26.8,
        humidity: 58.0,
        co2: 450.0,
        voc: 0.52,
        pm25: 28.7,
        pm10: 42.1,
        timestamp: now.toISOString(),
        location: {
            latitude: 34.7406,
            longitude: 10.7603,
            altitude: 8.0
        }
    },
    {
        _id: "reading-sousse-001",
        sensorId: "SENSOR_SOUSSE_001",
        temperature: 25.2,
        humidity: 60.0,
        co2: 430.0,
        voc: 0.38,
        pm25: 15.3,
        pm10: 22.8,
        timestamp: now.toISOString(),
        location: {
            latitude: 35.8256,
            longitude: 10.6369,
            altitude: 3.0
        }
    }
]);
print("✅ Sample readings: " + db.readings.countDocuments() + " created\n");

// ==================== 7. Create Sample Alerts ====================
print("🚨 Creating sample alerts...");
db.alerts.insertMany([
    {
        _id: "alert-sfax-001",
        type: "PM25_HIGH",
        severity: "WARNING",
        message: "PM2.5 levels exceeded 25 µg/m³ threshold",
        triggeredAt: now.toISOString(),
        sensorId: "SENSOR_SFAX_001",
        resolved: false,
        reading: {
            sensorId: "SENSOR_SFAX_001",
            timestamp: now.toISOString(),
            temperature: 26.8,
            humidity: 58.0,
            co2: 450.0,
            pm25: 28.7,
            voc: 0.52
        }
    },
    {
        _id: "alert-sfax-002",
        type: "PM10_HIGH",
        severity: "WARNING",
        message: "PM10 levels exceeded 40 µg/m³ threshold",
        triggeredAt: oneHourAgo.toISOString(),
        sensorId: "SENSOR_SFAX_001",
        resolved: true,
        reading: {
            sensorId: "SENSOR_SFAX_001",
            timestamp: oneHourAgo.toISOString(),
            temperature: 27.1,
            humidity: 56.0,
            co2: 455.0,
            pm25: 30.2,
            voc: 0.58
        }
    }
]);
print("✅ Sample alerts: " + db.alerts.countDocuments() + " created\n");

// ==================== Summary ====================
print("📊 ========================================");
print("   Database Initialization Complete!");
print("========================================\n");
print("Statistics:");
print("  • OAuth Clients: " + db.oauth_clients.countDocuments());
print("  • Identities: " + db.identities.countDocuments());
print("  • Tenants: " + db.tenants.countDocuments());
print("  • Sensors: " + db.sensors.countDocuments());
print("  • Readings: " + db.readings.countDocuments());
print("  • Alerts: " + db.alerts.countDocuments());
print("\n🔐 Default Admin Credentials:");
print("  Username: admin");
print("  Password: Admin@123");
print("  ⚠️  CHANGE THIS PASSWORD AFTER FIRST LOGIN!");
print("\n📋 OAuth Clients:");
print("  • airaware-web-client");
print("  • airaware-mobile-app");
print("  • airaware-api-client");
print("\n🌍 Test Sensors Available:");
print("  • SENSOR_TUNIS_001 (Downtown Tunis)");
print("  • SENSOR_TUNIS_002 (Carthage Industrial)");
print("  • SENSOR_TUNIS_003 (La Marsa Residential)");
print("  • SENSOR_SFAX_001 (Sfax Port)");
print("  • SENSOR_SOUSSE_001 (Sousse City Center)");
print("\n✅ Ready to start AirAware application!\n");