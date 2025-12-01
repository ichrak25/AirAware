# ============================================================================
# serving/batch_predictor.py
# ============================================================================
"""
AirAware MLOps - Batch Predictor
Process large batches of predictions
"""

import logging
import pandas as pd
from typing import List
from models import EnsembleModel
from data import FeatureEngineer, DataPreprocessor

logger = logging.getLogger(__name__)


class BatchPredictor:
    """Batch prediction processing"""

    def __init__(self):
        self.ensemble = EnsembleModel()
        self.feature_engineer = FeatureEngineer()
        self.preprocessor = DataPreprocessor.load()

        logger.info("Batch predictor initialized")

    def predict_batch(self, df: pd.DataFrame) -> pd.DataFrame:
        """Process batch predictions"""
        # Engineer features
        df_features = self.feature_engineer.engineer_features(df)

        # Preprocess
        X, _ = self.preprocessor.transform(df_features)

        # Predict
        results = self.ensemble.predict(X)

        # Add results to dataframe
        df['predicted_aqi'] = results['predicted_aqi']
        df['risk_level'] = results['risk_level']

        return df
