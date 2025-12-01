# ============================================================================
# models/ensemble_model.py
# ============================================================================
"""
AirAware MLOps - Ensemble Model
Combines predictions from multiple models safely
"""

import logging
import numpy as np
import pandas as pd
import yaml

logger = logging.getLogger(__name__)


class EnsembleModel:
    """Ensemble of multiple air quality models"""

    def __init__(self, config_path: str = "config/config.yaml"):
        with open(config_path, 'r') as f:
            config = yaml.safe_load(f)

        self.weights = config['models']['ensemble']['weights']

        self.anomaly_detector = None
        self.aqi_predictor = None
        self.forecaster = None

        logger.info("Ensemble model initialized")

    def set_models(self, anomaly_detector, aqi_predictor, forecaster):
        """Set component models"""
        self.anomaly_detector = anomaly_detector
        self.aqi_predictor = aqi_predictor
        self.forecaster = forecaster

    def predict(self, X: pd.DataFrame) -> dict:
        results = {}

        # ---------- ANOMALY DETECTION ----------
        if self.anomaly_detector:
            anomaly_results = self.anomaly_detector.detect_anomalies(X)
            results['anomaly_score'] = float(
                np.clip(anomaly_results['anomaly_score'].iloc[0], 0, 1)
            )
            results['is_anomaly'] = bool(anomaly_results['is_anomaly'].iloc[0])

            # Safety guard
            if results['is_anomaly'] and results['anomaly_score'] < 0.3:
                results['is_anomaly'] = False

        # ---------- AQI PREDICTION ----------
        if self.aqi_predictor:
            aqi_pred = self.aqi_predictor.predict(X)
            aqi_val = float(np.nan_to_num(aqi_pred[0], nan=0.0, posinf=500.0, neginf=0.0))
            results['predicted_aqi'] = np.clip(aqi_val, 0, 500)

        # ---------- RISK SCORE ----------
        anomaly_score = results.get('anomaly_score', 0)
        predicted_aqi = results.get('predicted_aqi', 0)

        aqi_norm = min(predicted_aqi / 500, 1.0)
        risk_score = (
                anomaly_score * self.weights.get('anomaly', 0.5) +
                aqi_norm * self.weights.get('aqi', 0.5)
        )

        results['risk_score'] = float(np.clip(risk_score, 0, 1))

        # ---------- RISK LEVEL ----------
        if results.get('is_anomaly', False):
            results['risk_level'] = "HIGH"
        else:
            results['risk_level'] = self._classify_risk(risk_score)

        return results


    def _classify_risk(self, score: float) -> str:
        """Classify risk level"""
        if score < 0.3:
            return "LOW"
        elif score < 0.6:
            return "MODERATE"
        elif score < 0.8:
            return "HIGH"
        else:
            return "CRITICAL"
