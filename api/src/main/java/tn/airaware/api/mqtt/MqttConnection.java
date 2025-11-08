package tn.airaware.api.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MqttConnection {

    private static final String BROKER_URL = "tcp://localhost:1883";
    private MqttClient client;

    @PostConstruct
    public void initialize() {
        System.out.println("===========================================");
        System.out.println("🔧 MqttConnection @PostConstruct CALLED!");
        System.out.println("===========================================");
    }

    public synchronized MqttClient getClient() throws MqttException {
        System.out.println("🔍 getClient() called - checking connection...");
        if (client == null || !client.isConnected()) {
            System.out.println("🔄 Client not connected, initiating connection...");
            connect();
        }
        return client;
    }

    void connect() throws MqttException {
        try {
            System.out.println("🔄 Attempting to connect to MQTT broker at " + BROKER_URL);
            client = new MqttClient(BROKER_URL, MqttClient.generateClientId());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(60);

            client.connect(options);
            System.out.println("✅ Connected to MQTT broker at " + BROKER_URL);
        } catch (MqttException e) {
            System.err.println("❌ Failed to connect to MQTT broker: " + e.getMessage());
            System.err.println("   Reason code: " + e.getReasonCode());
            System.err.println("   Cause: " + (e.getCause() != null ? e.getCause().getMessage() : "Unknown"));
            throw e; // Re-throw to let caller know connection failed
        }
    }

    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                System.out.println("🔌 Disconnected from MQTT broker.");
            }
        } catch (MqttException e) {
            System.err.println("⚠️ Disconnect error: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        boolean connected = client != null && client.isConnected();
        System.out.println("🔍 isConnected() = " + connected);
        return connected;
    }
}