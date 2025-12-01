# ============================================================================
# training/evaluator.py
# ============================================================================
"""
AirAware MLOps - Model Evaluator
Comprehensive model evaluation and reporting
"""

import logging
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from sklearn.metrics import (
    confusion_matrix, classification_report,
    mean_squared_error, mean_absolute_error, r2_score
)

logger = logging.getLogger(__name__)


class ModelEvaluator:
    """Evaluate model performance"""

    def __init__(self):
        logger.info("Model evaluator initialized")

    def evaluate_regression(self, y_true, y_pred):
        """Evaluate regression model"""
        metrics = {
            'mse': mean_squared_error(y_true, y_pred),
            'rmse': np.sqrt(mean_squared_error(y_true, y_pred)),
            'mae': mean_absolute_error(y_true, y_pred),
            'r2': r2_score(y_true, y_pred)
        }

        logger.info(f"Regression metrics: {metrics}")
        return metrics

    def plot_predictions(self, y_true, y_pred, save_path=None):
        """Plot actual vs predicted"""
        plt.figure(figsize=(10, 6))
        plt.scatter(y_true, y_pred, alpha=0.5)
        plt.plot([y_true.min(), y_true.max()],
                 [y_true.min(), y_true.max()],
                 'r--', lw=2)
        plt.xlabel('Actual')
        plt.ylabel('Predicted')
        plt.title('Actual vs Predicted AQI')

        if save_path:
            plt.savefig(save_path)
        plt.close()

    def plot_feature_importance(self, importance_df, save_path=None):
        """Plot feature importance"""
        plt.figure(figsize=(10, 8))
        sns.barplot(data=importance_df.head(20), x='importance', y='feature')
        plt.title('Top 20 Feature Importances')
        plt.tight_layout()

        if save_path:
            plt.savefig(save_path)
        plt.close()

