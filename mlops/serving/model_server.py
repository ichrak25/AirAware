"""
AirAware MLOps - FastAPI Model Server (FIXED VERSION)
REST API for model inference with WORKING anomaly detection and risk levels
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import pandas as pd
import numpy as np
import logging
from typing import List, Optional
from datetime import datetime
import sys
import os
from pathlib import Path

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Get absolute paths
MLOPS_DIR = Path(__file__).parent.parent.resolve()
sys.path.insert(0, str(MLOPS_DIR))

# Try to import the trained models
MODELS_AVAILABLE = False
anomaly_detector = None
aqi_predictor = None
ensemble_model = None
feature_engineer = None
preprocessor = None

try:
    from models import AnomalyDetector, AQIPredictor, EnsembleModel
    from data import FeatureEngineer, DataPreprocessor
    MODELS_AVAILABLE = True
except ImportError as e:
    logger.warning(f"Could not import ML models: {e}")
    logger.warning("Using fallback rule-based predictions")

# Import the alerting service
ALERTING_AVAILABLE = False
alerting_service = None
try:
    from monitoring.alerting import MLAlertingService, create_alerting_service
    ALERTING_AVAILABLE = True
except ImportError:
    logger.warning("Alerting module not available - alerts will be disabled")

# Create FastAPI app
app = FastAPI(
    title="AirAware ML API",
    version="2.1.0",
    description="Machine Learning API for AirAware - FIXED with proper anomaly detection"
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ============================================================================
# Pydantic Models
# ============================================================================

class SensorReading(BaseModel):
    """Sensor reading input"""
    sensorId: str
    temperature: float
    humidity: float
    co2: float
    voc: float
    pm25: float
    pm10: float
    timestamp: Optional[str] = None


class PredictionResponse(BaseModel):
    """Prediction output"""
    sensorId: str
    predicted_aqi: float
    anomaly_score: float
    is_anomaly: bool
    risk_level: str
    timestamp: str
    alerts_generated: Optional[int] = 0
    details: Optional[dict] = None


# ============================================================================
# FIXED: Rule-Based Prediction Logic (Works even without trained models)
# ============================================================================

# Normal ranges for each metric
NORMAL_RANGES = {
    'temperature': {'min': 15, 'max': 35, 'critical_min': 0, 'critical_max': 45},
    'humidity': {'min': 30, 'max': 70, 'critical_min': 10, 'critical_max': 90},
    'co2': {'min': 350, 'max': 1000, 'critical_min': 0, 'critical_max': 5000},
    'voc': {'min': 0, 'max': 0.5, 'critical_min': 0, 'critical_max': 3.0},
    'pm25': {'min': 0, 'max': 35, 'critical_min': 0, 'critical_max': 500},
    'pm10': {'min': 0, 'max': 50, 'critical_min': 0, 'critical_max': 600},
}

# AQI Breakpoints (EPA Standard)
AQI_BREAKPOINTS = [
    {'pm25_low': 0.0, 'pm25_high': 12.0, 'aqi_low': 0, 'aqi_high': 50, 'category': 'Good'},
    {'pm25_low': 12.1, 'pm25_high': 35.4, 'aqi_low': 51, 'aqi_high': 100, 'category': 'Moderate'},
    {'pm25_low': 35.5, 'pm25_high': 55.4, 'aqi_low': 101, 'aqi_high': 150, 'category': 'Unhealthy for Sensitive'},
    {'pm25_low': 55.5, 'pm25_high': 150.4, 'aqi_low': 151, 'aqi_high': 200, 'category': 'Unhealthy'},
    {'pm25_low': 150.5, 'pm25_high': 250.4, 'aqi_low': 201, 'aqi_high': 300, 'category': 'Very Unhealthy'},
    {'pm25_low': 250.5, 'pm25_high': 500.4, 'aqi_low': 301, 'aqi_high': 500, 'category': 'Hazardous'},
]


def calculate_aqi_from_pm25(pm25: float) -> tuple:
    """Calculate AQI from PM2.5 using EPA formula"""
    if pm25 < 0:
        return 0, 'Invalid'

    for bp in AQI_BREAKPOINTS:
        if bp['pm25_low'] <= pm25 <= bp['pm25_high']:
            aqi = ((bp['aqi_high'] - bp['aqi_low']) / (bp['pm25_high'] - bp['pm25_low'])) * \
                  (pm25 - bp['pm25_low']) + bp['aqi_low']
            return round(aqi), bp['category']

    # Above maximum
    return 500, 'Hazardous'


def calculate_anomaly_score(reading: dict) -> tuple:
    """
    Calculate anomaly score based on how far values are from normal ranges.
    Returns (anomaly_score, is_anomaly, anomaly_details)

    Score: 0.0 = completely normal, 1.0 = extremely anomalous
    """
    anomaly_scores = {}
    total_score = 0.0
    num_metrics = 0

    for metric, ranges in NORMAL_RANGES.items():
        if metric not in reading:
            continue

        value = reading[metric]
        if value is None:
            continue

        num_metrics += 1

        # Calculate how far outside normal range
        if value < ranges['min']:
            # Below normal
            if value < ranges['critical_min']:
                score = 1.0  # Critical
            else:
                score = (ranges['min'] - value) / (ranges['min'] - ranges['critical_min'])
                score = min(score, 1.0)
        elif value > ranges['max']:
            # Above normal
            if value > ranges['critical_max']:
                score = 1.0  # Critical
            else:
                score = (value - ranges['max']) / (ranges['critical_max'] - ranges['max'])
                score = min(score, 1.0)
        else:
            # Within normal range
            score = 0.0

        anomaly_scores[metric] = round(score, 3)
        total_score += score

    # Average anomaly score
    if num_metrics > 0:
        avg_score = total_score / num_metrics
    else:
        avg_score = 0.0

    # Also consider combined effects (high CO2 + high VOC is worse)
    combined_boost = 0.0
    if reading.get('co2', 0) > 1000 and reading.get('voc', 0) > 0.5:
        combined_boost = 0.15
    if reading.get('pm25', 0) > 50 and reading.get('pm10', 0) > 75:
        combined_boost += 0.1
    if reading.get('temperature', 25) > 35 and reading.get('humidity', 50) > 70:
        combined_boost += 0.1

    final_score = min(avg_score + combined_boost, 1.0)

    # Determine if it's an anomaly (threshold: 0.3)
    is_anomaly = final_score >= 0.3

    return round(final_score, 3), is_anomaly, anomaly_scores


def calculate_risk_level(predicted_aqi: float, anomaly_score: float) -> str:
    """
    Calculate risk level based on AQI and anomaly score.
    """
    # Base risk on AQI
    if predicted_aqi <= 50:
        aqi_risk = 0  # Good
    elif predicted_aqi <= 100:
        aqi_risk = 1  # Moderate
    elif predicted_aqi <= 150:
        aqi_risk = 2  # Unhealthy for sensitive
    elif predicted_aqi <= 200:
        aqi_risk = 3  # Unhealthy
    elif predicted_aqi <= 300:
        aqi_risk = 4  # Very Unhealthy
    else:
        aqi_risk = 5  # Hazardous

    # Anomaly can increase risk by 1 level
    if anomaly_score >= 0.7:
        aqi_risk += 2
    elif anomaly_score >= 0.4:
        aqi_risk += 1

    # Map to risk levels
    if aqi_risk <= 1:
        return 'LOW'
    elif aqi_risk <= 2:
        return 'MEDIUM'
    elif aqi_risk <= 3:
        return 'HIGH'
    else:
        return 'CRITICAL'


def make_prediction(reading: dict) -> dict:
    """
    Make a complete prediction for a sensor reading.
    Uses trained models if available, otherwise uses rule-based approach.
    """
    # Calculate AQI from PM2.5
    pm25 = reading.get('pm25', 0)
    predicted_aqi, aqi_category = calculate_aqi_from_pm25(pm25)

    # Calculate anomaly score
    anomaly_score, is_anomaly, anomaly_details = calculate_anomaly_score(reading)

    # Calculate risk level
    risk_level = calculate_risk_level(predicted_aqi, anomaly_score)

    return {
        'predicted_aqi': predicted_aqi,
        'aqi_category': aqi_category,
        'anomaly_score': anomaly_score,
        'is_anomaly': is_anomaly,
        'risk_level': risk_level,
        'anomaly_details': anomaly_details,
    }


# ============================================================================
# Startup / Shutdown Events
# ============================================================================

@app.on_event("startup")
async def load_models():
    """Load models on startup"""
    global anomaly_detector, aqi_predictor, ensemble_model
    global feature_engineer, preprocessor, alerting_service

    logger.info("=" * 60)
    logger.info("🚀 Starting AirAware ML API Server v2.1 (FIXED)")
    logger.info("=" * 60)
    logger.info(f"Working directory: {os.getcwd()}")
    logger.info(f"MLOps directory: {MLOPS_DIR}")

    if MODELS_AVAILABLE:
        # Change to mlops directory for relative paths
        original_dir = os.getcwd()
        os.chdir(MLOPS_DIR)

        try:
            model_files = [
                "models/anomaly_detector.pkl",
                "models/aqi_predictor.pkl",
                "models/preprocessor.pkl",
            ]

            missing_files = [f for f in model_files if not Path(f).exists()]

            if not missing_files:
                anomaly_detector = AnomalyDetector.load("models/anomaly_detector.pkl")
                logger.info("✅ Anomaly detector loaded")

                aqi_predictor = AQIPredictor.load("models/aqi_predictor.pkl")
                logger.info("✅ AQI predictor loaded")

                preprocessor = DataPreprocessor.load("models/preprocessor.pkl")
                logger.info("✅ Preprocessor loaded")

                feature_engineer = FeatureEngineer()
                logger.info("✅ Feature engineer initialized")

                ensemble_model = EnsembleModel()
                ensemble_model.set_models(anomaly_detector, aqi_predictor, None)
                logger.info("✅ Ensemble model created")
            else:
                logger.warning(f"⚠️ Missing model files: {missing_files}")
                logger.warning("⚠️ Using rule-based predictions instead")

        except Exception as e:
            logger.warning(f"⚠️ Failed to load ML models: {e}")
            logger.warning("⚠️ Using rule-based predictions instead")

        finally:
            os.chdir(original_dir)
    else:
        logger.info("ℹ️ ML models not available, using rule-based predictions")

    # Initialize alerting service
    if ALERTING_AVAILABLE:
        try:
            alerting_service = create_alerting_service({
                "webhook_url": os.getenv(
                    "BACKEND_ALERT_URL",
                    "http://localhost:8080/api-1.0-SNAPSHOT/api/alerts"
                ),
                "slack_webhook": os.getenv("SLACK_WEBHOOK_URL"),
                "anomaly_threshold": 0.3,  # Lower threshold for better detection
                "aqi_warning": 100,
                "aqi_unhealthy": 150,
                "aqi_dangerous": 200
            })
            logger.info("✅ Alerting service initialized")
        except Exception as e:
            logger.warning(f"⚠️ Alerting service failed: {e}")

    logger.info("=" * 60)
    logger.info("✅ Server ready! Using FIXED anomaly detection")
    logger.info("=" * 60)


@app.on_event("shutdown")
async def shutdown_event():
    """Cleanup on shutdown"""
    logger.info("🔴 Shutting down AirAware ML API Server")


# ============================================================================
# API Endpoints
# ============================================================================

@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "service": "AirAware ML API",
        "version": "2.1.0 (FIXED)",
        "status": "running",
        "prediction_mode": "trained_models" if ensemble_model else "rule_based",
        "features": {
            "prediction": True,
            "anomaly_detection": True,
            "alerting": alerting_service is not None
        },
        "docs": "/docs",
        "health": "/health"
    }


@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "models_loaded": ensemble_model is not None,
        "prediction_mode": "trained_models" if ensemble_model else "rule_based",
        "alerting_enabled": alerting_service is not None,
        "mlops_dir": str(MLOPS_DIR),
        "timestamp": datetime.now().isoformat()
    }


@app.post("/predict", response_model=PredictionResponse)
async def predict(reading: SensorReading):
    """
    Predict air quality for a single sensor reading.

    FIXED: Now properly calculates anomaly scores and risk levels!
    """
    try:
        logger.info(f"Processing prediction for sensor: {reading.sensorId}")
        logger.info(f"  Input: temp={reading.temperature}, humidity={reading.humidity}, "
                    f"co2={reading.co2}, voc={reading.voc}, pm25={reading.pm25}, pm10={reading.pm10}")

        # Convert reading to dict
        reading_dict = {
            'sensorId': reading.sensorId,
            'temperature': reading.temperature,
            'humidity': reading.humidity,
            'co2': reading.co2,
            'voc': reading.voc,
            'pm25': reading.pm25,
            'pm10': reading.pm10,
        }

        # Try trained model first, fall back to rule-based
        if ensemble_model is not None:
            try:
                # Use trained models
                df = pd.DataFrame([reading_dict])
                df['timestamp'] = pd.to_datetime(reading.timestamp or datetime.now().isoformat())

                df_features = feature_engineer.engineer_features(df)

                target_cols = ['aqi_pm25', 'aqi_category', 'aqi_pm10', 'target']
                for col in target_cols:
                    if col in df_features.columns:
                        df_features = df_features.drop(columns=[col])

                expected_features = preprocessor.feature_names
                for col in expected_features:
                    if col not in df_features.columns:
                        df_features[col] = 0.0 if col not in ["aqi_category", "location"] else "unknown"

                df_features = df_features[expected_features]
                X, _ = preprocessor.transform(df_features)

                results = ensemble_model.predict(X)

                # Get values from model
                predicted_aqi = float(results.get('predicted_aqi', 0))
                model_anomaly = float(results.get('anomaly_score', 0))

                # FIXED: If model returns 0 anomaly, use our rule-based calculation
                if model_anomaly == 0:
                    _, _, anomaly_details = calculate_anomaly_score(reading_dict)
                    anomaly_score, is_anomaly, _ = calculate_anomaly_score(reading_dict)
                else:
                    anomaly_score = model_anomaly
                    is_anomaly = anomaly_score >= 0.3
                    anomaly_details = {}

                # FIXED: Calculate proper risk level
                risk_level = calculate_risk_level(predicted_aqi, anomaly_score)

                logger.info(f"  Model prediction: AQI={predicted_aqi}, anomaly={anomaly_score}")

            except Exception as e:
                logger.warning(f"  Model prediction failed: {e}, using rule-based")
                results = make_prediction(reading_dict)
                predicted_aqi = results['predicted_aqi']
                anomaly_score = results['anomaly_score']
                is_anomaly = results['is_anomaly']
                risk_level = results['risk_level']
                anomaly_details = results['anomaly_details']
        else:
            # Use rule-based prediction
            results = make_prediction(reading_dict)
            predicted_aqi = results['predicted_aqi']
            anomaly_score = results['anomaly_score']
            is_anomaly = results['is_anomaly']
            risk_level = results['risk_level']
            anomaly_details = results['anomaly_details']

        logger.info(f"  Result: AQI={predicted_aqi}, Anomaly={anomaly_score:.1%}, "
                    f"IsAnomaly={is_anomaly}, Risk={risk_level}")

        # Generate alerts
        alerts_generated = 0
        if alerting_service is not None:
            try:
                alerts = alerting_service.process_prediction(
                    prediction={
                        "predicted_aqi": predicted_aqi,
                        "anomaly_score": anomaly_score,
                        "is_anomaly": is_anomaly,
                        "risk_level": risk_level
                    },
                    sensor_id=reading.sensorId
                )
                alerts_generated = len(alerts) if alerts else 0
                if alerts_generated > 0:
                    logger.info(f"  🚨 Generated {alerts_generated} alert(s)")
            except Exception as e:
                logger.error(f"  Alert generation failed: {e}")

        return PredictionResponse(
            sensorId=reading.sensorId,
            predicted_aqi=predicted_aqi,
            anomaly_score=anomaly_score,
            is_anomaly=is_anomaly,
            risk_level=risk_level,
            timestamp=datetime.now().isoformat(),
            alerts_generated=alerts_generated,
            details=anomaly_details if anomaly_details else None
        )

    except Exception as e:
        logger.error(f"Prediction failed for {reading.sensorId}: {e}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=f"Prediction failed: {str(e)}")


@app.post("/predict/batch")
async def predict_batch(readings: List[SensorReading]):
    """Batch predictions for multiple sensor readings"""
    results = []
    errors = []
    total_alerts = 0

    logger.info(f"Processing batch prediction for {len(readings)} readings")

    for idx, reading in enumerate(readings):
        try:
            result = await predict(reading)
            results.append(result)
            total_alerts += result.alerts_generated or 0
        except Exception as e:
            logger.error(f"Failed to process reading {idx} ({reading.sensorId}): {e}")
            errors.append({
                "index": idx,
                "sensorId": reading.sensorId,
                "error": str(e)
            })

    return {
        "predictions": results,
        "total": len(readings),
        "successful": len(results),
        "failed": len(errors),
        "alerts_generated": total_alerts,
        "errors": errors if errors else None
    }


@app.get("/models/info")
async def get_model_info():
    """Get information about loaded models"""
    return {
        "prediction_mode": "trained_models" if ensemble_model else "rule_based",
        "models": {
            "anomaly_detector": {
                "type": "IsolationForest" if anomaly_detector else "Rule-based",
                "loaded": anomaly_detector is not None
            },
            "aqi_predictor": {
                "type": "XGBoost" if aqi_predictor else "EPA Formula",
                "loaded": aqi_predictor is not None
            }
        },
        "thresholds": {
            "anomaly": {
                "low": "< 0.3",
                "medium": "0.3 - 0.7",
                "high": ">= 0.7"
            },
            "aqi_risk": {
                "LOW": "AQI <= 100",
                "MEDIUM": "AQI 101-150",
                "HIGH": "AQI 151-200",
                "CRITICAL": "AQI > 200 or high anomaly"
            }
        },
        "normal_ranges": NORMAL_RANGES
    }


@app.get("/test")
async def test_prediction():
    """Test endpoint with sample data to verify anomaly detection works"""

    test_cases = [
        {
            "name": "Normal Reading",
            "data": {"sensorId": "TEST", "temperature": 25, "humidity": 50, "co2": 500, "voc": 0.3, "pm25": 15, "pm10": 25}
        },
        {
            "name": "High PM2.5",
            "data": {"sensorId": "TEST", "temperature": 25, "humidity": 50, "co2": 500, "voc": 0.3, "pm25": 150, "pm10": 100}
        },
        {
            "name": "Very High CO2",
            "data": {"sensorId": "TEST", "temperature": 25, "humidity": 50, "co2": 3000, "voc": 0.3, "pm25": 20, "pm10": 30}
        },
        {
            "name": "Multiple Anomalies",
            "data": {"sensorId": "TEST", "temperature": 45, "humidity": 90, "co2": 2500, "voc": 2.0, "pm25": 200, "pm10": 250}
        },
        {
            "name": "Extreme Hazardous",
            "data": {"sensorId": "TEST", "temperature": 50, "humidity": 95, "co2": 5000, "voc": 5.0, "pm25": 500, "pm10": 600}
        },
    ]

    results = []
    for case in test_cases:
        prediction = make_prediction(case["data"])
        results.append({
            "name": case["name"],
            "input": case["data"],
            "prediction": prediction
        })

    return {
        "message": "Test predictions to verify anomaly detection",
        "results": results
    }


# ============================================================================
# Main
# ============================================================================

if __name__ == "__main__":
    import uvicorn

    print("\n" + "=" * 70)
    print("🚀 Starting AirAware ML API Server v2.1 (FIXED)")
    print("=" * 70)
    print(f"\n📁 MLOps Directory: {MLOPS_DIR}")
    print("\n🔗 Server will be available at:")
    print("   - API: http://localhost:5000")
    print("   - Docs: http://localhost:5000/docs")
    print("   - Health: http://localhost:5000/health")
    print("   - Test: http://localhost:5000/test")
    print("\n✨ FIXED Features:")
    print("   - Proper anomaly score calculation (0-100%)")
    print("   - Risk levels based on AQI + anomaly")
    print("   - Detailed anomaly breakdown per metric")
    print("   - Works even without trained models")
    print("\n" + "=" * 70 + "\n")

    uvicorn.run(
        app,
        host="0.0.0.0",
        port=5000,
        log_level="info"
    )