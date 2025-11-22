#!/usr/bin/env python3
"""
Register sensors with the AirAware API before running them
"""
import requests
import json

API_BASE_URL = "http://localhost:8080/api-1.0-SNAPSHOT/api"

# First, create a tenant
TENANT = {
    "organizationName": "Tunisia Environmental Monitoring",
    "contactEmail": "admin@tem.tn",
    "contactPhone": "+216-71-123-456",
    "address": "Avenue Habib Bourguiba, Tunis",
    "country": "Tunisia",
    "active": True
}

# Sensors to register
SENSORS = [
    {
        "deviceId": "SENSOR_TUNIS_001",
        "model": "AirAware Pro v1",
        "description": "Downtown Tunis Office - High traffic area monitoring",
        "status": "ACTIVE",
        "location": {
            "latitude": 36.8065,
            "longitude": 10.1815,
            "altitude": 4.0,
            "city": "Tunis",
            "country": "Tunisia"
        }
    },
    {
        "deviceId": "SENSOR_TUNIS_002",
        "model": "AirAware Pro v1",
        "description": "Carthage Industrial Zone - Factory emissions monitoring",
        "status": "ACTIVE",
        "location": {
            "latitude": 36.8500,
            "longitude": 10.1658,
            "altitude": 10.0,
            "city": "Tunis",
            "country": "Tunisia"
        }
    },
    {
        "deviceId": "SENSOR_TUNIS_003",
        "model": "AirAware Lite v2",
        "description": "La Marsa Residential Area - Residential air quality",
        "status": "ACTIVE",
        "location": {
            "latitude": 36.7525,
            "longitude": 10.2084,
            "altitude": 2.0,
            "city": "Tunis",
            "country": "Tunisia"
        }
    },
    {
        "deviceId": "SENSOR_SFAX_001",
        "model": "AirAware Pro v1",
        "description": "Sfax Port Area - Maritime and port pollution tracking",
        "status": "ACTIVE",
        "location": {
            "latitude": 34.7406,
            "longitude": 10.7603,
            "altitude": 8.0,
            "city": "Sfax",
            "country": "Tunisia"
        }
    },
    {
        "deviceId": "SENSOR_SOUSSE_001",
        "model": "AirAware Lite v2",
        "description": "Sousse City Center - Tourist area air quality",
        "status": "ACTIVE",
        "location": {
            "latitude": 35.8256,
            "longitude": 10.6369,
            "altitude": 3.0,
            "city": "Sousse",
            "country": "Tunisia"
        }
    }
]


def register_tenant():
    """Create the tenant organization"""
    print("=" * 70)
    print("📋 Registering Tenant Organization")
    print("=" * 70)

    try:
        response = requests.post(
            f"{API_BASE_URL}/tenants",
            json=TENANT,
            headers={"Content-Type": "application/json"}
        )

        if response.status_code == 201:
            tenant_data = response.json()
            print(f"✓ Tenant created successfully!")
            print(f"  Organization: {tenant_data['organizationName']}")
            print(f"  ID: {tenant_data['id']}")
            return tenant_data
        elif response.status_code == 409:
            print(f"⚠ Tenant already exists, fetching existing...")
            # Get existing tenant
            response = requests.get(
                f"{API_BASE_URL}/tenants/organization/{TENANT['organizationName']}"
            )
            if response.status_code == 200:
                return response.json()
        else:
            print(f"✗ Failed to create tenant: {response.status_code}")
            print(f"  Response: {response.text}")
            return None

    except Exception as e:
        print(f"✗ Error: {e}")
        return None


def register_sensors(tenant):
    """Register all sensors with the tenant"""
    print("\n" + "=" * 70)
    print("🔧 Registering Sensors")
    print("=" * 70)

    registered = 0
    failed = 0

    for sensor in SENSORS:
        # Add tenant reference
        sensor["tenant"] = {
            "organizationName": tenant["organizationName"],
            "contactEmail": tenant["contactEmail"],
            "contactPhone": tenant["contactPhone"]
        }

        try:
            response = requests.post(
                f"{API_BASE_URL}/sensors",
                json=sensor,
                headers={"Content-Type": "application/json"}
            )

            if response.status_code == 201:
                sensor_data = response.json()
                print(f"✓ {sensor['deviceId']}")
                print(f"  Location: {sensor['location']['city']}")
                print(f"  Model: {sensor['model']}")
                print(f"  ID: {sensor_data['id']}\n")
                registered += 1
            else:
                print(f"✗ {sensor['deviceId']} - Failed: {response.status_code}")
                print(f"  {response.text}\n")
                failed += 1

        except Exception as e:
            print(f"✗ {sensor['deviceId']} - Error: {e}\n")
            failed += 1

    print("=" * 70)
    print(f"Registration Summary: {registered} successful, {failed} failed")
    print("=" * 70)


def verify_registration():
    """Verify all sensors are registered"""
    print("\n" + "=" * 70)
    print("🔍 Verifying Registration")
    print("=" * 70)

    try:
        # Get all sensors
        response = requests.get(f"{API_BASE_URL}/sensors")
        if response.status_code == 200:
            sensors = response.json()
            print(f"✓ Total sensors in database: {len(sensors)}\n")

            for sensor in sensors:
                status_icon = "🟢" if sensor['status'] == "ACTIVE" else "🔴"
                print(f"{status_icon} {sensor['deviceId']}")
                print(f"   {sensor['description']}")
                print(f"   Location: {sensor['location']['city']}, "
                      f"{sensor['location']['country']}\n")
        else:
            print(f"✗ Failed to fetch sensors: {response.status_code}")

        # Get sensors overview
        response = requests.get(f"{API_BASE_URL}/sensors/overview")
        if response.status_code == 200:
            overview = response.json()
            print("=" * 70)
            print("📊 Sensors Overview")
            print("=" * 70)
            print(f"Total Sensors: {overview['totalSensors']}")
            print(f"Active: {overview['activeSensors']}")
            print(f"Inactive: {overview['inactiveSensors']}")
            print(f"Offline: {overview['offlineSensors']}")
            print("=" * 70)

    except Exception as e:
        print(f"✗ Error: {e}")


def main():
    print("\n🌍 AirAware Sensor Registration Tool\n")

    # Step 1: Register tenant
    tenant = register_tenant()
    if not tenant:
        print("\n✗ Failed to register tenant. Exiting.")
        return

    # Step 2: Register sensors
    register_sensors(tenant)

    # Step 3: Verify
    verify_registration()

    print("\n✅ Registration complete! You can now start the sensors.")
    print("   Run: python run_multiple_sensors.py\n")


if __name__ == "__main__":
    main()