# 🌬️ AirAware - Air Quality Monitoring System

<p align="center">
  <img src="docs/images/logo.png" alt="AirAware Logo" width="200"/>
</p>

<p align="center">
  <strong>Real-time air quality monitoring system for Tunisia</strong>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#architecture">Architecture</a> •
  <a href="#installation">Installation</a> •
  <a href="#usage">Usage</a> •
  <a href="#api-documentation">API</a> •
  <a href="#contributing">Contributing</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17+-orange?style=flat-square&logo=java" alt="Java 17+"/>
  <img src="https://img.shields.io/badge/React-18+-blue?style=flat-square&logo=react" alt="React 18+"/>
  <img src="https://img.shields.io/badge/MongoDB-7.0+-green?style=flat-square&logo=mongodb" alt="MongoDB"/>
  <img src="https://img.shields.io/badge/MQTT-Mosquitto-purple?style=flat-square" alt="MQTT"/>
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=flat-square" alt="License"/>
</p>

---

## 📖 Overview

**AirAware** is a comprehensive IoT-based air quality monitoring system designed for Tunisia. It collects real-time environmental data from Raspberry Pi sensors deployed across multiple cities, processes and stores the data, and presents it through a modern Progressive Web Application (PWA).

### 🎯 Problem Statement

Tunisia faces significant air quality challenges due to:
- Industrial emissions (Sfax, Gabes chemical zones)
- Urban traffic pollution (Tunis, Sousse)
- Limited real-time monitoring infrastructure
- Lack of accessible public air quality data

### 💡 Solution

AirAware provides:
- **Real-time monitoring** of PM2.5, PM10, CO2, VOC, temperature, and humidity
- **Strategic sensor placement** across 5 Tunisian cities
- **Secure, scalable architecture** using industry-standard technologies
- **Accessible PWA** that works on any device, even offline

---

## ✨ Features

### 🌡️ Environmental Monitoring
- Real-time air quality data collection
- PM2.5 and PM10 particulate matter tracking
- CO2 and VOC gas monitoring
- Temperature and humidity sensors
- AQI (Air Quality Index) calculation using EPA standards

### 📱 Progressive Web Application
- Installable on mobile and desktop
- Offline support with service workers
- Real-time dashboard with live updates
- Interactive map with sensor locations
- Historical data charts and analytics
- Dark mode support
- Responsive design

### 🔐 Security
- OAuth 2.0 with PKCE authorization flow
- JWT tokens signed with Ed25519 (quantum-resistant)
- Argon2 password hashing
- Role-Based Access Control (RBAC)
- Email verification with activation codes

### 🚨 Alerting System
- Configurable threshold-based alerts
- Real-time notifications
- Alert severity levels (Info, Warning, Critical)
- Alert acknowledgment and resolution

### 📊 Analytics
- Historical data visualization
- Trend analysis
- Data export capabilities
- Predictive analytics (MLOps module)

---

## 🏗️ Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Raspberry Pi   │────▶│   MQTT Broker   │────▶│    WildFly      │
│  (Sensors)      │     │  (Mosquitto)    │     │  (Java EE API)  │
└─────────────────┘     └─────────────────┘     └────────┬────────┘
                                                         │
                                                         ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   React PWA     │◀───▶│   REST API      │◀───▶│    MongoDB      │
│   (Frontend)    │     │   (JAX-RS)      │     │   (Database)    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

### System Modules

| Module | Description | Technology |
|--------|-------------|------------|
| **IoT Sensors** | Data collection from physical sensors | Raspberry Pi 4, Python, BME680, PMS5003 |
| **MQTT Broker** | Message queuing for sensor data | Eclipse Mosquitto 2.0+ |
| **API Module** | RESTful API for data management | Java EE, JAX-RS, WildFly 38 |
| **IAM Module** | Identity and access management | OAuth 2.0, JWT, Argon2 |
| **PWA Frontend** | User interface | React 18, Vite, Tailwind CSS |
| **MLOps** | Machine learning predictions | Python, FastAPI, scikit-learn |
| **Database** | Data persistence | MongoDB 7.0+ |

---

## 🚀 Installation

### Prerequisites

- **Java 17+** (OpenJDK or Oracle JDK)
- **Node.js 18+** and npm
- **MongoDB 7.0+**
- **Eclipse Mosquitto 2.0+**
- **WildFly 38.0.0**
- **Python 3.10+** (for Raspberry Pi and MLOps)
- **Maven 3.8+**

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/airaware.git
cd airaware
```

### 2. Database Setup

```bash
# Start MongoDB
mongod --dbpath /data/db

# Create database and collections
mongosh
use AirAwareDB
db.createCollection("sensors")
db.createCollection("readings")
db.createCollection("alerts")
db.createCollection("identities")
```

### 3. MQTT Broker Setup

```bash
# Install Mosquitto (Ubuntu/Debian)
sudo apt-get install mosquitto mosquitto-clients

# Start Mosquitto
sudo systemctl start mosquitto

