# ============================================================================
# setup.py
# ============================================================================
from setuptools import setup, find_packages

setup(
    name="airaware-mlops",
    version="1.0.0",
    description="MLOps module for AirAware air quality monitoring",
    author="AirAware Team",
    packages=find_packages(),
    install_requires=[
        'scikit-learn>=1.3.0',
        'xgboost>=2.0.0',
        'tensorflow>=2.15.0',
        'optuna>=3.5.0',
        'pandas>=2.1.0',
        'numpy>=1.26.0',
        'pymongo>=4.6.0',
        'fastapi>=0.108.0',
        'uvicorn>=0.25.0',
        'mlflow>=2.9.0',
        'prometheus-client>=0.19.0',
        'pytest>=7.4.0',
        'pyyaml>=6.0.0',
        'joblib>=1.3.0',
        'scipy>=1.11.0'
    ],
    python_requires='>=3.11',
    classifiers=[
        'Development Status :: 4 - Beta',
        'Intended Audience :: Developers',
        'Programming Language :: Python :: 3.11',
    ]
)