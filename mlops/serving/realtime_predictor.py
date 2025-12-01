# ============================================================================
# serving/realtime_predictor.py
# ============================================================================
"""
AirAware MLOps - Realtime Predictor
Low-latency predictions for streaming data
"""

import logging
import pandas as pd
from models import EnsembleModel
from data import FeatureEngineer, DataPreprocessor

logger = logging.getLogger(__name__)


class RealtimePredictor:
    """Real-time prediction with caching"""

    def __init__(self):
        self.ensemble = EnsembleModel()
        self.feature_engineer = FeatureEngineer()
        self.preprocessor = DataPreprocessor.load()
        self.cache = {}

        logger.info("Realtime predictor initialized")

    def predict_single(self, reading: dict) -> dict:
        """Predict single reading with caching"""
        # Check cache
        cache_key = self._generate_cache_key(reading)
        if cache_key in self.cache:
            return self.cache[cache_key]

        # Convert to DataFrame
        df = pd.DataFrame([reading])

        # Engineer & preprocess
        df = self.feature_engineer.engineer_features(df)
        X, _ = self.preprocessor.transform(df)

        # Predict
        result = self.ensemble.predict(X)

        # Cache result
        self.cache[cache_key] = result

        return result

    def _generate_cache_key(self, reading: dict) -> str:
        """Generate cache key"""
        return f"{reading['sensorId']}_{reading['timestamp']}"