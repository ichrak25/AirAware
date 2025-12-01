# ============================================================================
# models/time_series_forecaster.py
# ============================================================================
"""
AirAware MLOps - Time Series Forecaster
LSTM model for forecasting future air quality
"""

import logging
import numpy as np
import pandas as pd
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers
import yaml

logger = logging.getLogger(__name__)


class TimeSeriesForecaster:
    """Forecast air quality using LSTM"""

    def __init__(self, config_path: str = "config/config.yaml"):
        with open(config_path, 'r') as f:
            config = yaml.safe_load(f)

        self.params = config['models']['time_series_forecaster']['params']
        self.model = None
        self.is_fitted = False

        logger.info("Time series forecaster initialized")

    def build_model(self, input_shape):
        """Build LSTM model"""
        model = keras.Sequential([
            layers.LSTM(
                self.params['units'][0],
                return_sequences=True,
                input_shape=input_shape
            ),
            layers.Dropout(self.params['dropout']),

            layers.LSTM(
                self.params['units'][1],
                return_sequences=True
            ),
            layers.Dropout(self.params['dropout']),

            layers.LSTM(self.params['units'][2]),
            layers.Dropout(self.params['dropout']),

            layers.Dense(1)
        ])

        optimizer = keras.optimizers.Adam(learning_rate=self.params['learning_rate'])
        model.compile(optimizer=optimizer, loss='mse', metrics=['mae'])

        return model

    def fit(self, X_train, y_train, X_val=None, y_val=None):
        """Train forecaster"""
        logger.info(f"Training forecaster on {len(X_train)} sequences")

        if self.model is None:
            self.model = self.build_model(input_shape=(X_train.shape[1], X_train.shape[2]))

        callbacks = [
            keras.callbacks.EarlyStopping(patience=10, restore_best_weights=True),
            keras.callbacks.ReduceLROnPlateau(factor=0.5, patience=5)
        ]

        validation_data = (X_val, y_val) if X_val is not None else None

        history = self.model.fit(
            X_train, y_train,
            epochs=self.params['epochs'],
            batch_size=self.params['batch_size'],
            validation_data=validation_data,
            callbacks=callbacks,
            verbose=1
        )

        self.is_fitted = True
        logger.info("Forecaster trained successfully")
        return history

    def predict(self, X):
        """Forecast future values"""
        if not self.is_fitted:
            raise ValueError("Model not fitted")

        predictions = self.model.predict(X, verbose=0)
        return predictions.flatten()

    def save(self, path: str = "models/forecaster.h5"):
        """Save model"""
        self.model.save(path)
        logger.info(f"Forecaster saved to {path}")

    @classmethod
    def load(cls, path: str = "models/forecaster.h5"):
        """Load model"""
        forecaster = cls()
        forecaster.model = keras.models.load_model(path)
        forecaster.is_fitted = True
        logger.info(f"Forecaster loaded from {path}")
        return forecaster

