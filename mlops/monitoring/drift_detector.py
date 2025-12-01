# ============================================================================
# monitoring/drift_detector.py
# ============================================================================
"""
AirAware MLOps - Drift Detector
Detect data drift and concept drift
"""

import logging
import pandas as pd
import numpy as np
from scipy import stats
import yaml

logger = logging.getLogger(__name__)


class DriftDetector:
    """Detect data and concept drift"""

    def __init__(self, config_path: str = "config/config.yaml"):
        with open(config_path, 'r') as f:
            self.config = yaml.safe_load(f)

        self.drift_config = self.config['monitoring']['drift']
        self.reference_data = None

        logger.info("Drift detector initialized")

    def set_reference_data(self, df: pd.DataFrame):
        """Set reference data for drift detection"""
        self.reference_data = df
        logger.info(f"Reference data set: {len(df)} samples")

    def detect_drift(self, current_data: pd.DataFrame):
        """Detect drift using KS test"""
        if self.reference_data is None:
            logger.warning("No reference data set")
            return {}

        drift_results = {}

        for col in current_data.select_dtypes(include=[np.number]).columns:
            if col in self.reference_data.columns:
                # Kolmogorov-Smirnov test
                statistic, p_value = stats.ks_2samp(
                    self.reference_data[col].dropna(),
                    current_data[col].dropna()
                )

                is_drift = p_value < self.drift_config['threshold']

                drift_results[col] = {
                    'statistic': statistic,
                    'p_value': p_value,
                    'is_drift': is_drift
                }

                if is_drift:
                    logger.warning(f"Drift detected in {col}: p-value={p_value:.4f}")

        return drift_results

    def detect_concept_drift(self, y_true, y_pred_old, y_pred_new):
        """Detect concept drift in model predictions"""
        # Compare errors
        error_old = np.abs(y_true - y_pred_old)
        error_new = np.abs(y_true - y_pred_new)

        # Statistical test
        statistic, p_value = stats.ttest_ind(error_old, error_new)

        is_drift = p_value < 0.05 and error_new.mean() > error_old.mean()

        if is_drift:
            logger.warning("Concept drift detected")

        return {
            'statistic': statistic,
            'p_value': p_value,
            'is_drift': is_drift
        }

