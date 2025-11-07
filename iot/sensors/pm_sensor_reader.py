# iot/sensors/pm_sensor_reader.py
import random

def read_pm_sensor():
    """Simulated PMS5003/SDS011: PM2.5 and PM10"""
    pm25 = round(random.uniform(5, 35), 1)
    pm10 = round(random.uniform(10, 60), 1)
    return {"pm25": pm25, "pm10": pm10}
