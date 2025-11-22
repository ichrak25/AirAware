# iot/mqtt_publisher.py
import paho.mqtt.client as mqtt
import json
import time
from datetime import datetime, timezone
from config import MQTT_BROKER, MQTT_PORT, MQTT_TOPIC, DEVICE_ID, PUBLISH_INTERVAL
from sensors.mock_sensors import read_all_sensors

def format_payload_for_java(raw_data, device_id):
    """
    Convert Python sensor data to match Java Reading entity structure
    """
    return {
        "sensorId": device_id,  # Changed from device_id
        "temperature": raw_data.get("temperature", 0.0),
        "humidity": raw_data.get("humidity", 0.0),
        "co2": raw_data.get("co2", 0.0),  # Using SGP30 CO2
        "voc": raw_data.get("tvoc", 0.0),  # Map tvoc to voc
        "pm25": raw_data.get("pm25", 0.0),
        "pm10": raw_data.get("pm10", 0.0),
        "timestamp": datetime.now(timezone.utc).isoformat(),  # ISO-8601 format
        # Optional: Add location if needed
        # "location": {
        #     "latitude": 36.8065,
        #     "longitude": 10.1815,
        #     "city": "Tunis",
        #     "country": "Tunisia"
        # }
    }

def main():
    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION1, client_id=DEVICE_ID)

    try:
        client.connect(MQTT_BROKER, MQTT_PORT, 60)
        print(f"[MQTT] Connected to {MQTT_BROKER}:{MQTT_PORT}")
        print(f"[MQTT] Publishing to topic: {MQTT_TOPIC}")
        print(f"[MQTT] Device ID: {DEVICE_ID}")
        print(f"[MQTT] Interval: {PUBLISH_INTERVAL}s\n")
    except Exception as e:
        print(f"[ERROR] Failed to connect: {e}")
        return

    try:
        while True:
            # Read raw sensor data
            raw_data = read_all_sensors()

            # Convert to Java-compatible format
            payload = format_payload_for_java(raw_data, DEVICE_ID)

            # Publish to MQTT
            result = client.publish(MQTT_TOPIC, json.dumps(payload))

            if result.rc == mqtt.MQTT_ERR_SUCCESS:
                print(f"[✓] Published at {payload['timestamp']}")
                print(f"    Temp: {payload['temperature']}°C | "
                      f"Humidity: {payload['humidity']}% | "
                      f"CO2: {payload['co2']}ppm")
                print(f"    PM2.5: {payload['pm25']}µg/m³ | "
                      f"PM10: {payload['pm10']}µg/m³ | "
                      f"VOC: {payload['voc']}\n")
            else:
                print(f"[✗] Publish failed with code: {result.rc}")

            time.sleep(PUBLISH_INTERVAL)

    except KeyboardInterrupt:
        print("\n[STOPPED] Publishing stopped by user.")
    except Exception as e:
        print(f"\n[ERROR] Unexpected error: {e}")
    finally:
        client.disconnect()
        print("[MQTT] Disconnected.")

if __name__ == "__main__":
    main()