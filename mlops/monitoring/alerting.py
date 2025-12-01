# ============================================================================
# monitoring/alerting.py
# ============================================================================
"""
AirAware MLOps - Alert Manager
Generate and send alerts based on ML predictions
"""

import logging
import requests
import yaml
from datetime import datetime

logger = logging.getLogger(__name__)


class AlertManager:
    """Manage ML-based alerts"""

    def __init__(self, config_path: str = "config/config.yaml"):
        with open(config_path, 'r') as f:
            self.config = yaml.safe_load(f)

        self.alert_config = self.config['alerts']
        self.webhook_url = self.alert_config['webhook']['url']

        logger.info("Alert manager initialized")

    def check_and_alert(self, prediction_result: dict):
        """Check prediction and send alerts if needed"""
        # Anomaly alert
        if prediction_result.get('is_anomaly'):
            self.send_alert(
                alert_type="ANOMALY_DETECTED",
                severity="WARNING",
                message=f"Anomaly detected with score {prediction_result['anomaly_score']:.2f}",
                data=prediction_result
            )

        # AQI alert
        predicted_aqi = prediction_result.get('predicted_aqi', 0)
        if predicted_aqi > self.alert_config['aqi_thresholds']['warning']:
            severity = self._get_aqi_severity(predicted_aqi)
            self.send_alert(
                alert_type="AQI_HIGH",
                severity=severity,
                message=f"High AQI predicted: {predicted_aqi:.1f}",
                data=prediction_result
            )

    def send_alert(self, alert_type: str, severity: str, message: str, data: dict):
        """Send alert to webhook"""
        alert_payload = {
            "type": alert_type,
            "severity": severity,
            "message": message,
            "sensorId": data.get('sensorId', 'UNKNOWN'),
            "triggeredAt": datetime.now().isoformat(),
            "resolved": False,
            "reading": {
                "co2": data.get('co2', 0),
                "pm25": data.get('pm25', 0),
                "voc": data.get('voc', 0),
                "timestamp": data.get('timestamp', datetime.now().isoformat())
            }
        }

        try:
            response = requests.post(
                self.webhook_url,
                json=alert_payload,
                headers={'Content-Type': 'application/json'},
                timeout=5
            )

            if response.status_code == 201:
                logger.info(f"Alert sent successfully: {alert_type}")
            else:
                logger.error(f"Failed to send alert: {response.status_code}")

        except Exception as e:
            logger.error(f"Error sending alert: {e}")

    def _get_aqi_severity(self, aqi: float) -> str:
        """Determine severity based on AQI"""
        thresholds = self.alert_config['aqi_thresholds']

        if aqi >= thresholds['emergency']:
            return "CRITICAL"
        elif aqi >= thresholds['danger']:
            return "DANGER"
        elif aqi >= thresholds['warning']:
            return "WARNING"
        else:
            return "INFO"
