"""
AirAware MLOps - Feature Engineering
Extract and engineer features from raw sensor data for ML models
"""

import logging
import pandas as pd
import numpy as np
from typing import List, Dict, Optional
from datetime import datetime
import yaml

logger = logging.getLogger(__name__)


class FeatureEngineer:
    """Feature engineering for air quality data"""

    def __init__(self, config_path: str = "config/config.yaml"):
        """
        Initialize feature engineer with configuration

        Args:
            config_path: Path to configuration file
        """
        with open(config_path, 'r') as f:
            self.config = yaml.safe_load(f)

        self.feature_cols = self.config['data']['features']
        self.time_windows = self.config['data']['time_windows']

        logger.info("Feature engineer initialized")

    def engineer_features(
            self,
            df: pd.DataFrame,
            include_temporal: bool = True,
            include_rolling: bool = True,
            include_interactions: bool = True
    ) -> pd.DataFrame:
        """
        Engineer all features from raw sensor data

        Args:
            df: Raw sensor readings DataFrame
            include_temporal: Add temporal features
            include_rolling: Add rolling statistics
            include_interactions: Add feature interactions

        Returns:
            DataFrame with engineered features
        """
        df = df.copy()

        # Ensure timestamp is datetime
        if 'timestamp' in df.columns:
            df['timestamp'] = pd.to_datetime(df['timestamp'])
            df = df.sort_values('timestamp').reset_index(drop=True)

        # Temporal features
        if include_temporal:
            df = self._add_temporal_features(df)

        # Rolling statistics
        if include_rolling:
            df = self._add_rolling_features(df)

        # Rate of change
        df = self._add_rate_of_change(df)

        # Feature interactions
        if include_interactions:
            df = self._add_interaction_features(df)

        # Calculate AQI
        df = self._calculate_aqi(df)

        # Fill missing values
        df = self._handle_missing_values(df)

        logger.info(f"Feature engineering complete: {df.shape[1]} features")
        return df

    def _add_temporal_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """Add time-based features"""
        if 'timestamp' not in df.columns:
            return df

        df['hour_of_day'] = df['timestamp'].dt.hour
        df['day_of_week'] = df['timestamp'].dt.dayofweek
        df['day_of_month'] = df['timestamp'].dt.day
        df['month'] = df['timestamp'].dt.month
        df['is_weekend'] = df['day_of_week'].isin([5, 6]).astype(int)

        # Time of day categories
        df['time_category'] = pd.cut(
            df['hour_of_day'],
            bins=[0, 6, 12, 18, 24],
            labels=['night', 'morning', 'afternoon', 'evening'],
            include_lowest=True
        )

        # Cyclical encoding for hour (preserves circular nature)
        df['hour_sin'] = np.sin(2 * np.pi * df['hour_of_day'] / 24)
        df['hour_cos'] = np.cos(2 * np.pi * df['hour_of_day'] / 24)

        # Cyclical encoding for day of week
        df['day_sin'] = np.sin(2 * np.pi * df['day_of_week'] / 7)
        df['day_cos'] = np.cos(2 * np.pi * df['day_of_week'] / 7)

        logger.debug("Added temporal features")
        return df

    def _add_rolling_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """Add rolling window statistics"""
        sensor_cols = ['temperature', 'humidity', 'co2', 'voc', 'pm25', 'pm10']

        for col in sensor_cols:
            if col not in df.columns:
                continue

            # Rolling mean and std for different windows
            for window in ['1h', '6h', '24h']:
                window_size = self._parse_time_window(window)

                df[f'{col}_mean_{window}'] = df[col].rolling(
                    window=window_size, min_periods=1
                ).mean()

                df[f'{col}_std_{window}'] = df[col].rolling(
                    window=window_size, min_periods=1
                ).std()

                df[f'{col}_min_{window}'] = df[col].rolling(
                    window=window_size, min_periods=1
                ).min()

                df[f'{col}_max_{window}'] = df[col].rolling(
                    window=window_size, min_periods=1
                ).max()

        logger.debug("Added rolling features")
        return df

    def _add_rate_of_change(self, df: pd.DataFrame) -> pd.DataFrame:
        """Calculate rate of change for sensor readings"""
        sensor_cols = ['co2', 'pm25', 'pm10', 'voc']

        for col in sensor_cols:
            if col not in df.columns:
                continue

            # First difference (rate of change)
            df[f'{col}_diff'] = df[col].diff()

            # Percentage change
            df[f'{col}_pct_change'] = df[col].pct_change()

            # Acceleration (second derivative)
            df[f'{col}_acceleration'] = df[f'{col}_diff'].diff()

        logger.debug("Added rate of change features")
        return df

    def _add_interaction_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """Add feature interactions"""
        # Temperature-Humidity interaction
        if 'temperature' in df.columns and 'humidity' in df.columns:
            df['temp_humidity_interaction'] = df['temperature'] * df['humidity']

            # Heat index approximation
            df['heat_index'] = (
                    df['temperature'] +
                    0.5555 * (6.11 * np.exp(5417.7530 * ((1/273.16) - (1/(df['temperature']+273.15)))) - 10)
            )

        # PM2.5 to PM10 ratio
        if 'pm25' in df.columns and 'pm10' in df.columns:
            df['pm25_pm10_ratio'] = df['pm25'] / (df['pm10'] + 1e-6)

        # Total particulate matter
        if 'pm25' in df.columns and 'pm10' in df.columns:
            df['total_pm'] = df['pm25'] + df['pm10']

        # CO2-VOC combined risk
        if 'co2' in df.columns and 'voc' in df.columns:
            df['co2_voc_risk'] = (
                    (df['co2'] / 1000) * 0.5 +
                    (df['voc'] / 1) * 0.5
            )

        logger.debug("Added interaction features")
        return df

    def _calculate_aqi(self, df: pd.DataFrame) -> pd.DataFrame:
        """Calculate Air Quality Index (AQI) based on EPA standards"""
        if 'pm25' not in df.columns:
            return df

        # EPA AQI breakpoints for PM2.5
        breakpoints = self.config['aqi']['pm25_breakpoints']

        def calculate_aqi_value(pm25_value):
            if pd.isna(pm25_value):
                return np.nan

            for c_low, c_high, i_low, i_high in breakpoints:
                if c_low <= pm25_value <= c_high:
                    # Linear interpolation formula
                    aqi = ((i_high - i_low) / (c_high - c_low)) * (pm25_value - c_low) + i_low
                    return int(aqi)

            # If beyond scale, return maximum
            return 500

        df['aqi_pm25'] = df['pm25'].apply(calculate_aqi_value)

        # AQI category
        df['aqi_category'] = pd.cut(
            df['aqi_pm25'],
            bins=[0, 50, 100, 150, 200, 300, 500],
            labels=['Good', 'Moderate', 'Unhealthy for Sensitive', 'Unhealthy', 'Very Unhealthy', 'Hazardous']
        )

        logger.debug("Calculated AQI features")
        return df

    def _handle_missing_values(self, df: pd.DataFrame) -> pd.DataFrame:
        """Handle missing values in features"""
        # Forward fill for time series continuity
        df = df.ffill(limit=5)

        # Backward fill for remaining
        df = df.bfill(limit=5)

        # Fill remaining with median
        numeric_cols = df.select_dtypes(include=[np.number]).columns
        for col in numeric_cols:
            if df[col].isna().any():
                df[col].fillna(df[col].median(), inplace=True)

        return df

    def _parse_time_window(self, window: str) -> int:
        """Convert time window string to number of samples"""
        # Assuming 5-minute intervals (12 per hour)
        if window == '1h':
            return 12
        elif window == '6h':
            return 72
        elif window == '24h':
            return 288
        return 12

    def select_features(
            self,
            df: pd.DataFrame,
            feature_list: Optional[List[str]] = None
    ) -> pd.DataFrame:
        """
        Select specific features for modeling

        Args:
            df: DataFrame with all features
            feature_list: List of features to select (None = use config)

        Returns:
            DataFrame with selected features
        """
        if feature_list is None:
            feature_list = self.feature_cols

        # Add features that exist in DataFrame
        available_features = [f for f in feature_list if f in df.columns]

        if len(available_features) < len(feature_list):
            missing = set(feature_list) - set(available_features)
            logger.warning(f"Missing features: {missing}")

        return df[available_features]

    def create_sequences(
            self,
            df: pd.DataFrame,
            sequence_length: int = 24,
            target_col: str = 'aqi_pm25'
    ) -> tuple:
        """
        Create sequences for time series models (LSTM)

        Args:
            df: DataFrame with features
            sequence_length: Number of timesteps in sequence
            target_col: Target variable column name

        Returns:
            Tuple of (X_sequences, y_targets)
        """
        feature_cols = [c for c in df.columns if c != target_col and c != 'timestamp']

        X, y = [], []

        for i in range(len(df) - sequence_length):
            X.append(df[feature_cols].iloc[i:i+sequence_length].values)
            y.append(df[target_col].iloc[i+sequence_length])

        X = np.array(X)
        y = np.array(y)

        logger.info(f"Created sequences: X shape {X.shape}, y shape {y.shape}")
        return X, y


# Example usage
if __name__ == "__main__":
    # Configure logging
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )

    # Create sample data
    dates = pd.date_range('2025-01-01', periods=100, freq='5min')
    sample_data = pd.DataFrame({
        'timestamp': dates,
        'temperature': np.random.normal(25, 5, 100),
        'humidity': np.random.normal(60, 10, 100),
        'co2': np.random.normal(800, 200, 100),
        'voc': np.random.normal(0.5, 0.2, 100),
        'pm25': np.random.normal(15, 5, 100),
        'pm10': np.random.normal(30, 10, 100)
    })

    # Initialize feature engineer
    engineer = FeatureEngineer()

    # Engineer features
    engineered_df = engineer.engineer_features(sample_data)

    print(f"\nOriginal shape: {sample_data.shape}")
    print(f"Engineered shape: {engineered_df.shape}")
    print(f"\nNew features:\n{engineered_df.columns.tolist()}")
    print(f"\nSample data:\n{engineered_df.head()}")