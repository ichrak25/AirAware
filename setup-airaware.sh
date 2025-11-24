#!/bin/bash

# ==================== AirAware Database Setup Script ====================
# This script initializes the MongoDB database with all necessary data

set -e  # Exit on any error

echo ""
echo "🌍 ========================================"
echo "   AirAware Database Setup"
echo "========================================"
echo ""

# Check if MongoDB is running
echo "🔍 Checking MongoDB connection..."
if mongosh --eval "db.version()" > /dev/null 2>&1; then
    echo "✅ MongoDB is running"
else
    echo "❌ MongoDB is not running!"
    echo "   Please start MongoDB first:"
    echo "   sudo systemctl start mongod"
    echo "   OR"
    echo "   brew services start mongodb-community (macOS)"
    exit 1
fi

# Check if init script exists
if [ ! -f "src/main/resources/db/init-airaware.js" ]; then
    echo "❌ Error: init-airaware.js not found in src/main/resources/db/"
    echo "   Please ensure the script exists in the correct location."
    exit 1
fi

echo ""
echo "⚠️  WARNING: This will DELETE all existing data in AirAwareDB!"
echo "   This is intended for DEVELOPMENT use only."
echo ""
read -p "Continue? (y/N): " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "❌ Setup cancelled."
    exit 1
fi

echo ""
echo "🚀 Running MongoDB initialization script..."
echo ""

# Run the initialization script
mongosh mongodb://localhost:27017/AirAwareDB < src/main/resources/db/init-airaware.js

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ ========================================"
    echo "   Database Setup Complete!"
    echo "========================================"
    echo ""
    echo "📋 Next Steps:"
    echo ""
    echo "1. Update SMTP credentials in:"
    echo "   src/main/resources/META-INF/microprofile-config.properties"
    echo ""
    echo "2. Start the AirAware application:"
    echo "   mvn clean package"
    echo "   java -jar target/airaware.war"
    echo ""
    echo "3. Login with default admin:"
    echo "   Username: admin"
    echo "   Password: Admin@123"
    echo "   🔐 CHANGE THIS PASSWORD AFTER FIRST LOGIN!"
    echo ""
    echo "4. Start IoT simulators (optional):"
    echo "   cd iot"
    echo "   python3 sensor_simulator.py"
    echo ""
    echo "5. Access the application:"
    echo "   http://localhost:8080/airaware"
    echo ""
else
    echo ""
    echo "❌ ========================================"
    echo "   Database Setup Failed!"
    echo "========================================"
    echo ""
    echo "Please check the error messages above and try again."
    exit 1
fi