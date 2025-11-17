#!/bin/bash

# AirAware System Diagnostics
# Checks all components: MQTT, MongoDB, Java API, Sensors

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

API_BASE="http://localhost:8080/api-1.0-SNAPSHOT/api"
MQTT_HOST="localhost"
MQTT_PORT=1883
MONGO_HOST="localhost"
MONGO_PORT=27017

print_header() {
    echo ""
    echo "======================================================================"
    echo -e "${BLUE}$1${NC}"
    echo "======================================================================"
}

check_service() {
    local service=$1
    local host=$2
    local port=$3

    if nc -z -w2 $host $port 2>/dev/null; then
        echo -e "${GREEN}✓ $service is running${NC} ($host:$port)"
        return 0
    else
        echo -e "${RED}✗ $service is NOT running${NC} ($host:$port)"
        return 1
    fi
}

# ============================================================================
# 1. CHECK INFRASTRUCTURE SERVICES
# ============================================================================

print_header "1️⃣  INFRASTRUCTURE SERVICES"

check_service "MQTT Broker" $MQTT_HOST $MQTT_PORT
mqtt_status=$?

check_service "MongoDB" $MONGO_HOST $MONGO_PORT
mongo_status=$?

check_service "Java API" "localhost" 8080
api_status=$?

# ============================================================================
# 2. CHECK MONGODB DATA
# ============================================================================

print_header "2️⃣  MONGODB DATABASE CHECK"

