"""
AirAware MLOps - FastAPI Model Server (Enhanced with Alerting)
REST API for model inference with automatic alert generation
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import pandas as pd
import logging
from typing import List, Optional
from datetime import datetime
import sys
import os
from pathlib import Path

# Get absolute paths
MLOPS_DIR = Path(__file__).parent.parent.resolve()
sys.path.insert(0, str(MLOPS_DIR))

from models import AnomalyDetector, AQIPredictor, EnsembleModel
from data import FeatureEngineer, DataPreprocessor

# Import the alerting service
try:
    from monitoring.alerting import MLAlertingService, create_alerting_service
    ALERTING_AVAILABLE = True
except ImportError:
    ALERTING_AVAILABLE = False
    logging.warning("Alerting module not available - alerts will be disabled")

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Create FastAPI app
app = FastAPI(
    title="AirAware ML API",
    version="2.0.0",
    description="Machine Learning API for AirAware Air Quality Monitoring (with Alerting)"
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Global model instances
anomaly_detector = None
aqi_predictor = None
ensemble_model = None
feature_engineer = None
preprocessor = None
alerting_service = None


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
    timestamp: str


class PredictionResponse(BaseModel):
    """Prediction output"""
    sensorId: str
    predicted_aqi: float
    anomaly_score: float
    is_anomaly: bool
    risk_level: str
    timestamp: str
    alerts_generated: Optional[int] = 0


class AlertConfig(BaseModel):
    """Configuration for alerting thresholds"""
    anomaly_threshold: float = 0.7
    aqi_warning: float = 100
    aqi_unhealthy: float = 150
    aqi_dangerous: float = 200
    enabled: bool = True


# ============================================================================
# Startup / Shutdown Events
# ============================================================================

@app.on_event("startup")
async def load_models():
    """Load models and initialize alerting on startup"""
    global anomaly_detector, aqi_predictor, ensemble_model
    global feature_engineer, preprocessor, alerting_service

    logger.info("=" * 60)
    logger.info("🚀 Starting AirAware ML API Server v2.0")
    logger.info("=" * 60)
    logger.info(f"Working directory: {os.getcwd()}")
    logger.info(f"MLOps directory: {MLOPS_DIR}")
    logger.info("Loading models...")

    # Change to mlops directory for relative paths
    original_dir = os.getcwd()
    os.chdir(MLOPS_DIR)

    try:
        # Verify files exist
        model_files = [
            "models/anomaly_detector.pkl",
            "models/aqi_predictor.pkl",
            "models/preprocessor.pkl",
            "config/config.yaml"
        ]

        missing_files = [f for f in model_files if not Path(f).exists()]
        if missing_files:
            logger.error("Missing files:")
            for f in missing_files:
                logger.error(f"  ❌ {MLOPS_DIR / f}")
            raise FileNotFoundError(f"Missing required files: {missing_files}")

        # Load models
        anomaly_detector = AnomalyDetector.load("models/anomaly_detector.pkl")
        logger.info("✅ Anomaly detector loaded")

        aqi_predictor = AQIPredictor.load("models/aqi_predictor.pkl")
        logger.info("✅ AQI predictor loaded")

        preprocessor = DataPreprocessor.load("models/preprocessor.pkl")
        logger.info("✅ Preprocessor loaded")

        feature_engineer = FeatureEngineer()
        logger.info("✅ Feature engineer initialized")

        # Create ensemble model
        ensemble_model = EnsembleModel()
        ensemble_model.set_models(anomaly_detector, aqi_predictor, None)
        logger.info("✅ Ensemble model created")

        # Initialize alerting service
        if ALERTING_AVAILABLE:
            alerting_service = create_alerting_service({
                "webhook_url": os.getenv(
                    "BACKEND_ALERT_URL",
                    "http://localhost:8080/api-1.0-SNAPSHOT/api/alerts"
                ),
                "slack_webhook": os.getenv("SLACK_WEBHOOK_URL"),
                "anomaly_threshold": 0.7,
                "aqi_warning": 100,
                "aqi_unhealthy": 150,
                "aqi_dangerous": 200
            })
            logger.info("✅ Alerting service initialized")
            logger.info("🔔 ML-based alerts are ENABLED")
        else:
            logger.warning("⚠️ Alerting service not available")

        logger.info("=" * 60)
        logger.info("✅ All models loaded successfully!")
        logger.info("=" * 60)

    except FileNotFoundError as e:
        logger.error("=" * 60)
        logger.error("❌ MODEL FILES NOT FOUND!")
        logger.error("=" * 60)
        logger.error(f"Error: {e}")
        logger.error("\n💡 Solution:")
        logger.error(f"   1. Navigate to: {MLOPS_DIR}")
        logger.error("   2. Train models: python -m training.trainer")
        logger.error("=" * 60)
        raise

    except Exception as e:
        logger.error("=" * 60)
        logger.error("❌ FAILED TO LOAD MODELS!")
        logger.error("=" * 60)
        logger.error(f"Error: {e}")
        import traceback
        traceback.print_exc()
        raise

    finally:
        os.chdir(original_dir)


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
        "version": "2.0.0",
        "status": "running",
        "features": {
            "prediction": True,
            "anomaly_detection": True,
            "alerting": ALERTING_AVAILABLE and alerting_service is not None
        },
        "docs": "/docs",
        "health": "/health",
        "mlops_dir": str(MLOPS_DIR)
    }


@app.get("/health")
async def health_check():
    """Health check endpoint"""
    models_loaded = all([
        anomaly_detector is not None,
        aqi_predictor is not None,
        ensemble_model is not None,
        feature_engineer is not None,
        preprocessor is not None
    ])

    return {
        "status": "healthy" if models_loaded else "degraded",
        "models_loaded": models_loaded,
        "alerting_enabled": alerting_service is not None,
        "mlops_dir": str(MLOPS_DIR),
        "timestamp": datetime.now().isoformat()
    }


@app.post("/predict", response_model=PredictionResponse)
async def predict(reading: SensorReading):
    """
    Predict air quality for a single sensor reading
    Also generates alerts if thresholds are exceeded
    """
    try:
        if ensemble_model is None:
            raise HTTPException(
                status_code=503,
                detail="Models not loaded yet. Please wait for startup to complete."
            )

        # Convert to DataFrame
        reading_dict = reading.model_dump() if hasattr(reading, 'model_dump') else reading.dict()
        df = pd.DataFrame([reading_dict])
        df['timestamp'] = pd.to_datetime(df['timestamp'])

        logger.info(f"Processing prediction for sensor: {reading.sensorId}")

        # Feature engineering
        df_features = feature_engineer.engineer_features(df)
        logger.debug(f"Engineered features shape: {df_features.shape}")

        # Remove target columns
        target_cols = ['aqi_pm25', 'aqi_category', 'aqi_pm10', 'target']
        for col in target_cols:
            if col in df_features.columns:
                df_features = df_features.drop(columns=[col])

        # Align features with training
        expected_features = preprocessor.feature_names
        for col in expected_features:
            if col not in df_features.columns:
                if col in ["aqi_category", "location"]:
                    df_features[col] = "unknown"
                else:
                    df_features[col] = 0.0

        df_features = df_features[expected_features]

        # Preprocess
        X, _ = preprocessor.transform(df_features)
        logger.debug(f"Preprocessed features shape: {X.shape}")

        # Predict
        results = ensemble_model.predict(X)

        predicted_aqi = float(results.get('predicted_aqi', 0))
        anomaly_score = float(results.get('anomaly_score', 0))
        is_anomaly = bool(results.get('is_anomaly', False))
        risk_level = str(results.get('risk_level', 'UNKNOWN'))

        logger.info(f"Prediction complete for {reading.sensorId}: "
                    f"AQI={predicted_aqi:.1f}, Anomaly={is_anomaly}, Risk={risk_level}")

        # 🔔 GENERATE ALERTS
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
                alerts_generated = len(alerts)
                if alerts_generated > 0:
                    logger.info(f"🚨 Generated {alerts_generated} alert(s) for {reading.sensorId}")
            except Exception as e:
                logger.error(f"Alert generation failed: {e}")

        return PredictionResponse(
            sensorId=reading.sensorId,
            predicted_aqi=predicted_aqi,
            anomaly_score=anomaly_score,
            is_anomaly=is_anomaly,
            risk_level=risk_level,
            timestamp=datetime.now().isoformat(),
            alerts_generated=alerts_generated
        )

    except HTTPException:
        raise
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

    logger.info(f"Batch complete: {len(results)} success, {len(errors)} errors, {total_alerts} alerts")

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
    if aqi_predictor is None:
        raise HTTPException(status_code=503, detail="Models not loaded")

    try:
        feature_importance = aqi_predictor.get_feature_importance(top_n=10)

        return {
            "models": {
                "anomaly_detector": {
                    "type": "IsolationForest",
                    "loaded": anomaly_detector is not None
                },
                "aqi_predictor": {
                    "type": "XGBoost",
                    "loaded": aqi_predictor is not None,
                    "top_features": feature_importance.to_dict('records') if feature_importance is not None else None
                }
            },
            "preprocessor": {
                "loaded": preprocessor is not None,
                "features_count": len(preprocessor.feature_names) if preprocessor else 0
            },
            "alerting": {
                "enabled": alerting_service is not None,
                "thresholds": alerting_service.thresholds if alerting_service else None
            }
        }
    except Exception as e:
        logger.error(f"Failed to get model info: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/alerting/configure")
async def configure_alerting(config: AlertConfig):
    """Update alerting configuration"""
    global alerting_service

    if not ALERTING_AVAILABLE:
        raise HTTPException(status_code=503, detail="Alerting module not available")

    if not config.enabled:
        alerting_service = None
        return {"status": "Alerting disabled"}

    alerting_service = create_alerting_service({
        "anomaly_threshold": config.anomaly_threshold,
        "aqi_warning": config.aqi_warning,
        "aqi_unhealthy": config.aqi_unhealthy,
        "aqi_dangerous": config.aqi_dangerous
    })

    return {
        "status": "Alerting configured",
        "thresholds": alerting_service.thresholds
    }


@app.get("/alerting/status")
async def alerting_status():
    """Get current alerting status"""
    return {
        "enabled": alerting_service is not None,
        "thresholds": alerting_service.thresholds if alerting_service else None,
        "backend_url": alerting_service.backend_url if alerting_service else None,
        "slack_configured": (alerting_service.slack_webhook is not None) if alerting_service else False
    }


# ============================================================================
# Main
# ============================================================================

if __name__ == "__main__":
    import uvicorn

    print("\n" + "=" * 70)
    print("🚀 Starting AirAware ML API Server v2.0 (with Alerting)")
    print("=" * 70)
    print(f"\n📁 MLOps Directory: {MLOPS_DIR}")
    print("\n🔗 Server will be available at:")
    print("   - API: http://localhost:8000")
    print("   - Docs: http://localhost:8000/docs")
    print("   - Health: http://localhost:8000/health")
    print("\n🔔 Alerting Features:")
    print("   - ML-based anomaly alerts")
    print("   - AQI threshold alerts")
    print("   - Risk level alerts")
    print("   - Webhook integration with Java backend")
    print("\n" + "=" * 70 + "\n")

    uvicorn.run(
        app,
        host="0.0.0.0",
        port=8000,
        log_level="info"
    )