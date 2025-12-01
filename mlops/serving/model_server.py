"""
AirAware MLOps - FastAPI Model Server
REST API for model inference (with absolute path handling)
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import pandas as pd
import logging
from typing import List
from datetime import datetime
import sys
import os
from pathlib import Path

# Get absolute paths
MLOPS_DIR = Path(__file__).parent.parent.resolve()
sys.path.insert(0, str(MLOPS_DIR))

from models import AnomalyDetector, AQIPredictor, EnsembleModel
from data import FeatureEngineer, DataPreprocessor

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Create FastAPI app
app = FastAPI(
    title="AirAware ML API",
    version="1.0.0",
    description="Machine Learning API for AirAware Air Quality Monitoring"
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


# ============================================================================
# Startup / Shutdown Events
# ============================================================================

@app.on_event("startup")
async def load_models():
    """Load models on startup"""
    global anomaly_detector, aqi_predictor, ensemble_model
    global feature_engineer, preprocessor

    logger.info("=" * 60)
    logger.info("🚀 Starting AirAware ML API Server")
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
        logger.error("   3. Verify files exist:")
        logger.error(f"      {MLOPS_DIR / 'models/anomaly_detector.pkl'}")
        logger.error(f"      {MLOPS_DIR / 'models/aqi_predictor.pkl'}")
        logger.error(f"      {MLOPS_DIR / 'models/preprocessor.pkl'}")
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
        # Restore original directory
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
        "version": "1.0.0",
        "status": "running",
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
        "mlops_dir": str(MLOPS_DIR),
        "timestamp": datetime.now().isoformat()
    }


@app.post("/predict", response_model=PredictionResponse)
async def predict(reading: SensorReading):
    """Predict air quality for a single sensor reading"""
    try:
        # Check if models are loaded
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

        # Feature engineering (includes all rolling features, temporal features, etc.)
        df_features = feature_engineer.engineer_features(df)
        logger.debug(f"Engineered features shape: {df_features.shape}")

        # ✅ CRITICAL: Remove target column if it exists (we're predicting it!)
        if 'aqi_pm25' in df_features.columns:
            df_features = df_features.drop(columns=['aqi_pm25'])
            logger.debug("Removed target column 'aqi_pm25' from features")

        # ✅ Remove any other target-related columns
        target_related = ['aqi_category', 'aqi_pm10', 'target']
        for col in target_related:
            if col in df_features.columns:
                df_features = df_features.drop(columns=[col])
                logger.debug(f"Removed target-related column '{col}'")

        # =========================================================
        # ✅ CRITICAL FIX: align inference features with training
        # =========================================================

        # Features expected by the preprocessor (during fit)
        expected_features = preprocessor.feature_names

        for col in expected_features:
            if col not in df_features.columns:
                if col == "aqi_category":
                    df_features[col] = "Unknown"   # categorical safe default
                elif col == "location":
                    df_features[col] = "unknown"   # categorical safe default
                else:
                    df_features[col] = 0.0          # numeric fallback

        # Reorder columns EXACTLY as training
        df_features = df_features[expected_features]

        # Preprocess (this will align features with training)
        X, _ = preprocessor.transform(df_features)

        logger.debug(f"Preprocessed features shape: {X.shape}")
        logger.debug(f"Features: {X.columns.tolist()[:10]}...")  # Show first 10

        # Predict using ensemble
        results = ensemble_model.predict(X)
        logger.info(f"Prediction complete for {reading.sensorId}: AQI={results.get('predicted_aqi', 0):.1f}")

        return PredictionResponse(
            sensorId=reading.sensorId,
            predicted_aqi=float(results.get('predicted_aqi', 0)),
            anomaly_score=float(results.get('anomaly_score', 0)),
            is_anomaly=bool(results.get('is_anomaly', False)),
            risk_level=str(results.get('risk_level', 'UNKNOWN')),
            timestamp=datetime.now().isoformat()
        )

    except HTTPException:
        raise

    except Exception as e:
        logger.error(f"Prediction failed for {reading.sensorId}: {e}")
        import traceback
        traceback.print_exc()
        raise HTTPException(
            status_code=500,
            detail=f"Prediction failed: {str(e)}"
        )


@app.post("/predict/batch")
async def predict_batch(readings: List[SensorReading]):
    """Batch predictions for multiple sensor readings"""
    results = []
    errors = []

    logger.info(f"Processing batch prediction for {len(readings)} readings")

    for idx, reading in enumerate(readings):
        try:
            result = await predict(reading)
            results.append(result)
        except Exception as e:
            logger.error(f"Failed to process reading {idx} ({reading.sensorId}): {e}")
            errors.append({
                "index": idx,
                "sensorId": reading.sensorId,
                "error": str(e)
            })

    logger.info(f"Batch processing complete: {len(results)} success, {len(errors)} errors")

    return {
        "predictions": results,
        "total": len(readings),
        "successful": len(results),
        "failed": len(errors),
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
            }
        }
    except Exception as e:
        logger.error(f"Failed to get model info: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# ============================================================================
# Main
# ============================================================================

if __name__ == "__main__":
    import uvicorn

    print("\n" + "=" * 70)
    print("🚀 Starting AirAware ML API Server")
    print("=" * 70)
    print(f"\n📁 MLOps Directory: {MLOPS_DIR}")
    print("\n📍 Server will be available at:")
    print("   - API: http://localhost:8000")
    print("   - Docs: http://localhost:8000/docs")
    print("   - Health: http://localhost:8000/health")
    print("\n" + "=" * 70 + "\n")

    uvicorn.run(
        app,
        host="0.0.0.0",
        port=8000,
        log_level="info"
    )