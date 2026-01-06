package tn.airaware.api.mqtt;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import org.bson.Document;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import tn.airaware.api.config.ApiDatabase;
import tn.airaware.api.entities.Alert;
import tn.airaware.api.entities.Reading;
import tn.airaware.api.services.AlertThresholdService;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * MQTT Subscriber for receiving sensor readings from Raspberry Pi devices
 *
 * ENHANCED: Now includes automatic alert generation when thresholds are exceeded
 * FIXED: Added InstantTypeAdapter for proper java.time.Instant handling with Gson
 */
@Singleton
@Startup
public class MQTTSubscriber implements MqttCallback {

    private static final Logger LOGGER = Logger.getLogger(MQTTSubscriber.class.getName());

    // MQTT Configuration
    private static final String BROKER_URL = System.getenv("MQTT_BROKER_URL") != null ?
            System.getenv("MQTT_BROKER_URL") : "tcp://localhost:1883";
    private static final String CLIENT_ID = "airaware-api-" + UUID.randomUUID().toString().substring(0, 8);
    private static final String TOPIC = "airaware/sensors";
    private static final int QOS = 1;

    // Reconnection settings
    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    private static final long RECONNECT_DELAY_MS = 5000;

    @Inject
    @ApiDatabase
    private MongoDatabase database;

    @Inject
    private AlertThresholdService alertThresholdService;

    private MqttClient mqttClient;

    // Gson with custom Instant adapter - THIS IS THE FIX!
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
            .create();

    private volatile boolean isRunning = true;
    private int reconnectAttempts = 0;

    /**
     * Custom TypeAdapter for java.time.Instant
     * Handles ISO-8601 formatted timestamps from MQTT messages
     */
    private static class InstantTypeAdapter extends TypeAdapter<Instant> {

