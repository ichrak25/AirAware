"""
AirAware MLOps - Data Preprocessor
Standardization, normalization, and encoding for ML models
FIXED: Properly handles and removes string columns
"""

import logging
import pandas as pd
import numpy as np
from typing import Dict, List, Optional, Tuple
from sklearn.preprocessing import StandardScaler, MinMaxScaler, LabelEncoder
from sklearn.model_selection import train_test_split
import joblib
import os

logger = logging.getLogger(__name__)


class DataPreprocessor:
    """Preprocess data for machine learning models"""

    def __init__(self):
        """Initialize preprocessor"""
        self.scalers = {}
        self.encoders = {}
        self.feature_names = []
        self.columns_to_drop = []  # ✅ NEW: Track columns to drop

        logger.info("Data preprocessor initialized")

    def fit_transform(
            self,
            df: pd.DataFrame,
            target_col: Optional[str] = None,
            scaling_method: str = 'standard'
    ) -> Tuple[pd.DataFrame, Optional[pd.Series]]:
        """
        Fit and transform data

        Args:
            df: Input dataframe
            target_col: Target column name (if supervised learning)
            scaling_method: 'standard' or 'minmax'

        Returns:
            Tuple of (X_transformed, y) if target_col provided, else (X_transformed, None)
        """
        df = df.copy()

        # Separate features and target
        if target_col and target_col in df.columns:
            y = df[target_col]
            X = df.drop(columns=[target_col])
        else:
            y = None
            X = df

        # ✅ CRITICAL FIX: Drop ID and timestamp columns BEFORE encoding
        self.columns_to_drop = self._identify_columns_to_drop(X)
        if self.columns_to_drop:
            logger.info(f"Dropping non-feature columns: {self.columns_to_drop}")
            X = X.drop(columns=self.columns_to_drop)

        # Handle categorical variables (convert to numeric)
        X = self._encode_categorical(X, fit=True)

        # Handle missing values
        X = self._handle_missing(X)

        # Scale numerical features
        X = self._scale_features(X, method=scaling_method, fit=True)

        # ✅ Verify all columns are numeric
        non_numeric = X.select_dtypes(exclude=[np.number]).columns.tolist()
        if non_numeric:
            logger.warning(f"Found non-numeric columns after preprocessing: {non_numeric}")
            logger.warning("Dropping these columns...")
            X = X.drop(columns=non_numeric)

        # Store feature names
        self.feature_names = X.columns.tolist()

        logger.info(f"Data preprocessed: {X.shape[0]} samples, {X.shape[1]} features")

        return X, y

    def transform(
            self,
            df: pd.DataFrame,
            target_col: Optional[str] = None
    ) -> Tuple[pd.DataFrame, Optional[pd.Series]]:
        """
        Transform new data using fitted preprocessor

        Args:
            df: Input dataframe
            target_col: Target column name

        Returns:
            Tuple of (X_transformed, y)
        """
        df = df.copy()

        # Separate features and target
        if target_col and target_col in df.columns:
            y = df[target_col]
            X = df.drop(columns=[target_col])
        else:
            y = None
            X = df

        # ✅ Drop same columns as in training
        if self.columns_to_drop:
            existing_drop_cols = [col for col in self.columns_to_drop if col in X.columns]
            if existing_drop_cols:
                X = X.drop(columns=existing_drop_cols)

        # Encode categorical variables
        X = self._encode_categorical(X, fit=False)

        # Handle missing values
        X = self._handle_missing(X)

        # Scale features
        X = self._scale_features(X, fit=False)

        # ✅ Remove any remaining non-numeric columns
        non_numeric = X.select_dtypes(exclude=[np.number]).columns.tolist()
        if non_numeric:
            logger.warning(f"Dropping non-numeric columns: {non_numeric}")
            X = X.drop(columns=non_numeric)

        # Ensure columns match training data
        missing_cols = set(self.feature_names) - set(X.columns)
        extra_cols = set(X.columns) - set(self.feature_names)

        if missing_cols:
            logger.warning(f"Missing columns: {missing_cols}")
            for col in missing_cols:
                X[col] = 0

        if extra_cols:
            logger.warning(f"Extra columns (will be dropped): {extra_cols}")
            X = X.drop(columns=list(extra_cols))

        # Reorder columns to match training
        X = X[self.feature_names]

        return X, y

    def _identify_columns_to_drop(self, df: pd.DataFrame) -> List[str]:
        """
        Identify columns that should be dropped before training

        These include:
        - ID columns (sensorId, deviceId, etc.)
        - Timestamp columns
        - Text/string columns that aren't categorical features

        Returns:
            List of column names to drop
        """
        columns_to_drop = []

        # List of known ID/timestamp column patterns
        id_patterns = ['id', 'sensorid', 'deviceid', 'userid', 'tenantid', '_id']
        time_patterns = ['timestamp', 'datetime', 'date', 'time', 'createdat', 'updatedat']

        for col in df.columns:
            col_lower = col.lower()

            # Check if it's an ID column
            if any(pattern in col_lower for pattern in id_patterns):
                columns_to_drop.append(col)
                continue

            # Check if it's a timestamp column
            if any(pattern in col_lower for pattern in time_patterns):
                columns_to_drop.append(col)
                continue

            # Check if it's a pandas datetime
            if pd.api.types.is_datetime64_any_dtype(df[col]):
                columns_to_drop.append(col)
                continue

        return columns_to_drop

    def _encode_categorical(
            self,
            df: pd.DataFrame,
            fit: bool = True
    ) -> pd.DataFrame:
        """Encode categorical variables"""
        df = df.copy()

        categorical_cols = df.select_dtypes(include=['object', 'category']).columns

        for col in categorical_cols:
            # ✅ Skip if already in columns_to_drop
            if col in self.columns_to_drop:
                continue

            if fit:
                # Create and fit encoder
                encoder = LabelEncoder()
                df[col] = encoder.fit_transform(df[col].astype(str))
                self.encoders[col] = encoder
                logger.debug(f"Fitted encoder for {col}")
            else:
                # Use existing encoder
                if col in self.encoders:
                    encoder = self.encoders[col]
                    # Handle unseen categories
                    known_categories = set(encoder.classes_)
                    df[col] = df[col].astype(str).apply(
                        lambda x: x if x in known_categories else encoder.classes_[0]
                    )
                    df[col] = encoder.transform(df[col])

        return df

    def _handle_missing(self, df: pd.DataFrame) -> pd.DataFrame:
        """Handle missing values"""
        df = df.copy()

        # ✅ Use ffill() and bfill() instead of deprecated fillna(method=...)
        df = df.ffill(limit=5)  # Forward fill
        df = df.bfill(limit=5)  # Backward fill

        # Fill remaining with median for numeric columns
        numeric_cols = df.select_dtypes(include=[np.number]).columns
        for col in numeric_cols:
            if df[col].isna().any():
                median_value = df[col].median()
                if pd.isna(median_value):
                    median_value = 0  # Fallback if all values are NaN
                df[col].fillna(median_value, inplace=True)

        # Fill categorical with mode
        categorical_cols = df.select_dtypes(include=['object', 'category']).columns
        for col in categorical_cols:
            if df[col].isna().any():
                mode_values = df[col].mode()
                if len(mode_values) > 0:
                    df[col].fillna(mode_values[0], inplace=True)
                else:
                    df[col].fillna('unknown', inplace=True)

        return df

    def _scale_features(
            self,
            df: pd.DataFrame,
            method: str = 'standard',
            fit: bool = True
    ) -> pd.DataFrame:
        """Scale numerical features"""
        df = df.copy()

        # Select numerical columns
        numeric_cols = df.select_dtypes(include=[np.number]).columns.tolist()

        # Skip already normalized features
        skip_cols = ['hour_sin', 'hour_cos', 'day_sin', 'day_cos', 'is_weekend']
        numeric_cols = [c for c in numeric_cols if c not in skip_cols]

        if not numeric_cols:
            return df

        if fit:
            # Create and fit scaler
            if method == 'standard':
                scaler = StandardScaler()
            elif method == 'minmax':
                scaler = MinMaxScaler()
            else:
                raise ValueError(f"Unknown scaling method: {method}")

            df[numeric_cols] = scaler.fit_transform(df[numeric_cols])
            self.scalers['features'] = scaler
            logger.debug(f"Fitted {method} scaler")
        else:
            # Use existing scaler
            if 'features' in self.scalers:
                scaler = self.scalers['features']
                df[numeric_cols] = scaler.transform(df[numeric_cols])

        return df

    def split_data(
            self,
            X: pd.DataFrame,
            y: pd.Series,
            test_size: float = 0.2,
            val_size: float = 0.1,
            random_state: int = 42
    ) -> Tuple:
        """
        Split data into train, validation, and test sets

        Args:
            X: Features
            y: Target
            test_size: Test set proportion
            val_size: Validation set proportion (from remaining data)
            random_state: Random seed

        Returns:
            Tuple of (X_train, X_val, X_test, y_train, y_val, y_test)
        """
        # First split: train+val vs test
        X_temp, X_test, y_temp, y_test = train_test_split(
            X, y, test_size=test_size, random_state=random_state
        )

        # Second split: train vs val
        val_size_adjusted = val_size / (1 - test_size)
        X_train, X_val, y_train, y_val = train_test_split(
            X_temp, y_temp, test_size=val_size_adjusted, random_state=random_state
        )

        logger.info(f"Data split: Train={len(X_train)}, Val={len(X_val)}, Test={len(X_test)}")

        return X_train, X_val, X_test, y_train, y_val, y_test

    def save(self, path: str = "models/preprocessor.pkl"):
        """Save preprocessor to disk"""
        os.makedirs(os.path.dirname(path), exist_ok=True)

        preprocessor_data = {
            'scalers': self.scalers,
            'encoders': self.encoders,
            'feature_names': self.feature_names,
            'columns_to_drop': self.columns_to_drop  # ✅ Save this too
        }

        joblib.dump(preprocessor_data, path)
        logger.info(f"Preprocessor saved to {path}")

    @classmethod
    def load(cls, path: str = "models/preprocessor.pkl"):
        """Load preprocessor from disk"""
        preprocessor = cls()

        preprocessor_data = joblib.load(path)
        preprocessor.scalers = preprocessor_data['scalers']
        preprocessor.encoders = preprocessor_data['encoders']
        preprocessor.feature_names = preprocessor_data['feature_names']
        preprocessor.columns_to_drop = preprocessor_data.get('columns_to_drop', [])  # ✅ Load this

        logger.info(f"Preprocessor loaded from {path}")
        return preprocessor


