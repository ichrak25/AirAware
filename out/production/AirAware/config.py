# iot/config.py
"""
Configuration for AirAware IoT MQTT Publisher
"""

# MQTT Broker Configuration
MQTT_BROKER = "localhost"  # Change to your broker address
MQTT_PORT = 1883
MQTT_TOPIC = "airaware/sensors"  # Must match Java MqttListenerService

# Device Configuration
DEVICE_ID = "sensor_001"  # Unique identifier for this sensor

# Publishing Configuration
PUBLISH_INTERVAL = 5  # seconds between readings

# Sensor Location (Optional - uncomment to include)
SENSOR_LOCATION = {
    "latitude": 36.8065,
    "longitude": 10.1815,
    "altitude": 4.0,
    "city": "Tunis",
    "country": "Tunisia"
}

# Enable/Disable location in payload
INCLUDE_LOCATION = False