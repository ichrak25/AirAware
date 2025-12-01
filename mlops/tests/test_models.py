# ============================================================================
# tests/test_models.py
# ============================================================================
"""
AirAware MLOps - Model Tests
"""

import pytest
import pandas as pd
import numpy as np
from models import AnomalyDetector, AQIPredictor


def create_sample_data(n_samples=100):
    """Create sample data for testing"""
    return pd.DataFrame({
        'temperature': np.random.normal(25, 5, n_samples),
        'humidity': np.random.normal(60, 10, n_samples),
        'co2': np.random.normal(800, 200, n_samples),
        'voc': np.random.normal(0.5, 0.2, n_samples),
        'pm25': np.random.normal(15, 5, n_samples),
        'pm10': np.random.normal(30, 10, n_samples)
    })


def test_anomaly_detector():
    """Test anomaly detector"""
    X = create_sample_data()

    detector = AnomalyDetector()
    detector.fit(X)

    predictions = detector.predict(X)
    assert len(predictions) == len(X)
    assert set(predictions).issubset({-1, 1})


def test_aqi_predictor():
    """Test AQI predictor"""
    X = create_sample_data()
    y = np.random.randint(0, 100, len(X))

    predictor = AQIPredictor()
    predictor.fit(X, y)

    predictions = predictor.predict(X)
    assert len(predictions) == len(X)
    assert all(predictions >= 0)


if __name__ == "__main__":
    pytest.main([__file__, "-v"])