# iot/sensors/mock_sensors.py
import time
from sensors.bme680_reader import read_bme680
from sensors.sgp30_reader import read_sgp30
from sensors.pm_sensor_reader import read_pm_sensor
from sensors.mhz19_reader import read_mhz19

def read_all_sensors():
    """Aggregate all sensor readings into one payload"""
    data = {}
    data.update(read_bme680())
    data.update(read_sgp30())
    data.update(read_pm_sensor())
    data.update(read_mhz19())
    data["timestamp"] = time.strftime("%Y-%m-%d %H:%M:%S")
    return data

# quick test
if __name__ == "__main__":
    print(read_all_sensors())
