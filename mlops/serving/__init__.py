"""
AirAware MLOps Serving Module
"""

# Don't import model_server here - it causes circular import
# from .model_server import ModelServer  # ❌ REMOVED

from .batch_predictor import BatchPredictor
from .realtime_predictor import RealtimePredictor

__all__ = [
    'BatchPredictor',
    'RealtimePredictor'
]