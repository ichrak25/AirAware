# ============================================================================
# models/anomaly_detector.py
# ============================================================================
"""
AirAware MLOps - Anomaly Detector
Isolation Forest for detecting unusual air quality patterns
"""

import logging
import numpy as np
import pandas as pd
from sklearn.ensemble import IsolationForest
import joblib
import os
import yaml

logger = logging.getLogger(__name__)


class AnomalyDetector:
    """Detect anomalies in air quality data using Isolation Forest"""

    def __init__(self, config_path: str = "config/config.yaml"):
        with open(config_path, 'r') as f:
            config = yaml.safe_load(f)

        params = config['models']['anomaly_detector']['params']

        self.model = IsolationForest(
            n_estimators=params['n_estimators'],
            contamination=params['contamination'],
            max_samples=params['max_samples'],
            random_state=params['random_state'],
            n_jobs=-1
        )

        self.threshold = params['contamination']
        self.is_fitted = False

        logger.info("Anomaly detector initialized")

    def fit(self, X: pd.DataFrame):
        """Train anomaly detector"""
        logger.info(f"Training anomaly detector on {len(X)} samples")
        self.model.fit(X)
        self.is_fitted = True
        logger.info("Anomaly detector trained successfully")

    def predict(self, X: pd.DataFrame) -> np.ndarray:
        """
        Predict anomalies
        Returns: -1 for anomalies, 1 for normal
        """
        if not self.is_fitted:
            raise ValueError("Model not fitted. Call fit() first.")

        return self.model.predict(X)

    def predict_scores(self, X: pd.DataFrame) -> np.ndarray:
        """
        Get normalized anomaly scores
        Range: 0–1 (1 = most anomalous)
        """
        if not self.is_fitted:
            raise ValueError("Model not fitted. Call fit() first.")

        scores = self.model.score_samples(X)

        min_score = np.min(scores)
        max_score = np.max(scores)
        denom = max_score - min_score

        # ✅ SAFE NORMALIZATION (NO NaN / INF)
        if denom == 0 or np.isnan(denom):
            logger.warning("Anomaly score normalization skipped (constant scores)")
            scores_normalized = np.zeros_like(scores)
        else:
            scores_normalized = 1 - (scores - min_score) / denom

        return scores_normalized

    def detect_anomalies(self, X: pd.DataFrame) -> pd.DataFrame:
        """
        Detect anomalies with detailed results

        Returns:
            DataFrame with columns:
            - is_anomaly
            - anomaly_score
            - anomaly_confidence
        """
        scores = self.predict_scores(X)
        predictions = self.predict(X)

        results = pd.DataFrame({
            'is_anomaly': predictions == -1,
            'anomaly_score': scores,
            'anomaly_confidence': np.abs(scores - 0.5) * 2  # 0–1 scale
        }, index=X.index)

        return results

    def save(self, path: str = "models/anomaly_detector.pkl"):
        """Save model"""
        os.makedirs(os.path.dirname(path), exist_ok=True)
        joblib.dump(self.model, path)
        logger.info(f"Anomaly detector saved to {path}")

    @classmethod
    def load(cls, path: str = "models/anomaly_detector.pkl"):
        """Load trained model"""
        detector = cls()
        detector.model = joblib.load(path)
        detector.is_fitted = True
        logger.info(f"Anomaly detector loaded from {path}")
        return detector
