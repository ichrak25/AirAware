"""
AirAware MLOps - Logging Configuration
Centralized logging setup for all MLOps components
"""

import logging
import logging.handlers
import os
from pathlib import Path
import yaml


def setup_logging(config_path: str = "config/config.yaml", log_dir: str = "logs"):
    """
    Setup logging configuration for MLOps module

    Args:
        config_path: Path to configuration file
        log_dir: Directory for log files
    """
    # Create logs directory
    Path(log_dir).mkdir(parents=True, exist_ok=True)

    # Load config
    with open(config_path, 'r') as f:
        config = yaml.safe_load(f)

    log_config = config.get('logging', {})

    # Configure root logger
    log_level = getattr(logging, log_config.get('level', 'INFO'))
    log_format = log_config.get('format', '%(asctime)s - %(name)s - %(levelname)s - %(message)s')

    # Create formatter
    formatter = logging.Formatter(log_format)

    # Console handler
    console_handler = logging.StreamHandler()
    console_handler.setLevel(log_level)
    console_handler.setFormatter(formatter)

    # File handler with rotation
    log_file = os.path.join(log_dir, log_config.get('file', 'mlops.log'))
    max_bytes = log_config.get('max_bytes', 10485760)  # 10MB
    backup_count = log_config.get('backup_count', 5)

    file_handler = logging.handlers.RotatingFileHandler(
        log_file,
        maxBytes=max_bytes,
        backupCount=backup_count
    )
    file_handler.setLevel(log_level)
    file_handler.setFormatter(formatter)

    # Configure root logger
    root_logger = logging.getLogger()
    root_logger.setLevel(log_level)
    root_logger.addHandler(console_handler)
    root_logger.addHandler(file_handler)

    # Suppress noisy libraries
    logging.getLogger('urllib3').setLevel(logging.WARNING)
    logging.getLogger('pymongo').setLevel(logging.WARNING)
    logging.getLogger('tensorflow').setLevel(logging.ERROR)

    logging.info("Logging configured successfully")
    logging.info(f"Log file: {log_file}")
    logging.info(f"Log level: {log_level}")


def get_logger(name: str) -> logging.Logger:
    """
    Get a logger instance

    Args:
        name: Logger name (typically __name__)

    Returns:
        Logger instance
    """
    return logging.getLogger(name)


# Example usage
if __name__ == "__main__":
    setup_logging()
    logger = get_logger(__name__)

    logger.debug("Debug message")
    logger.info("Info message")
    logger.warning("Warning message")
    logger.error("Error message")
    logger.critical("Critical message")