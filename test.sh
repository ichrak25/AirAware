#!/bin/bash
# ============================================================================
# AirAware Alert System - Test Script
# ============================================================================
# Usage: ./test-alerts.sh
# Make sure MQTT Mosquitto and WildFly are running first!
# ============================================================================

echo "═══════════════════════════════════════════════════════════"
echo "🧪 AirAware Alert System - Test Suite"
echo "═══════════════════════════════════════════════════════════"
echo ""

# Check if mosquitto_pub is available
if ! command -v mosquitto_pub &> /dev/null; then
    echo "❌ mosquitto_pub not found. Install Mosquitto clients first."
    exit 1
fi

TIMESTAMP=$(date -u +%Y-%m-%dT%H:%M:%SZ)

echo "📤 Test 1: Sending PM2.5 CRITICAL Alert (value=275)..."
mosquitto_pub -h localhost -t "airaware/sensors" -m "{
  \"sensorId\": \"SENSOR_TEST_PM25\",
  \"temperature\": 25.5,
  \"humidity\": 65.0,
  \"co2\": 450,
  \"voc\": 0.3,
  \"pm25\": 275.0,
  \"pm10\": 50.0,
  \"timestamp\": \"$TIMESTAMP\"
}"
echo "✅ Sent! Check WildFly logs for: PM25_VERY_UNHEALTHY - CRITICAL"
echo ""
sleep 2

echo "📤 Test 2: Sending CO2 WARNING Alert (value=2500)..."
mosquitto_pub -h localhost -t "airaware/sensors" -m "{
  \"sensorId\": \"SENSOR_TEST_CO2\",
  \"temperature\": 25.5,
  \"humidity\": 65.0,
  \"co2\": 2500,
  \"voc\": 0.3,
  \"pm25\": 15.0,
  \"pm10\": 30.0,
  \"timestamp\": \"$TIMESTAMP\"
}"
echo "✅ Sent! Check WildFly logs for: CO2_HIGH - WARNING"
echo ""
sleep 2

echo "📤 Test 3: Sending Temperature CRITICAL Alert (value=42)..."
mosquitto_pub -h localhost -t "airaware/sensors" -m "{
  \"sensorId\": \"SENSOR_TEST_TEMP\",
  \"temperature\": 42.0,
  \"humidity\": 65.0,
  \"co2\": 450,
  \"voc\": 0.3,
  \"pm25\": 15.0,
  \"pm10\": 30.0,
  \"timestamp\": \"$TIMESTAMP\"
}"
echo "✅ Sent! Check WildFly logs for: TEMPERATURE_EXTREME - CRITICAL"
echo ""
sleep 2

echo "📤 Test 4: Sending VOC CRITICAL Alert (value=3.5)..."
mosquitto_pub -h localhost -t "airaware/sensors" -m "{
  \"sensorId\": \"SENSOR_TEST_VOC\",
  \"temperature\": 25.0,
  \"humidity\": 65.0,
  \"co2\": 450,
  \"voc\": 3.5,
  \"pm25\": 15.0,
  \"pm10\": 30.0,
  \"timestamp\": \"$TIMESTAMP\"
}"
echo "✅ Sent! Check WildFly logs for: VOC_DANGEROUS - CRITICAL"
echo ""
sleep 2

echo "📤 Test 5: Sending Normal Reading (no alerts expected)..."
mosquitto_pub -h localhost -t "airaware/sensors" -m "{
  \"sensorId\": \"SENSOR_TEST_NORMAL\",
  \"temperature\": 22.0,
  \"humidity\": 55.0,
  \"co2\": 400,
  \"voc\": 0.2,
  \"pm25\": 8.0,
  \"pm10\": 15.0,
  \"timestamp\": \"$TIMESTAMP\"
}"
echo "✅ Sent! No alerts should be generated for normal values."
echo ""

echo "═══════════════════════════════════════════════════════════"
echo "🔍 Checking alerts via API..."
echo "═══════════════════════════════════════════════════════════"
echo ""

echo "📊 Fetching all alerts..."
curl -s http://localhost:8080/api-1.0-SNAPSHOT/api/alerts | head -c 500
echo ""
echo ""

echo "═══════════════════════════════════════════════════════════"
echo "✅ Test Complete!"
echo "═══════════════════════════════════════════════════════════"
echo ""
echo "📋 Next Steps:"
echo "   1. Check WildFly console for alert logs"
echo "   2. Check MongoDB: mongosh AirAwareDB --eval 'db.alerts.find().pretty()'"
echo "   3. Check Frontend: http://localhost:5173/alerts"
echo "   4. Check email inbox (if EMAIL_NOTIFICATIONS_ENABLED=true)"
echo ""