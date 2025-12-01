# ============================================================================
# training/hyperparameter_tuner.py
# ============================================================================
"""
AirAware MLOps - Hyperparameter Tuner
Optuna-based hyperparameter optimization
"""

import logging
import optuna
import pandas as pd
import xgboost as xgb
from sklearn.model_selection import cross_val_score
import yaml

logger = logging.getLogger(__name__)


class HyperparameterTuner:
    """Hyperparameter tuning with Optuna"""

    def __init__(self, config_path: str = "config/config.yaml"):
        with open(config_path, 'r') as f:
            self.config = yaml.safe_load(f)

        self.tuning_config = self.config['training']['tuning']

        logger.info("Hyperparameter tuner initialized")

    def tune_xgboost(self, X_train, y_train):
        """Tune XGBoost hyperparameters"""
        logger.info("Starting XGBoost hyperparameter tuning")

        def objective(trial):
            params = {
                'max_depth': trial.suggest_int('max_depth', 3, 10),
                'learning_rate': trial.suggest_float('learning_rate', 0.01, 0.3),
                'n_estimators': trial.suggest_int('n_estimators', 50, 300),
                'subsample': trial.suggest_float('subsample', 0.6, 1.0),
                'colsample_bytree': trial.suggest_float('colsample_bytree', 0.6, 1.0),
                'min_child_weight': trial.suggest_int('min_child_weight', 1, 7)
            }

            model = xgb.XGBRegressor(**params, random_state=42, n_jobs=-1)

            # Cross-validation
            scores = cross_val_score(
                model, X_train, y_train,
                cv=5, scoring='neg_mean_squared_error',
                n_jobs=-1
            )

            return -scores.mean()  # Return negative MSE (minimize)

        study = optuna.create_study(direction='minimize')
        study.optimize(
            objective,
            n_trials=self.tuning_config['n_trials'],
            timeout=self.tuning_config['timeout']
        )

        logger.info(f"Best params: {study.best_params}")
        logger.info(f"Best score: {study.best_value}")

        return study.best_params

