# ============================================================================
# monitoring/performance_tracker.py
# ============================================================================
"""
AirAware MLOps - Performance Tracker
Track model performance metrics over time
"""

import logging
import pandas as pd
import numpy as np
from datetime import datetime
from prometheus_client import Gauge, Counter, Histogram
import yaml

logger = logging.getLogger(__name__)

# Prometheus metrics
model_accuracy = Gauge('model_accuracy', 'Model accuracy')
model_latency = Histogram('model_latency_seconds', 'Prediction latency')
prediction_count = Counter('predictions_total', 'Total predictions')
error_count = Counter('prediction_errors_total', 'Prediction errors')


class PerformanceTracker:
    """Track and monitor model performance"""

    def __init__(self, config_path: str = "config/config.yaml"):
        with open(config_path, 'r') as f:
            self.config = yaml.safe_load(f)

        self.metrics_history = []
        logger.info("Performance tracker initialized")

    def track_prediction(self, y_true, y_pred, latency_ms):
        """Track single prediction"""
        prediction_count.inc()
        model_latency.observe(latency_ms / 1000)  # Convert to seconds

        # Calculate metrics
        error = abs(y_true - y_pred)

        self.metrics_history.append({
            'timestamp': datetime.now(),
            'y_true': y_true,
            'y_pred': y_pred,
            'error': error,
            'latency_ms': latency_ms
        })

    def track_batch(self, y_true, y_pred):
        """Track batch predictions"""
        mse = np.mean((y_true - y_pred) ** 2)
        mae = np.mean(np.abs(y_true - y_pred))

        logger.info(f"Batch metrics - MSE: {mse:.2f}, MAE: {mae:.2f}")

        return {'mse': mse, 'mae': mae}

    def get_recent_metrics(self, hours: int = 24):
        """Get recent performance metrics"""
        if not self.metrics_history:
            return {}

        df = pd.DataFrame(self.metrics_history)
        df = df[df['timestamp'] > datetime.now() - pd.Timedelta(hours=hours)]

        metrics = {
            'avg_error': df['error'].mean(),
            'max_error': df['error'].max(),
            'avg_latency_ms': df['latency_ms'].mean(),
            'prediction_count': len(df)
        }

        return metrics

    def check_performance_degradation(self, threshold: float = 0.9):
        """Check if performance has degraded"""
        recent_metrics = self.get_recent_metrics(hours=24)

        if not recent_metrics:
            return False

        # Check if error has increased significantly
        if recent_metrics['avg_error'] > threshold:
            logger.warning(f"Performance degradation detected: {recent_metrics}")
            return True

        return False
