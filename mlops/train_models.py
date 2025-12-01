# train_models.py
from training import ModelTrainer
from config import setup_logging

# Setup logging
setup_logging()

# Initialize trainer
trainer = ModelTrainer()

# Run training (30 days of data)
metrics = trainer.run_training_pipeline(days=30)

print(f"Training complete! Metrics: {metrics}")