#!/usr/bin/env python3
"""
AirAware Multi-Sensor Launcher
Starts multiple simulated sensors publishing to MQTT
"""
import multiprocessing
import time
import paho.mqtt.client as mqtt
import json
from datetime import datetime, timezone
from sensors.mock_sensors import read_all_sensors

# MQTT Configuration
MQTT_BROKER = "localhost"
MQTT_PORT = 1883
MQTT_TOPIC = "airaware/sensors"
PUBLISH_INTERVAL = 5  # seconds

# Define multiple sensors with different locations
SENSORS = [
    {
        "device_id": "SENSOR_TUNIS_001",
        "location": {
            "latitude": 36.8065,
            "longitude": 10.1815,
            "altitude": 4.0,
            "city": "Tunis",
            "country": "Tunisia"
        },
        "description": "Downtown Tunis Office",
        "model": "AirAware Pro v1"
    },
    {
        "device_id": "SENSOR_TUNIS_002",
        "location": {
            "latitude": 36.8500,
            "longitude": 10.1658,
            "altitude": 10.0,
            "city": "Tunis",
            "country": "Tunisia"
        },
        "description": "Carthage Industrial Zone",
        "model": "AirAware Pro v1"
    },
    {
        "device_id": "SENSOR_TUNIS_003",
        "location": {
            "latitude": 36.7525,
            "longitude": 10.2084,
            "altitude": 2.0,
            "city": "Tunis",
            "country": "Tunisia"
        },
        "description": "La Marsa Residential Area",
        "model": "AirAware Lite v2"
    },
    {
        "device_id": "SENSOR_SFAX_001",
        "location": {
            "latitude": 34.7406,
            "longitude": 10.7603,
            "altitude": 8.0,
            "city": "Sfax",
            "country": "Tunisia"
        },
        "description": "Sfax Port Area",
        "model": "AirAware Pro v1"
    },
    {
        "device_id": "SENSOR_SOUSSE_001",
        "location": {
            "latitude": 35.8256,
            "longitude": 10.6369,
            "altitude": 3.0,
            "city": "Sousse",
            "country": "Tunisia"
        },
        "description": "Sousse City Center",
        "model": "AirAware Lite v2"
    }
]


def format_payload(raw_data, sensor_config):
    """Convert sensor data to Java Reading format"""
    return {
        "sensorId": sensor_config["device_id"],
        "temperature": raw_data.get("temperature", 0.0),
        "humidity": raw_data.get("humidity", 0.0),
        "co2": raw_data.get("co2", 0.0),
        "voc": raw_data.get("tvoc", 0.0),
        "pm25": raw_data.get("pm25", 0.0),
        "pm10": raw_data.get("pm10", 0.0),
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "location": sensor_config["location"]
    }


def publish_sensor_data(sensor_config):
    """Run a single sensor publisher"""
    device_id = sensor_config["device_id"]

    # Create MQTT client
    client = mqtt.Client(
        mqtt.CallbackAPIVersion.VERSION1,
        client_id=device_id
    )

    try:
        client.connect(MQTT_BROKER, MQTT_PORT, 60)
        print(f"[{device_id}] ✓ Connected to MQTT broker")
        print(f"[{device_id}] Location: {sensor_config['location']['city']}")
        print(f"[{device_id}] Model: {sensor_config['model']}\n")
    except Exception as e:
        print(f"[{device_id}] ✗ Connection failed: {e}")
        return

    try:
        while True:
            # Read sensor data
            raw_data = read_all_sensors()

            # Format for Java backend
            payload = format_payload(raw_data, sensor_config)

            # Publish to MQTT
            result = client.publish(MQTT_TOPIC, json.dumps(payload))

            if result.rc == mqtt.MQTT_ERR_SUCCESS:
                print(f"[{device_id}] 📡 T:{payload['temperature']:.1f}°C "
                      f"H:{payload['humidity']:.1f}% "
                      f"CO2:{payload['co2']:.0f}ppm "
                      f"PM2.5:{payload['pm25']:.1f}µg/m³")
            else:
                print(f"[{device_id}] ✗ Publish failed: {result.rc}")

            time.sleep(PUBLISH_INTERVAL)

    except KeyboardInterrupt:
        print(f"\n[{device_id}] Stopped by user")
    except Exception as e:
        print(f"\n[{device_id}] Error: {e}")
    finally:
        client.disconnect()
        print(f"[{device_id}] Disconnected")


def main():
    print("=" * 70)
    print("🌍 AirAware Multi-Sensor Simulator")
    print("=" * 70)
    print(f"Starting {len(SENSORS)} sensors...")
    print(f"MQTT Broker: {MQTT_BROKER}:{MQTT_PORT}")
    print(f"Topic: {MQTT_TOPIC}")
    print(f"Publish Interval: {PUBLISH_INTERVAL}s")
    print("=" * 70)
    print()

    # Create process for each sensor
    processes = []
    for sensor in SENSORS:
        p = multiprocessing.Process(
            target=publish_sensor_data,
            args=(sensor,)
        )
        p.start()
        processes.append(p)
        time.sleep(0.5)  # Stagger startup

    print(f"\n✓ All {len(SENSORS)} sensors started!")
    print("Press Ctrl+C to stop all sensors\n")

    try:
        # Keep main process alive
        for p in processes:
            p.join()
    except KeyboardInterrupt:
        print("\n\n🛑 Stopping all sensors...")
        for p in processes:
            p.terminate()
            p.join()
        print("✓ All sensors stopped")


if __name__ == "__main__":
    main()