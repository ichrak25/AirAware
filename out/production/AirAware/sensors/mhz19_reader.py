# iot/sensors/mhz19_reader.py
import random

def read_mhz19():
    """Simulated MH-Z19: CO2 concentration"""
    co2 = round(random.uniform(400, 1200), 1)
    return {"co2_mhz19": co2}
