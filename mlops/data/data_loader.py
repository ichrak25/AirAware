"""
AirAware MLOps - Data Loader
Loads sensor readings from MongoDB for ML training and inference
"""

import logging
from datetime import datetime, timedelta
from typing import List, Dict, Optional, Tuple
import pandas as pd
from pymongo import MongoClient
from pymongo.collection import Collection
import yaml

logger = logging.getLogger(__name__)


class AirAwareDataLoader:
    """Load and prepare data from AirAware MongoDB database"""

    def __init__(self, config_path: str = "config/config.yaml"):
        """
        Initialize data loader with configuration

        Args:
            config_path: Path to configuration file
        """
        with open(config_path, 'r') as f:
            self.config = yaml.safe_load(f)

        # MongoDB connection
        db_config = self.config['database']['mongodb']
        self.client = MongoClient(db_config['uri'])
        self.db = self.client[db_config['database']]

        # Collections
        self.readings_collection = self.db[db_config['collections']['readings']]
        self.sensors_collection = self.db[db_config['collections']['sensors']]
        self.alerts_collection = self.db[db_config['collections']['alerts']]

        logger.info("Data loader initialized successfully")

    def load_readings(
            self,
            start_date: Optional[datetime] = None,
            end_date: Optional[datetime] = None,
            sensor_ids: Optional[List[str]] = None,
            limit: Optional[int] = None
    ) -> pd.DataFrame:
        """
        Load sensor readings from MongoDB

        Args:
            start_date: Start timestamp for filtering
            end_date: End timestamp for filtering
            sensor_ids: List of sensor IDs to filter
            limit: Maximum number of readings to return

        Returns:
            DataFrame with sensor readings
        """
        # Build query
        query = {}

        if start_date or end_date:
            query['timestamp'] = {}
            if start_date:
                query['timestamp']['$gte'] = start_date.isoformat()
            if end_date:
                query['timestamp']['$lte'] = end_date.isoformat()

        if sensor_ids:
            query['sensorId'] = {'$in': sensor_ids}

        # Execute query
        cursor = self.readings_collection.find(query)

        if limit:
            cursor = cursor.limit(limit)

        # Convert to DataFrame
        readings = list(cursor)

        if not readings:
            logger.warning("No readings found for given criteria")
            return pd.DataFrame()

        df = pd.DataFrame(readings)

        # Data type conversions
        df['timestamp'] = pd.to_datetime(df['timestamp'])

        # Numeric columns
        numeric_cols = ['temperature', 'humidity', 'co2', 'voc', 'pm25', 'pm10']
        for col in numeric_cols:
            if col in df.columns:
                df[col] = pd.to_numeric(df[col], errors='coerce')

        # Remove MongoDB _id field
        if '_id' in df.columns:
            df = df.drop('_id', axis=1)

        logger.info(f"Loaded {len(df)} readings from MongoDB")
        return df

    def load_recent_readings(
            self,
            hours: int = 24,
            sensor_ids: Optional[List[str]] = None
    ) -> pd.DataFrame:
        """
        Load readings from the last N hours

        Args:
            hours: Number of hours to look back
            sensor_ids: Optional list of sensor IDs

        Returns:
            DataFrame with recent readings
        """
        end_date = datetime.now()
        start_date = end_date - timedelta(hours=hours)

        return self.load_readings(
            start_date=start_date,
            end_date=end_date,
            sensor_ids=sensor_ids
        )

    def load_training_data(
            self,
            days: int = 30,
            validation_split: float = 0.1
    ) -> Tuple[pd.DataFrame, pd.DataFrame]:
        """
        Load and split data for model training

        Args:
            days: Number of days of historical data
            validation_split: Fraction of data for validation

        Returns:
            Tuple of (training_data, validation_data)
        """
        # Load historical data
        end_date = datetime.now()
        start_date = end_date - timedelta(days=days)

        df = self.load_readings(start_date=start_date, end_date=end_date)

        if df.empty:
            raise ValueError("No training data available")

        # Sort by timestamp
        df = df.sort_values('timestamp').reset_index(drop=True)

        # Split data
        split_idx = int(len(df) * (1 - validation_split))
        train_df = df.iloc[:split_idx]
        val_df = df.iloc[split_idx:]

        logger.info(f"Training data: {len(train_df)} samples")
        logger.info(f"Validation data: {len(val_df)} samples")

        return train_df, val_df

    def load_labeled_alerts(
            self,
            start_date: Optional[datetime] = None,
            end_date: Optional[datetime] = None
    ) -> pd.DataFrame:
        """
        Load historical alerts for supervised learning

        Args:
            start_date: Start timestamp
            end_date: End timestamp

        Returns:
            DataFrame with alert labels
        """
        query = {}

        if start_date or end_date:
            query['triggeredAt'] = {}
            if start_date:
                query['triggeredAt']['$gte'] = start_date.isoformat()
            if end_date:
                query['triggeredAt']['$lte'] = end_date.isoformat()

        alerts = list(self.alerts_collection.find(query))

        if not alerts:
            logger.warning("No alerts found")
            return pd.DataFrame()

        df = pd.DataFrame(alerts)
        df['triggeredAt'] = pd.to_datetime(df['triggeredAt'])

        if '_id' in df.columns:
            df = df.drop('_id', axis=1)

        logger.info(f"Loaded {len(df)} alerts")
        return df

    def load_sensor_metadata(
            self,
            sensor_ids: Optional[List[str]] = None
    ) -> pd.DataFrame:
        """
        Load sensor metadata (location, model, etc.)

        Args:
            sensor_ids: Optional list of sensor IDs

        Returns:
            DataFrame with sensor information
        """
        query = {}
        if sensor_ids:
            query['deviceId'] = {'$in': sensor_ids}

        sensors = list(self.sensors_collection.find(query))

        if not sensors:
            logger.warning("No sensors found")
            return pd.DataFrame()

        df = pd.DataFrame(sensors)

        if '_id' in df.columns:
            df = df.drop('_id', axis=1)

        logger.info(f"Loaded {len(df)} sensor metadata records")
        return df

    def get_statistics(
            self,
            sensor_id: Optional[str] = None
    ) -> Dict:
        """
        Get statistical summary of sensor data

        Args:
            sensor_id: Optional sensor ID for filtering

        Returns:
            Dictionary with statistics
        """
        query = {}
        if sensor_id:
            query['sensorId'] = sensor_id

        # Aggregation pipeline
        pipeline = [
            {'$match': query},
            {'$group': {
                '_id': None,
                'total_readings': {'$sum': 1},
                'avg_temperature': {'$avg': '$temperature'},
                'avg_humidity': {'$avg': '$humidity'},
                'avg_co2': {'$avg': '$co2'},
                'avg_pm25': {'$avg': '$pm25'},
                'max_co2': {'$max': '$co2'},
                'max_pm25': {'$max': '$pm25'},
                'min_temperature': {'$min': '$temperature'},
                'max_temperature': {'$max': '$temperature'}
            }}
        ]

        result = list(self.readings_collection.aggregate(pipeline))

        if not result:
            return {}

        stats = result[0]
        stats.pop('_id', None)

        logger.info(f"Statistics computed: {stats['total_readings']} readings")
        return stats

    def get_active_sensors(self) -> List[str]:
        """
        Get list of active sensor IDs

        Returns:
            List of active sensor device IDs
        """
        sensors = list(self.sensors_collection.find(
            {'status': 'ACTIVE'},
            {'deviceId': 1}
        ))

        sensor_ids = [s['deviceId'] for s in sensors]
        logger.info(f"Found {len(sensor_ids)} active sensors")

        return sensor_ids

    def close(self):
        """Close MongoDB connection"""
        self.client.close()
        logger.info("MongoDB connection closed")


# Example usage
if __name__ == "__main__":
    # Configure logging
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )

    # Initialize data loader
    loader = AirAwareDataLoader()

    try:
        # Load recent readings
        df = loader.load_recent_readings(hours=24)
        print(f"\nRecent Readings Shape: {df.shape}")
        print(f"\nColumns: {df.columns.tolist()}")
        print(f"\nFirst few rows:\n{df.head()}")

        # Get statistics
        stats = loader.get_statistics()
        print(f"\nStatistics: {stats}")

        # Get active sensors
        sensors = loader.get_active_sensors()
        print(f"\nActive Sensors: {sensors}")

    finally:
        loader.close()