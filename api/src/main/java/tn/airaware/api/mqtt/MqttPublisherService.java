package tn.airaware.api.mqtt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.paho.client.mqttv3.*;

@ApplicationScoped
public class MqttPublisherService {

    private static final String COMMAND_TOPIC = "airaware/commands";

    @Inject
    private MqttConnection mqttConnection;

    public void publishMessage(String message) {
        try {
            MqttClient client = mqttConnection.getClient();
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(1);
            client.publish(COMMAND_TOPIC, mqttMessage);
            System.out.println("📤 Published message to " + COMMAND_TOPIC + ": " + message);
        } catch (MqttException e) {
            System.err.println("❌ Publish failed: " + e.getMessage());
        }
    }
}
