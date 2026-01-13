# iot/config.py
"""
Configuration for AirAware IoT MQTT Publisher
"""

# MQTT Broker Configuration
MQTT_BROKER = "localhost"  # Change to your broker address
MQTT_PORT = 1883
MQTT_TOPIC = "airaware/sensors"  # Must match Java MqttListenerService

# MQTT Topics for dynamic sensor management
MQTT_SENSOR_REGISTERED_TOPIC = "airaware/sensors/registered"
MQTT_SENSOR_DELETED_TOPIC = "airaware/sensors/deleted"
MQTT_SENSOR_UPDATED_TOPIC = "airaware/sensors/updated"

# API Configuration
API_BASE_URL = "http://localhost:8080/api-1.0-SNAPSHOT/api"

# Device Configuration
DEVICE_ID = "sensor_001"  # Unique identifier for this sensor

# Publishing Configuration
PUBLISH_INTERVAL = 5  # seconds between readings

# API Polling Configuration (fallback when MQTT notifications fail)
API_POLL_INTERVAL = 60  # seconds between API polls

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