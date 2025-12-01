"""
AirAware MLOps - Data Validator
Validates data quality and detects anomalies in sensor readings
"""

import logging
import pandas as pd
import numpy as np
from typing import Dict, List, Tuple
import yaml

logger = logging.getLogger(__name__)


class DataValidator:
    """Validate sensor data quality"""

    def __init__(self, config_path: str = "config/config.yaml"):
        """
        Initialize data validator

        Args:
            config_path: Path to configuration file
        """
        with open(config_path, 'r') as f:
            self.config = yaml.safe_load(f)

        self.validation_rules = self.config['data']['validation']

        logger.info("Data validator initialized")

    def validate_dataframe(self, df: pd.DataFrame) -> Tuple[bool, Dict]:
        """
        Validate entire dataframe

        Args:
            df: Input dataframe

        Returns:
            Tuple of (is_valid, validation_report)
        """
        report = {
            'is_valid': True,
            'total_rows': len(df),
            'issues': [],
            'warnings': [],
            'statistics': {}
        }

        # Check required columns
        required_cols = ['temperature', 'humidity', 'co2', 'pm25', 'timestamp']
        missing_cols = [col for col in required_cols if col not in df.columns]

        if missing_cols:
            report['is_valid'] = False
            report['issues'].append(f"Missing required columns: {missing_cols}")
            return False, report

        # Validate each column
        for col, rules in self.validation_rules.items():
            if col not in df.columns:
                continue

            col_report = self._validate_column(df[col], col, rules)

            if col_report['invalid_count'] > 0:
                report['warnings'].append(
                    f"{col}: {col_report['invalid_count']} invalid values"
                )

            report['statistics'][col] = col_report

        # Check for duplicates
        duplicates = df.duplicated(subset=['timestamp', 'sensorId'], keep=False).sum()
        if duplicates > 0:
            report['warnings'].append(f"Found {duplicates} duplicate records")

        # Check for missing values
        missing_report = self._check_missing_values(df)
        report['statistics']['missing_values'] = missing_report

        # Check for outliers
        outlier_report = self._detect_outliers(df)
        report['statistics']['outliers'] = outlier_report

        # Overall validation
        if report['issues']:
            report['is_valid'] = False

        logger.info(f"Validation complete: {report['is_valid']}")
        return report['is_valid'], report

    def _validate_column(
            self,
            series: pd.Series,
            col_name: str,
            rules: Dict
    ) -> Dict:
        """Validate a single column"""
        report = {
            'total': len(series),
            'invalid_count': 0,
            'invalid_indices': [],
            'min': series.min(),
            'max': series.max(),
            'mean': series.mean(),
            'std': series.std()
        }

        # Range validation
        if 'min' in rules and 'max' in rules:
            invalid_mask = (series < rules['min']) | (series > rules['max'])
            invalid_indices = series[invalid_mask].index.tolist()

            report['invalid_count'] = len(invalid_indices)
            report['invalid_indices'] = invalid_indices[:10]  # First 10

        return report

    def _check_missing_values(self, df: pd.DataFrame) -> Dict:
        """Check for missing values"""
        missing = df.isnull().sum()
        missing_pct = (missing / len(df)) * 100

        return {
            'counts': missing.to_dict(),
            'percentages': missing_pct.to_dict(),
            'total_missing': missing.sum()
        }

    def _detect_outliers(
            self,
            df: pd.DataFrame,
            threshold: float = 3.0
    ) -> Dict:
        """
        Detect outliers using Z-score method

        Args:
            df: Input dataframe
            threshold: Z-score threshold (default: 3.0)

        Returns:
            Dictionary with outlier statistics
        """
        outlier_report = {}

        numeric_cols = df.select_dtypes(include=[np.number]).columns

        for col in numeric_cols:
            if col in df.columns:
                # Calculate Z-scores
                z_scores = np.abs((df[col] - df[col].mean()) / df[col].std())
                outliers = z_scores > threshold

                outlier_report[col] = {
                    'count': outliers.sum(),
                    'percentage': (outliers.sum() / len(df)) * 100,
                    'indices': df[outliers].index.tolist()[:10]
                }

        return outlier_report

    def clean_invalid_data(
            self,
            df: pd.DataFrame,
            strategy: str = 'remove'
    ) -> pd.DataFrame:
        """
        Clean invalid data based on validation rules

        Args:
            df: Input dataframe
            strategy: 'remove', 'clip', or 'interpolate'

        Returns:
            Cleaned dataframe
        """
        df_clean = df.copy()

        for col, rules in self.validation_rules.items():
            if col not in df_clean.columns:
                continue

            min_val = rules.get('min')
            max_val = rules.get('max')

            if strategy == 'remove':
                # Remove rows with invalid values
                mask = (df_clean[col] >= min_val) & (df_clean[col] <= max_val)
                df_clean = df_clean[mask]

            elif strategy == 'clip':
                # Clip values to valid range
                df_clean[col] = df_clean[col].clip(min_val, max_val)

            elif strategy == 'interpolate':
                # Mark invalid as NaN and interpolate
                invalid_mask = (df_clean[col] < min_val) | (df_clean[col] > max_val)
                df_clean.loc[invalid_mask, col] = np.nan
                df_clean[col] = df_clean[col].interpolate(method='linear')

        logger.info(f"Data cleaned: {len(df)} -> {len(df_clean)} rows ({strategy})")
        return df_clean

    def validate_real_time(self, reading: Dict) -> Tuple[bool, List[str]]:
        """
        Validate a single real-time reading

        Args:
            reading: Dictionary with sensor reading

        Returns:
            Tuple of (is_valid, error_messages)
        """
        errors = []

        # Check required fields
        required = ['temperature', 'humidity', 'co2', 'pm25']
        for field in required:
            if field not in reading:
                errors.append(f"Missing required field: {field}")

        if errors:
            return False, errors

        # Validate ranges
        for field, rules in self.validation_rules.items():
            if field in reading:
                value = reading[field]
                min_val = rules.get('min')
                max_val = rules.get('max')

                if value < min_val or value > max_val:
                    errors.append(
                        f"{field} value {value} outside valid range [{min_val}, {max_val}]"
                    )

        is_valid = len(errors) == 0
        return is_valid, errors


# Example usage
if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)

    # Create sample data
    sample_data = pd.DataFrame({
        'timestamp': pd.date_range('2025-01-01', periods=100, freq='5min'),
        'sensorId': 'SENSOR_001',
        'temperature': np.random.normal(25, 5, 100),
        'humidity': np.random.normal(60, 10, 100),
        'co2': np.random.normal(800, 200, 100),
        'pm25': np.random.normal(15, 5, 100)
    })

    # Add some invalid values
    sample_data.loc[10, 'temperature'] = 150  # Invalid
    sample_data.loc[20, 'co2'] = -100  # Invalid

    # Validate
    validator = DataValidator()
    is_valid, report = validator.validate_dataframe(sample_data)

    print(f"\nValidation Result: {is_valid}")
    print(f"Total Rows: {report['total_rows']}")
    print(f"Issues: {report['issues']}")
    print(f"Warnings: {report['warnings']}")
    print(f"\nStatistics: {report['statistics']}")

    # Clean data
    cleaned_data = validator.clean_invalid_data(sample_data, strategy='clip')
    print(f"\nCleaned Data Shape: {cleaned_data.shape}")