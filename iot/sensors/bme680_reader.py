# iot/sensors/bme680_reader.py
import random

def read_bme680():
    """Simulated BME680: temperature, humidity, VOC"""
    temperature = round(random.uniform(20, 30), 2)
    humidity = round(random.uniform(35, 65), 2)
    voc = round(random.uniform(0.3, 1.0), 2)
    return {"temperature": temperature, "humidity": humidity, "voc": voc}