# Example usage
if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)

    # Create sample data with ID columns (simulating real scenario)
    sample_data = pd.DataFrame({
        'sensorId': ['SENSOR_001', 'SENSOR_002', 'SENSOR_003'] * 34,  # ✅ String ID column
        'timestamp': pd.date_range('2025-01-01', periods=102, freq='5min'),  # ✅ Timestamp
        'temperature': np.random.normal(25, 5, 102),
        'humidity': np.random.normal(60, 10, 102),
        'co2': np.random.normal(800, 200, 102),
        'pm25': np.random.normal(15, 5, 102),
        'aqi_category': np.random.choice(['Good', 'Moderate', 'Unhealthy'], 102),
        'target': np.random.randint(0, 2, 102)
    })

    print("Original data:")
    print(sample_data.dtypes)
    print(f"\nShape: {sample_data.shape}")

    # Initialize preprocessor
    preprocessor = DataPreprocessor()

    # Fit and transform
    X, y = preprocessor.fit_transform(sample_data, target_col='target')

    print(f"\n✅ Transformed data shape: {X.shape}")
    print(f"✅ Feature names: {preprocessor.feature_names}")
    print(f"✅ Dropped columns: {preprocessor.columns_to_drop}")
    print(f"\n✅ All columns numeric: {X.select_dtypes(include=[np.number]).shape[1] == X.shape[1]}")
    print(f"\nFirst few rows:\n{X.head()}")