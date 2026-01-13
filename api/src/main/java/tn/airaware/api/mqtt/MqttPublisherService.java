package tn.airaware.api.mqtt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.eclipse.paho.client.mqttv3.*;
import tn.airaware.api.entities.Sensor;

@ApplicationScoped
public class MqttPublisherService {

    private static final String COMMAND_TOPIC = "airaware/commands";
    private static final String SENSOR_REGISTERED_TOPIC = "airaware/sensors/registered";
    private static final String SENSOR_DELETED_TOPIC = "airaware/sensors/deleted";
    private static final String SENSOR_UPDATED_TOPIC = "airaware/sensors/updated";

    @Inject
    private MqttConnection mqttConnection;

    /**
     * Publish a generic message to the commands topic
     */
    public void publishMessage(String message) {
        publishToTopic(COMMAND_TOPIC, message);
    }

    /**
     * Notify IoT devices that a new sensor has been registered
     * This triggers the dynamic sensor simulator to start publishing data for this sensor
     */
    public void notifySensorRegistered(Sensor sensor) {
        try {
            Jsonb jsonb = JsonbBuilder.create();
            String sensorJson = jsonb.toJson(sensor);
            publishToTopic(SENSOR_REGISTERED_TOPIC, sensorJson);
            System.out.println("📤 Sensor registration notification sent: " + sensor.getDeviceId());
        } catch (Exception e) {
            System.err.println("❌ Failed to serialize sensor: " + e.getMessage());
        }
    }

    /**
     * Notify IoT devices that a sensor has been deleted
     * This triggers the dynamic sensor simulator to stop publishing data for this sensor
     */
    public void notifySensorDeleted(String deviceId) {
        String message = String.format("{\"deviceId\": \"%s\", \"action\": \"deleted\"}", deviceId);
        publishToTopic(SENSOR_DELETED_TOPIC, message);
        System.out.println("📤 Sensor deletion notification sent: " + deviceId);
    }

    /**
     * Notify IoT devices that a sensor has been updated (e.g., status changed)
     */
    public void notifySensorUpdated(Sensor sensor) {
        try {
            Jsonb jsonb = JsonbBuilder.create();
            String sensorJson = jsonb.toJson(sensor);
            publishToTopic(SENSOR_UPDATED_TOPIC, sensorJson);
            System.out.println("📤 Sensor update notification sent: " + sensor.getDeviceId());
        } catch (Exception e) {
            System.err.println("❌ Failed to serialize sensor: " + e.getMessage());
        }
    }

    /**
     * Publish a message to a specific MQTT topic
     */
    private void publishToTopic(String topic, String message) {
        try {
            MqttClient client = mqttConnection.getClient();
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(1);
            client.publish(topic, mqttMessage);
            System.out.println("📤 Published to " + topic);
        } catch (MqttException e) {
            System.err.println("❌ Publish to " + topic + " failed: " + e.getMessage());
        }
    }
}