# Or on Windows
net start Mosquitto
```

### 4. Backend Setup (API & IAM Modules)

```bash
# Build API module
cd api
mvn clean package -DskipTests
cp target/api-1.0-SNAPSHOT.war $JBOSS_HOME/standalone/deployments/

# Build IAM module
cd ../iam
mvn clean package -DskipTests
cp target/iam-1.0-SNAPSHOT.war $JBOSS_HOME/standalone/deployments/

# Start WildFly
$JBOSS_HOME/bin/standalone.sh
```

### 5. Frontend Setup (PWA)

```bash
cd airaware-pwa

# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build
```

### 6. Raspberry Pi Setup (Sensors)

```bash
# On Raspberry Pi
cd raspberry-pi

# Install dependencies
pip install -r requirements.txt

# Configure MQTT broker address
nano config.yaml
# Set broker: YOUR_PC_IP_ADDRESS

# Start sensor data collection
python mqtt_publisher.py
```

---

## 📁 Project Structure

```
airaware/
├── api/                          # API Module (Java EE)
│   ├── src/main/java/
│   │   └── com/airaware/
│   │       ├── entity/           # JPA Entities
│   │       ├── repository/       # Data Access Layer
│   │       ├── resource/         # REST Controllers
│   │       ├── service/          # Business Logic
│   │       └── mqtt/             # MQTT Listener
│   └── pom.xml
│
├── iam/                          # IAM Module (Authentication)
│   ├── src/main/java/
│   │   └── com/airaware/iam/
│   │       ├── entity/           # Identity, Role entities
│   │       ├── resource/         # Auth endpoints
│   │       ├── service/          # Auth services
│   │       └── filter/           # JWT, CORS filters
│   └── pom.xml
│
├── airaware-pwa/                 # Frontend (React PWA)
│   ├── src/
│   │   ├── components/           # Reusable UI components
│   │   ├── pages/                # Page components
│   │   ├── services/             # API service layer
│   │   ├── context/              # React context providers
│   │   └── utils/                # Helper functions
│   ├── public/                   # Static assets
│   ├── vite.config.js            # Vite configuration
│   └── package.json
│
├── raspberry-pi/                 # IoT Sensor Code
│   ├── mqtt_publisher.py         # Main sensor script
│   ├── sensors/                  # Sensor drivers
│   ├── config.yaml               # Configuration
│   └── requirements.txt
│
├── mlops/                        # Machine Learning Module
│   ├── api/                      # FastAPI endpoints
│   ├── models/                   # ML models
│   └── requirements.txt
│
└── docs/                         # Documentation
    ├── api/                      # API documentation
    ├── deployment/               # Deployment guides
    └── images/                   # Screenshots and diagrams
```

---

## 🖥️ Usage

### Starting the System

1. **Start MongoDB:**
   ```bash
   # Windows
   net start MongoDB
   
   # Linux
   sudo systemctl start mongod
   ```

2. **Start MQTT Broker:**
   ```bash
   # Windows
   net start Mosquitto
   
   # Linux
   sudo systemctl start mosquitto
   ```

3. **Start WildFly:**
   ```bash
   # Windows
   %JBOSS_HOME%\bin\standalone.bat
   
   # Linux
   $JBOSS_HOME/bin/standalone.sh
   ```

4. **Start PWA:**
   ```bash
   cd airaware-pwa
   npm run dev
   ```

5. **Start Raspberry Pi Sensors:**
   ```bash
   python mqtt_publisher.py
   ```

### Accessing the Application

- **PWA Dashboard:** http://localhost:5173
- **API Endpoints:** http://localhost:8080/api-1.0-SNAPSHOT/api
- **IAM Endpoints:** http://localhost:8080/iam-1.0-SNAPSHOT/api

### Default Credentials

After registration, activate your account using the code from WildFly logs.

---

## 📡 API Documentation

### Authentication

#### Register User
```http
POST /iam-1.0-SNAPSHOT/api/register
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

#### Activate Account
```http
POST /iam-1.0-SNAPSHOT/api/register/activate
Content-Type: application/json

{
  "email": "john@example.com",
  "activationCode": "123456"
}
```

#### Login
```http
POST /iam-1.0-SNAPSHOT/api/oauth/login
Content-Type: application/json

{
  "username": "john_doe",
  "password": "SecurePass123!"
}
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJFZERTQSIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 3600
}
```

### Sensors

#### Get All Sensors
```http
GET /api-1.0-SNAPSHOT/api/sensors
Authorization: Bearer <token>
```

#### Register New Sensor
```http
POST /api-1.0-SNAPSHOT/api/sensors
Authorization: Bearer <token>
Content-Type: application/json

{
  "sensorId": "SENSOR_TUNIS_001",
  "name": "Tunis Downtown",
  "description": "Downtown monitoring station",
  "status": "ACTIVE",
  "location": {
    "latitude": 36.8065,
    "longitude": 10.1815,
    "city": "Tunis",
    "country": "Tunisia"
  }
}
```

### Readings

#### Get Latest Readings
```http
GET /api-1.0-SNAPSHOT/api/readings?limit=50
Authorization: Bearer <token>
```

