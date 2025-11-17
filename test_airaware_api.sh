#!/bin/bash

# AirAware API Testing Suite
# Tests all REST endpoints

API_BASE="http://localhost:8080/api-1.0-SNAPSHOT/api"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_header() {
    echo ""
    echo "======================================================================"
    echo -e "${BLUE}$1${NC}"
    echo "======================================================================"
}

test_endpoint() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4

    echo -e "\n${YELLOW}Testing: $description${NC}"
    echo "Endpoint: $method $endpoint"

    if [ -z "$data" ]; then
        response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X $method "$API_BASE$endpoint")
    else
        response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X $method \
            -H "Content-Type: application/json" \
            -d "$data" \
            "$API_BASE$endpoint")
    fi

    http_status=$(echo "$response" | grep "HTTP_STATUS" | cut -d':' -f2)
    body=$(echo "$response" | sed '/HTTP_STATUS/d')

    if [ "$http_status" -ge 200 ] && [ "$http_status" -lt 300 ]; then
        echo -e "${GREEN}✓ SUCCESS (HTTP $http_status)${NC}"
        echo "$body" | jq '.' 2>/dev/null || echo "$body"
    else
        echo -e "${RED}✗ FAILED (HTTP $http_status)${NC}"
        echo "$body"
    fi
}

# ============================================================================
# TENANT TESTS
# ============================================================================

print_header "📋 TENANT API TESTS"

# Create Tenant
test_endpoint POST "/tenants" \
'{
    "organizationName": "Test Organization",
    "contactEmail": "test@example.com",
    "contactPhone": "+216-12-345-678",
    "address": "123 Test Street",
    "country": "Tunisia",
    "active": true
}' \
"Create new tenant"

# Get all tenants
test_endpoint GET "/tenants" "" "Get all tenants"

# Get active tenants
test_endpoint GET "/tenants/active" "" "Get active tenants"

# Get tenants by country
test_endpoint GET "/tenants/country/Tunisia" "" "Get tenants by country"

# Get tenant statistics
test_endpoint GET "/tenants/statistics" "" "Get tenant statistics"

# ============================================================================
# SENSOR TESTS
# ============================================================================

print_header "🔧 SENSOR API TESTS"

# Create Sensor
test_endpoint POST "/sensors" \
'{
    "deviceId": "TEST_SENSOR_001",
    "model": "AirAware Test v1",
    "description": "Test sensor for API validation",
    "status": "ACTIVE",
    "location": {
        "latitude": 36.8065,
        "longitude": 10.1815,
        "altitude": 4.0,
        "city": "Tunis",
        "country": "Tunisia"
    }
}' \
"Register new sensor"

# Get all sensors
test_endpoint GET "/sensors" "" "Get all sensors"

# Get active sensors
test_endpoint GET "/sensors/active" "" "Get active sensors"

# Get sensors by status
test_endpoint GET "/sensors/status/ACTIVE" "" "Get sensors by status"

# Get sensors overview
test_endpoint GET "/sensors/overview" "" "Get sensors overview"

# ============================================================================
# READING TESTS
# ============================================================================

print_header "📊 READING API TESTS"

# Create Reading
test_endpoint POST "/readings" \
'{
    "sensorId": "TEST_SENSOR_001",
    "temperature": 25.5,
    "humidity": 55.0,
    "co2": 850.0,
    "voc": 0.45,
    "pm25": 15.2,
    "pm10": 25.8
}' \
"Create new reading"

# Get all readings
test_endpoint GET "/readings" "" "Get all readings"

# Get recent readings (last 24 hours)
test_endpoint GET "/readings/recent?hours=24" "" "Get recent readings"

# Get readings by sensor
test_endpoint GET "/readings/sensor/TEST_SENSOR_001" "" "Get readings by sensor"

# Get latest reading by sensor
test_endpoint GET "/readings/sensor/TEST_SENSOR_001/latest" "" "Get latest reading"

# Get sensor statistics
test_endpoint GET "/readings/sensor/TEST_SENSOR_001/stats" "" "Get sensor statistics"

# Get air quality summary
test_endpoint GET "/readings/summary" "" "Get air quality summary"

# ============================================================================
# ALERT TESTS
# ============================================================================

print_header "🚨 ALERT API TESTS"

# Create Alert
test_endpoint POST "/alerts" \
'{
    "type": "CO2_HIGH",
    "severity": "WARNING",
    "message": "CO2 levels above 1000 ppm detected",
    "sensorId": "TEST_SENSOR_001"
}' \
"Create new alert"

# Get all alerts
test_endpoint GET "/alerts" "" "Get all alerts"

# Get unresolved alerts
test_endpoint GET "/alerts/unresolved" "" "Get unresolved alerts"

# Get alerts by severity
test_endpoint GET "/alerts/severity/WARNING" "" "Get alerts by severity"

# Get alerts by sensor
test_endpoint GET "/alerts/sensor/TEST_SENSOR_001" "" "Get alerts by sensor"

# ============================================================================
# REAL SENSOR DATA TESTS
# ============================================================================

print_header "🌍 REAL SENSOR DATA TESTS"

# Test with actual sensor IDs
test_endpoint GET "/sensors" "" "Get all registered sensors"
test_endpoint GET "/readings/sensor/SENSOR_TUNIS_001/latest" "" "Get latest reading - Tunis 001"
test_endpoint GET "/readings/sensor/SENSOR_TUNIS_002/latest" "" "Get latest reading - Tunis 002"
test_endpoint GET "/readings/sensor/SENSOR_SFAX_001/latest" "" "Get latest reading - Sfax 001"

# Get health for each sensor
print_header "🏥 SENSOR HEALTH CHECKS"
for sensor_id in SENSOR_TUNIS_001 SENSOR_TUNIS_002 SENSOR_TUNIS_003 SENSOR_SFAX_001 SENSOR_SOUSSE_001; do
    # First get the sensor to get its ID
    response=$(curl -s "$API_BASE/sensors")
    sensor=$(echo "$response" | jq -r ".[] | select(.deviceId==\"$sensor_id\") | .id" 2>/dev/null)

    if [ ! -z "$sensor" ]; then
        test_endpoint GET "/sensors/$sensor/health" "" "Health check - $sensor_id"
    else
        echo -e "${YELLOW}⚠ Sensor $sensor_id not found${NC}"
    fi
done

# ============================================================================
# SUMMARY
# ============================================================================

print_header "📈 DATA SUMMARY"

echo -e "\n${BLUE}Getting final statistics...${NC}\n"

echo "Tenant Summary:"
curl -s "$API_BASE/tenants/statistics" | jq '.'

echo -e "\nSensor Summary:"
curl -s "$API_BASE/sensors/overview" | jq '.'

echo -e "\nAir Quality Summary:"
curl -s "$API_BASE/readings/summary" | jq '.'

print_header "✅ TESTING COMPLETE"

echo -e "\n${GREEN}All tests completed!${NC}"
echo -e "Check the results above for any ${RED}FAILED${NC} tests.\n"