        @Override
        public void write(JsonWriter out, Instant value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.toString());
            }
        }

        @Override
        public Instant read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            String timestampStr = in.nextString();

            try {
                // Try standard ISO-8601 format first (e.g., "2025-12-28T13:26:18.585462+00:00")
                return Instant.parse(timestampStr);
            } catch (DateTimeParseException e1) {
                try {
                    // Try with offset format (e.g., "2025-12-28T13:26:18+00:00")
                    return DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(timestampStr, Instant::from);
                } catch (DateTimeParseException e2) {
                    try {
                        // Try without timezone (assume UTC)
                        return Instant.parse(timestampStr + "Z");
                    } catch (DateTimeParseException e3) {
                        LOGGER.warning("Could not parse timestamp: " + timestampStr + ", using current time");
                        return Instant.now();
                    }
                }
            }
        }
    }

    @PostConstruct
    public void init() {
        LOGGER.info("═══════════════════════════════════════════════════════════");
        LOGGER.info("🚀 Initializing MQTT Subscriber");
        LOGGER.info("   Broker: " + BROKER_URL);
        LOGGER.info("   Topic: " + TOPIC);
        LOGGER.info("   Client ID: " + CLIENT_ID);
        LOGGER.info("═══════════════════════════════════════════════════════════");

        connectToBroker();
    }

    private void connectToBroker() {
        try {
            mqttClient = new MqttClient(BROKER_URL, CLIENT_ID, new MemoryPersistence());
            mqttClient.setCallback(this);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            options.setConnectionTimeout(30);
            options.setKeepAliveInterval(60);

            LOGGER.info("📡 Connecting to MQTT broker...");
            mqttClient.connect(options);
            mqttClient.subscribe(TOPIC, QOS);

            reconnectAttempts = 0;
            LOGGER.info("✅ Successfully connected to MQTT broker and subscribed to: " + TOPIC);
            LOGGER.info("🔔 Alert threshold monitoring is ACTIVE");

        } catch (MqttException e) {
            LOGGER.severe("❌ Failed to connect to MQTT broker: " + e.getMessage());
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (!isRunning || reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            LOGGER.severe("Max reconnection attempts reached or service stopped");
            return;
        }

        reconnectAttempts++;
        LOGGER.info("Scheduling reconnection attempt " + reconnectAttempts + "/" + MAX_RECONNECT_ATTEMPTS);

        new Thread(() -> {
            try {
                Thread.sleep(RECONNECT_DELAY_MS);
                if (isRunning) {
                    connectToBroker();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    @PreDestroy
    public void cleanup() {
        isRunning = false;
        LOGGER.info("🔴 Shutting down MQTT Subscriber...");

        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                mqttClient.close();
            }
            LOGGER.info("MQTT Subscriber disconnected successfully");
        } catch (MqttException e) {
            LOGGER.warning("Error during MQTT cleanup: " + e.getMessage());
        }
    }

    // ==================== MQTT CALLBACK METHODS ====================

    @Override
    public void connectionLost(Throwable cause) {
        LOGGER.warning("⚠️ MQTT connection lost: " + cause.getMessage());
        if (isRunning) {
            scheduleReconnect();
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        try {
            String payload = new String(message.getPayload());

            LOGGER.info("═══════════════════════════════════════════════════════════");
            LOGGER.info("📨 MQTT MESSAGE RECEIVED!");
            LOGGER.info("   Topic: " + topic);
            LOGGER.info("   Payload: " + payload);
            LOGGER.info("   QoS: " + message.getQos());
            LOGGER.info("═══════════════════════════════════════════════════════════");

            // Parse the reading
            Reading reading = parseReading(payload);
            if (reading != null) {
                LOGGER.info("✅ Successfully parsed Reading: " + reading.toString());

                // Save reading to database
                saveReadingToMongo(reading);
                LOGGER.info("✅ Reading saved to database");

                // 🔔 CHECK THRESHOLDS AND GENERATE ALERTS
                if (alertThresholdService != null) {
                    try {
                        List<Alert> alerts = alertThresholdService.checkThresholdsAndGenerateAlerts(reading);

                        if (!alerts.isEmpty()) {
                            LOGGER.info("🚨 Generated " + alerts.size() + " alert(s) for sensor: " + reading.getSensorId());
                            for (Alert alert : alerts) {
                                LOGGER.info("   → " + alert.getType() + " [" + alert.getSeverity() + "]: " + alert.getMessage());
                            }
                        } else {
                            LOGGER.info("✅ All values within normal range - no alerts generated");
                        }
                    } catch (Exception e) {
                        LOGGER.severe("❌ Error checking thresholds: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    LOGGER.warning("⚠️ AlertThresholdService not injected - alerts disabled");
                }
            }

        } catch (Exception e) {
            LOGGER.severe("❌ Error processing MQTT message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Not used for subscriber
    }

    // ==================== HELPER METHODS ====================

    private Reading parseReading(String payload) {
        try {
            // Try to parse as a Reading object directly using Gson with InstantTypeAdapter
            Reading reading = gson.fromJson(payload, Reading.class);

            // Ensure we have a timestamp
            if (reading.getTimestamp() == null) {
                reading.setTimestamp(Instant.now());
            }

            return reading;

        } catch (JsonSyntaxException e) {
            LOGGER.warning("Failed to parse reading with Gson: " + e.getMessage());
            LOGGER.info("Attempting manual parsing...");

            // Try to parse as a generic Document and map manually
            try {
                Document doc = Document.parse(payload);
                Reading reading = new Reading();

                reading.setSensorId(doc.getString("sensorId"));
                reading.setTemperature(getDoubleValue(doc, "temperature"));
                reading.setHumidity(getDoubleValue(doc, "humidity"));
                reading.setCo2(getDoubleValue(doc, "co2"));
                reading.setVoc(getDoubleValue(doc, "voc"));
                reading.setPm25(getDoubleValue(doc, "pm25"));
                reading.setPm10(getDoubleValue(doc, "pm10"));

                // Parse timestamp
                Object timestampObj = doc.get("timestamp");
                if (timestampObj instanceof String) {
                    try {
                        reading.setTimestamp(Instant.parse((String) timestampObj));
                    } catch (Exception ex) {
                        try {
                            // Try with offset format
                            String ts = (String) timestampObj;
                            reading.setTimestamp(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(ts, Instant::from));
                        } catch (Exception ex2) {
                            reading.setTimestamp(Instant.now());
                        }
                    }
                } else {
                    reading.setTimestamp(Instant.now());
                }

                // Parse location if present
                Document locDoc = doc.get("location", Document.class);
                if (locDoc != null) {
                    // Location is handled by the Coordinates nested object if your Reading entity supports it
                    LOGGER.fine("Location data present: " + locDoc.toJson());
                }

                LOGGER.info("✅ Successfully parsed Reading manually");
                return reading;

            } catch (Exception ex) {
                LOGGER.severe("❌ Failed to parse reading manually: " + ex.getMessage());
                ex.printStackTrace();
                return null;
            }
        }
    }

    private double getDoubleValue(Document doc, String key) {
        Object value = doc.get(key);
        if (value == null) return 0.0;  // Return 0.0 for primitive double
        if (value instanceof Double) return (Double) value;
        if (value instanceof Integer) return ((Integer) value).doubleValue();
        if (value instanceof Long) return ((Long) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private void saveReadingToMongo(Reading reading) {
        try {
            MongoCollection<Document> collection = database.getCollection("readings");

            Document doc = new Document()
                    .append("sensorId", reading.getSensorId())
                    .append("temperature", reading.getTemperature())
                    .append("humidity", reading.getHumidity())
                    .append("co2", reading.getCo2())
                    .append("voc", reading.getVoc())
                    .append("pm25", reading.getPm25())
                    .append("pm10", reading.getPm10())
                    .append("timestamp", reading.getTimestamp() != null ?
                            reading.getTimestamp().toString() : Instant.now().toString());

            collection.insertOne(doc);

        } catch (Exception e) {
            LOGGER.severe("❌ Failed to save reading to MongoDB: " + e.getMessage());
            e.printStackTrace();
        }
    }
}