#### Get Readings by Sensor
```http
GET /api-1.0-SNAPSHOT/api/readings/sensor/SENSOR_TUNIS_001
Authorization: Bearer <token>
```

### Alerts

#### Get All Alerts
```http
GET /api-1.0-SNAPSHOT/api/alerts
Authorization: Bearer <token>
```

#### Resolve Alert
```http
PUT /api-1.0-SNAPSHOT/api/alerts/{id}/resolve
Authorization: Bearer <token>
```

---

## 🗺️ Sensor Locations

| Sensor ID | Location | City | Coordinates |
|-----------|----------|------|-------------|
| SENSOR_TUNIS_001 | Downtown | Tunis | 36.8065, 10.1815 |
| SENSOR_TUNIS_002 | Carthage | Tunis | 36.8500, 10.1658 |
| SENSOR_TUNIS_003 | La Marsa | Tunis | 36.7525, 10.2084 |
| SENSOR_SFAX_001 | Industrial Zone | Sfax | 34.7406, 10.7603 |
| SENSOR_SOUSSE_001 | City Center | Sousse | 35.8256, 10.6369 |

---

## 📊 AQI Calculation

AirAware uses EPA standards for Air Quality Index calculation:

| AQI Range | Category | PM2.5 (µg/m³) | Color |
|-----------|----------|---------------|-------|
| 0-50 | Good | 0.0-12.0 | 🟢 Green |
| 51-100 | Moderate | 12.1-35.4 | 🟡 Yellow |
| 101-150 | Unhealthy for Sensitive Groups | 35.5-55.4 | 🟠 Orange |
| 151-200 | Unhealthy | 55.5-150.4 | 🔴 Red |
| 201-300 | Very Unhealthy | 150.5-250.4 | 🟣 Purple |
| 301-500 | Hazardous | 250.5-500.4 | 🟤 Maroon |

---

## 🔧 Configuration

### Environment Variables

```bash
# MongoDB
MONGODB_URI=mongodb://localhost:27017/AirAwareDB

# MQTT
MQTT_BROKER_URL=tcp://localhost:1883
MQTT_TOPIC=airaware/sensors/#

# JWT
JWT_SECRET_KEY=your-secret-key
JWT_EXPIRATION=3600

# Email (for activation codes)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=your-email@gmail.com
SMTP_PASSWORD=your-app-password
```

### Raspberry Pi Configuration (config.yaml)

```yaml
mqtt:
  broker: 192.168.1.100  # Your PC's IP address
  port: 1883
  topic: airaware/sensors

sensor:
  id: SENSOR_TUNIS_001
  interval: 30  # seconds between readings

location:
  latitude: 36.8065
  longitude: 10.1815
  city: Tunis
  country: Tunisia
```

---

## 🧪 Testing

### Run API Tests
```bash
cd api
mvn test
```

### Run Frontend Tests
```bash
cd airaware-pwa
npm run test
```

### Manual API Testing

```bash
# Register sensor
curl -X POST http://localhost:8080/api-1.0-SNAPSHOT/api/sensors \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"sensorId":"TEST_001","name":"Test Sensor","status":"ACTIVE"}'

# Send test reading via MQTT
mosquitto_pub -h localhost -t "airaware/sensors" \
  -m '{"sensorId":"TEST_001","temperature":25.5,"humidity":60,"pm25":15.2}'
```

---

## 🐛 Troubleshooting

### Common Issues

| Problem | Solution |
|---------|----------|
| CORS errors | Check CorsFilter includes localhost:5173 |
| 401 Unauthorized | Verify JWT token, check JwtAuthenticationFilter |
| No sensor data | Verify MQTT connection, check firewall port 1883 |
| MongoDB connection refused | Ensure MongoDB service is running |
| PWA not updating | Clear browser cache, unregister service worker |

### Debug Commands

```bash
# Check MongoDB data
mongosh AirAwareDB --eval "db.readings.find().sort({timestamp:-1}).limit(5).pretty()"

# Monitor MQTT messages
mosquitto_sub -h localhost -t "airaware/#" -v

# Check WildFly logs
tail -f $JBOSS_HOME/standalone/log/server.log

# Verify API health
curl http://localhost:8080/api-1.0-SNAPSHOT/api/sensors
```

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Code Style

- **Java:** Follow Google Java Style Guide
- **JavaScript/React:** Use ESLint with Airbnb config
- **Python:** Follow PEP 8

---

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👥 Authors

- **Your Name** - *Initial work* - [GitHub Profile](https://github.com/yourusername)

---

## 🙏 Acknowledgments

- [EPA Air Quality Index](https://www.airnow.gov/aqi/aqi-basics/) for AQI calculation standards
- [OpenStreetMap](https://www.openstreetmap.org/) for map tiles
- [Tailwind CSS](https://tailwindcss.com/) for styling
- [Lucide Icons](https://lucide.dev/) for icons

---

## 📞 Support

For support, email support@airaware.tn or open an issue on GitHub.

---

<p align="center">
  Made with ❤️ for Tunisia 🇹🇳
</p>
