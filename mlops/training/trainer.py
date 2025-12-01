"""
AirAware MLOps - Model Trainer
End-to-end training pipeline
"""

import logging
import pandas as pd
from datetime import datetime
import yaml
import mlflow  # ✅ This imports the REAL mlflow library (installed via pip)

# ✅ Import from OUR renamed folder (mlflow_tracking)
from mlflow_tracking import MLflowTracker

# ✅ Import our data modules
from data import AirAwareDataLoader, FeatureEngineer, DataValidator, DataPreprocessor

# ✅ Import our model modules
from models import AnomalyDetector, AQIPredictor, TimeSeriesForecaster, EnsembleModel

logger = logging.getLogger(__name__)


class ModelTrainer:
    """Complete model training pipeline"""

    def __init__(self, config_path: str = "config/config.yaml"):
        with open(config_path, 'r') as f:
            self.config = yaml.safe_load(f)

        self.data_loader = AirAwareDataLoader(config_path)
        self.feature_engineer = FeatureEngineer(config_path)
        self.validator = DataValidator(config_path)
        self.preprocessor = DataPreprocessor()

        self.models = {}

        logger.info("Model trainer initialized")

    def run_training_pipeline(self, days: int = 30):
        """Execute complete training pipeline"""
        logger.info(f"Starting training pipeline with {days} days of data")

        # Start MLflow run
        mlflow.start_run(run_name=f"training_{datetime.now().strftime('%Y%m%d_%H%M%S')}")

        try:
            # 1. Load data
            logger.info("Step 1/7: Loading data")
            train_df, val_df = self.data_loader.load_training_data(days=days)

            # 2. Validate data
            logger.info("Step 2/7: Validating data")
            is_valid, report = self.validator.validate_dataframe(train_df)
            if not is_valid:
                logger.error(f"Data validation failed: {report['issues']}")
                raise ValueError("Data validation failed")

            # Clean data
            train_df = self.validator.clean_invalid_data(train_df, strategy='clip')
            val_df = self.validator.clean_invalid_data(val_df, strategy='clip')

            # 3. Feature engineering
            logger.info("Step 3/7: Engineering features")
            train_df = self.feature_engineer.engineer_features(train_df)
            val_df = self.feature_engineer.engineer_features(val_df)

            # 4. Preprocessing
            logger.info("Step 4/7: Preprocessing data")
            X_train, y_train = self.preprocessor.fit_transform(
                train_df, target_col='aqi_pm25'
            )
            X_val, y_val = self.preprocessor.transform(val_df, target_col='aqi_pm25')

            # Save preprocessor
            self.preprocessor.save()

            # 5. Train models
            logger.info("Step 5/7: Training models")
            self._train_anomaly_detector(X_train)
            self._train_aqi_predictor(X_train, y_train, X_val, y_val)

            # 6. Evaluate models
            logger.info("Step 6/7: Evaluating models")
            metrics = self._evaluate_models(X_val, y_val)

            # Log metrics to MLflow
            for model_name, model_metrics in metrics.items():
                for metric_name, value in model_metrics.items():
                    mlflow.log_metric(f"{model_name}_{metric_name}", value)

            # 7. Save models
            logger.info("Step 7/7: Saving models")
            self._save_models()

            # Log artifacts
            mlflow.log_artifacts("models")

            logger.info("Training pipeline completed successfully")
            mlflow.log_param("status", "success")

            return metrics

        except Exception as e:
            logger.error(f"Training pipeline failed: {e}")
            mlflow.log_param("status", "failed")
            mlflow.log_param("error", str(e))
            raise

        finally:
            mlflow.end_run()
            self.data_loader.close()

    def _train_anomaly_detector(self, X_train):
        """Train anomaly detection model"""
        logger.info("Training anomaly detector")

        detector = AnomalyDetector()
        detector.fit(X_train)

        self.models['anomaly_detector'] = detector
        logger.info("Anomaly detector trained")

    def _train_aqi_predictor(self, X_train, y_train, X_val, y_val):
        """Train AQI prediction model"""
        logger.info("Training AQI predictor")

        predictor = AQIPredictor()
        predictor.fit(X_train, y_train, X_val, y_val)

        # Log feature importance
        importance = predictor.get_feature_importance()
        logger.info(f"Top features:\n{importance.head(10)}")

        self.models['aqi_predictor'] = predictor
        logger.info("AQI predictor trained")

    def _evaluate_models(self, X_val, y_val):
        """Evaluate all models"""
        metrics = {}

        # Evaluate anomaly detector
        if 'anomaly_detector' in self.models:
            detector = self.models['anomaly_detector']
            anomaly_results = detector.detect_anomalies(X_val)
            metrics['anomaly_detector'] = {
                'anomaly_rate': anomaly_results['is_anomaly'].mean(),
                'avg_score': anomaly_results['anomaly_score'].mean()
            }

        # Evaluate AQI predictor
        if 'aqi_predictor' in self.models:
            predictor = self.models['aqi_predictor']
            metrics['aqi_predictor'] = predictor.evaluate(X_val, y_val)

        return metrics

    def _save_models(self):
        """Save all models"""
        for name, model in self.models.items():
            model.save(f"models/{name}.pkl")


# Example usage
if __name__ == "__main__":
    import sys

    # Setup logging
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        handlers=[
            logging.FileHandler('logs/training.log'),
            logging.StreamHandler(sys.stdout)
        ]
    )

    try:
        # Initialize trainer
        trainer = ModelTrainer()

        # Run training
        metrics = trainer.run_training_pipeline(days=7)  # Start with 7 days for testing

        print("\n" + "="*60)
        print("✅ TRAINING COMPLETED SUCCESSFULLY!")
        print("="*60)
        print("\nMetrics:")
        for model_name, model_metrics in metrics.items():
            print(f"\n{model_name}:")
            for metric_name, value in model_metrics.items():
                print(f"  {metric_name}: {value:.4f}")
        print("\n" + "="*60)

    except Exception as e:
        print("\n" + "="*60)
        print("❌ TRAINING FAILED!")
        print("="*60)
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)