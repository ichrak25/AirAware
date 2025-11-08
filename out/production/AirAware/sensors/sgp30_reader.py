# iot/sensors/sgp30_reader.py
import random

def read_sgp30():
    """Simulated SGP30: CO2 and TVOC"""
    co2 = round(random.uniform(400, 1000), 1)
    tvoc = round(random.uniform(0.2, 0.8), 2)
    return {"co2": co2, "tvoc": tvoc}
