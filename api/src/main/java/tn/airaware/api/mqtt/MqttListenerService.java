package tn.airaware.api.mqtt;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.paho.client.mqttv3.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import tn.airaware.api.entities.Reading;
import tn.airaware.api.services.ReadingService;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;  // ADD THIS IMPORT

@ApplicationScoped
public class MqttListenerService implements MqttCallback {

    private static final String TOPIC = "airaware/sensors";

    @Inject
    private MqttConnection mqttConnection;

    @Inject
    private ReadingService readingService;

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    // This ensures the bean is initialized eagerly when the application starts
    public void onStart(@Observes @Initialized(ApplicationScoped.class) Object init) {
        System.out.println("===========================================");
        System.out.println("🎯 MQTT LISTENER - APPLICATION CONTEXT INITIALIZED");
        System.out.println("===========================================");
    }

    @PostConstruct
    public void init() {
        System.out.println("===========================================");
        System.out.println("🚀 MqttListenerService @PostConstruct CALLED!");
        System.out.println("===========================================");

        try {
            System.out.println("🔄 Attempting to get MQTT client...");

            if (mqttConnection == null) {
                System.err.println("❌ ERROR: MqttConnection is NULL! Not injected!");
                return;
            }

            MqttClient client = mqttConnection.getClient();

            if (client == null) {
                System.err.println("❌ MQTT client is null!");
                return;
            }

            if (!client.isConnected()) {
                System.err.println("⚠️ MQTT client exists but is not connected!");
                return;
            }

            System.out.println("✅ MQTT client is connected, setting callback...");
            client.setCallback(this);

            System.out.println("🔄 Subscribing to topic: " + TOPIC);
            client.subscribe(TOPIC, 1); // QoS 1

            System.out.println("✅ Successfully subscribed to topic: " + TOPIC);
            System.out.println("📡 Waiting for messages on topic: " + TOPIC);
            System.out.println("===========================================");

        } catch (MqttException e) {
            System.err.println("❌ MQTT subscription failed!");
            System.err.println("   Error message: " + e.getMessage());
            System.err.println("   Reason code: " + e.getReasonCode());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ Unexpected error during MQTT initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void cleanup() {
        System.out.println("🔄 Cleaning up MQTT listener...");
        if (mqttConnection != null) {
            mqttConnection.disconnect();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("⚠️ MQTT Connection lost!");
        System.err.println("   Cause: " + cause.getMessage());
        cause.printStackTrace();
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        try {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            System.out.println("===========================================");
            System.out.println("📥 MQTT MESSAGE RECEIVED!");
            System.out.println("   Topic: " + topic);
            System.out.println("   Payload: " + payload);
            System.out.println("   QoS: " + message.getQos());
            System.out.println("===========================================");

            Reading reading = mapper.readValue(payload, Reading.class);
            System.out.println("✅ Successfully parsed Reading: " + reading);

            readingService.saveReading(reading);
            System.out.println("✅ Reading saved to database");

        } catch (Exception e) {
            System.err.println("❌ Failed to process message from topic: " + topic);
            System.err.println("   Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        System.out.println("📤 Message delivery complete");
    }
}