# ============================================================================
# models/aqi_predictor.py
# ============================================================================
"""
AirAware MLOps - AQI Predictor
XGBoost model for Air Quality Index prediction
"""

import logging
import numpy as np
import pandas as pd
import xgboost as xgb
from sklearn.metrics import mean_squared_error, mean_absolute_error, r2_score
import joblib
import os
import yaml

logger = logging.getLogger(__name__)


class AQIPredictor:
    """Predict Air Quality Index using XGBoost"""

    def __init__(self, config_path: str = "config/config.yaml"):
        with open(config_path, 'r') as f:
            config = yaml.safe_load(f)

        params = config['models']['aqi_predictor']['params']

        self.model = xgb.XGBRegressor(
            max_depth=params['max_depth'],
            learning_rate=params['learning_rate'],
            n_estimators=params['n_estimators'],
            objective=params['objective'],
            subsample=params['subsample'],
            colsample_bytree=params['colsample_bytree'],
            random_state=params['random_state'],
            n_jobs=-1
        )

        self.is_fitted = False
        self.feature_importance = None

        logger.info("AQI predictor initialized")

    def fit(
            self,
            X_train: pd.DataFrame,
            y_train: pd.Series,
            X_val: pd.DataFrame = None,
            y_val: pd.Series = None
    ):
        """Train AQI predictor"""
        logger.info(f"Training AQI predictor on {len(X_train)} samples")

        eval_set = [(X_train, y_train)]
        if X_val is not None and y_val is not None:
            eval_set.append((X_val, y_val))

        self.model.fit(
            X_train, y_train,
            eval_set=eval_set,
            verbose=False
        )

        self.is_fitted = True
        self.feature_importance = pd.DataFrame({
            'feature': X_train.columns,
            'importance': self.model.feature_importances_
        }).sort_values('importance', ascending=False)

        logger.info("AQI predictor trained successfully")

    def predict(self, X: pd.DataFrame) -> np.ndarray:
        """Predict AQI values"""
        if not self.is_fitted:
            raise ValueError("Model not fitted. Call fit() first.")

        predictions = self.model.predict(X)
        return predictions

    def evaluate(self, X: pd.DataFrame, y: pd.Series) -> dict:
        """Evaluate model performance"""
        predictions = self.predict(X)

        metrics = {
            'mse': mean_squared_error(y, predictions),
            'rmse': np.sqrt(mean_squared_error(y, predictions)),
            'mae': mean_absolute_error(y, predictions),
            'r2': r2_score(y, predictions)
        }

        logger.info(f"Evaluation metrics: {metrics}")
        return metrics

    def get_feature_importance(self, top_n: int = 10) -> pd.DataFrame:
        """Get top N important features"""
        if self.feature_importance is None:
            raise ValueError("Model not fitted yet")

        return self.feature_importance.head(top_n)

    def save(self, path: str = "models/aqi_predictor.pkl"):
        """Save model"""
        os.makedirs(os.path.dirname(path), exist_ok=True)
        joblib.dump(self.model, path)
        logger.info(f"AQI predictor saved to {path}")

    @classmethod
    def load(cls, path: str = "models/aqi_predictor.pkl"):
        """Load model"""
        predictor = cls()
        predictor.model = joblib.load(path)
        predictor.is_fitted = True
        logger.info(f"AQI predictor loaded from {path}")
        return predictor
