# iot/mqtt_publisher.py
import paho.mqtt.client as mqtt
import json
import time
from config import MQTT_BROKER, MQTT_PORT, MQTT_TOPIC, DEVICE_ID, PUBLISH_INTERVAL
from sensors.mock_sensors import read_all_sensors

def main():
    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION1, client_id=DEVICE_ID)

    client.connect(MQTT_BROKER, MQTT_PORT, 60)
    print(f"[MQTT] Connected to {MQTT_BROKER}:{MQTT_PORT}")

    try:
        while True:
            data = read_all_sensors()
            payload = {"device_id": DEVICE_ID, **data}
            client.publish(MQTT_TOPIC, json.dumps(payload))
            print("[PUBLISHED]", payload)
            time.sleep(PUBLISH_INTERVAL)
    except KeyboardInterrupt:
        print("\n[STOPPED] Publishing stopped.")
        client.disconnect()

if __name__ == "__main__":
    main()