if [ $mongo_status -eq 0 ]; then
    echo -e "\n${YELLOW}Checking MongoDB collections...${NC}\n"

    mongo_output=$(mongosh --quiet --eval "
        use AirAwareDB;
        print('Tenants: ' + db.tenants.countDocuments());
        print('Sensors: ' + db.sensors.countDocuments());
        print('Readings: ' + db.readings.countDocuments());
        print('Alerts: ' + db.alerts.countDocuments());
    " 2>/dev/null)

    if [ $? -eq 0 ]; then
        echo "$mongo_output"

        # Check if collections are empty
        if echo "$mongo_output" | grep -q "Readings: 0"; then
            echo -e "\n${RED}⚠️  WARNING: No readings in database!${NC}"
            echo "   → MQTT sensors may not be publishing"
            echo "   → Or MqttListenerService is not receiving messages"
        fi

        if echo "$mongo_output" | grep -q "Sensors: 0"; then
            echo -e "\n${RED}⚠️  WARNING: No sensors registered!${NC}"
            echo "   → Run: python register_sensors.py"
        fi
    else
        echo -e "${RED}✗ Could not query MongoDB${NC}"
        echo "   → Check if mongosh is installed"
    fi
else
    echo -e "${RED}✗ Cannot check MongoDB - service not running${NC}"
fi

# ============================================================================
# 3. CHECK JAVA API ENDPOINTS
# ============================================================================

print_header "3️⃣  JAVA API ENDPOINTS"

if [ $api_status -eq 0 ]; then
    echo -e "\n${YELLOW}Testing API endpoints...${NC}\n"

    # Test tenant endpoint
    tenant_response=$(curl -s -o /dev/null -w "%{http_code}" "$API_BASE/tenants")
    if [ "$tenant_response" = "200" ]; then
        tenant_count=$(curl -s "$API_BASE/tenants" | jq '. | length' 2>/dev/null)
        echo -e "${GREEN}✓ Tenants API${NC} - $tenant_count tenant(s)"
    else
        echo -e "${RED}✗ Tenants API${NC} (HTTP $tenant_response)"
    fi

    # Test sensor endpoint
    sensor_response=$(curl -s -o /dev/null -w "%{http_code}" "$API_BASE/sensors")
    if [ "$sensor_response" = "200" ]; then
        sensor_count=$(curl -s "$API_BASE/sensors" | jq '. | length' 2>/dev/null)
        echo -e "${GREEN}✓ Sensors API${NC} - $sensor_count sensor(s)"
    else
        echo -e "${RED}✗ Sensors API${NC} (HTTP $sensor_response)"
    fi

    # Test readings endpoint
    reading_response=$(curl -s -o /dev/null -w "%{http_code}" "$API_BASE/readings")
    if [ "$reading_response" = "200" ]; then
        reading_count=$(curl -s "$API_BASE/readings" | jq '. | length' 2>/dev/null)
        echo -e "${GREEN}✓ Readings API${NC} - $reading_count reading(s)"
    else
        echo -e "${RED}✗ Readings API${NC} (HTTP $reading_response)"
    fi
else
    echo -e "${RED}✗ Cannot test API - service not running${NC}"
fi

# ============================================================================
# 4. CHECK MQTT SUBSCRIPTION
# ============================================================================

print_header "4️⃣  MQTT MESSAGE FLOW"

if [ $mqtt_status -eq 0 ]; then
    echo -e "\n${YELLOW}Checking MQTT topic: airaware/sensors${NC}"
    echo "Listening for 5 seconds..."

    # Subscribe and wait for messages
    timeout 5 mosquitto_sub -h $MQTT_HOST -p $MQTT_PORT -t "airaware/sensors" -C 1 > /tmp/mqtt_test 2>&1

    if [ -s /tmp/mqtt_test ]; then
        echo -e "${GREEN}✓ MQTT messages detected${NC}"
        echo "Sample message:"
        cat /tmp/mqtt_test | jq '.' 2>/dev/null || cat /tmp/mqtt_test
    else
        echo -e "${RED}✗ No MQTT messages received${NC}"
        echo "   → Check if Python sensors are running"
        echo "   → Run: python run_multiple_sensors.py"
    fi
    rm -f /tmp/mqtt_test
else
    echo -e "${RED}✗ Cannot check MQTT - broker not running${NC}"
fi

# ============================================================================
# 5. CHECK JAVA LOGS
# ============================================================================

print_header "5️⃣  JAVA APPLICATION LOGS"

if [ $api_status -eq 0 ]; then
    echo -e "\n${YELLOW}Checking for MQTT listener initialization...${NC}\n"

    # Check if we can find logs
    if command -v docker &> /dev/null; then
        echo "Looking for Docker container logs..."
        # Try common container names
        for container in airaware-api airaware api; do
            if docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
                echo -e "${GREEN}Found container: $container${NC}"
                echo "Recent MQTT-related logs:"
                docker logs $container 2>&1 | grep -i mqtt | tail -5
                break
            fi
        done
    fi

    # Check common log locations
    for log_path in \
        "/var/log/wildfly/server.log" \
        "/opt/wildfly/standalone/log/server.log" \
        "./logs/server.log"; do
        if [ -f "$log_path" ]; then
            echo -e "\n${GREEN}Found log file: $log_path${NC}"
            echo "Recent MQTT logs:"
            grep -i mqtt "$log_path" | tail -10
            break
        fi
    done
else
    echo -e "${RED}✗ Cannot check logs - API not running${NC}"
fi

# ============================================================================
# 6. DIAGNOSIS & RECOMMENDATIONS
# ============================================================================

print_header "6️⃣  DIAGNOSIS & RECOMMENDATIONS"

echo ""

# Calculate issues
issues=0
[ $mqtt_status -ne 0 ] && ((issues++))
[ $mongo_status -ne 0 ] && ((issues++))
[ $api_status -ne 0 ] && ((issues++))

if [ $issues -eq 0 ]; then
    echo -e "${GREEN}✓ All core services are running${NC}\n"

    # Check data flow
    sensor_count=$(curl -s "$API_BASE/sensors" 2>/dev/null | jq '. | length' 2>/dev/null)
    reading_count=$(curl -s "$API_BASE/readings" 2>/dev/null | jq '. | length' 2>/dev/null)

    if [ "$sensor_count" = "0" ]; then
        echo -e "${YELLOW}📋 NEXT STEPS:${NC}"
        echo "   1. Register sensors: python register_sensors.py"
        echo "   2. Start sensors: python run_multiple_sensors.py"
        echo "   3. Wait 10 seconds for data to flow"
        echo "   4. Re-run tests: bash test_airaware_api.sh"
    elif [ "$reading_count" = "0" ]; then
        echo -e "${YELLOW}⚠️  ISSUE: Sensors registered but no readings${NC}\n"
        echo "Possible causes:"
        echo "   1. Python sensors not running"
        echo "      → Check: ps aux | grep run_multiple_sensors"
        echo "      → Start: python run_multiple_sensors.py"
        echo ""
        echo "   2. MqttListenerService not initialized"
        echo "      → Check Java logs for '@PostConstruct CALLED'"
        echo "      → Restart Java application"
        echo ""
        echo "   3. MQTT topic mismatch"
        echo "      → Python publishes to: airaware/sensors"
        echo "      → Java listens on: airaware/sensors"
        echo "      → Verify in config.py and MqttListenerService.java"
    else
        echo -e "${GREEN}✓ System is healthy!${NC}"
        echo "   Sensors: $sensor_count"
        echo "   Readings: $reading_count"
    fi
else
    echo -e "${RED}⚠️  CRITICAL ISSUES DETECTED${NC}\n"

    [ $mqtt_status -ne 0 ] && echo "   → Start MQTT broker: mosquitto -v"
    [ $mongo_status -ne 0 ] && echo "   → Start MongoDB: mongod --dbpath /path/to/data"
    [ $api_status -ne 0 ] && echo "   → Start Java API: mvn quarkus:dev (or your deployment method)"
fi

print_header "DIAGNOSTIC COMPLETE"