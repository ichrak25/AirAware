import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import {
    Layers,
    Filter,
    MapPin,
    Activity,
    X,
    ChevronRight,
    RefreshCw,
    WifiOff,
} from 'lucide-react';
import { useSensors } from '../context/AppContext';
import { sensorsAPI, readingsAPI, mockData } from '../services/api';
import { calculateAQI, formatNumber, getSensorStatus } from '../utils/helpers';
import SensorMap from '../components/Map/SensorMap';
import AQIGauge from '../components/Gauges/AQIGauge';
import { MetricRow } from '../components/Cards/MetricCard';
import { Sparkline } from '../components/Charts/TimeSeriesChart';

/**
 * Map Page - WITH REAL API INTEGRATION
 * Full-screen interactive map with sensor details panel
 */
export default function MapPage() {
    const { sensors, setSensors, selectedSensor, setSelectedSensor } = useSensors();
    const [currentReadings, setCurrentReadings] = useState({});
    const [historicalData, setHistoricalData] = useState({});
    const [filterStatus, setFilterStatus] = useState('all');
    const [showPanel, setShowPanel] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const [isRefreshing, setIsRefreshing] = useState(false);
    const [useMockData, setUseMockData] = useState(false);
    const [error, setError] = useState(null);

    // Load data
    useEffect(() => {
        loadData();

        // Auto-refresh every 30 seconds
        const interval = setInterval(refreshData, 30000);
        return () => clearInterval(interval);
    }, []);

    const loadData = async () => {
        setIsLoading(true);
        setError(null);

        try {
            console.log('[Map] Fetching real data from API...');

            // Fetch sensors
            let sensorsData = [];
            try {
                sensorsData = await sensorsAPI.getAll();
                console.log('[Map] Loaded:', sensorsData.length, 'sensors');
            } catch (e) {
                console.warn('[Map] Failed to fetch sensors:', e.message);
            }

            // Fetch readings
            let readingsData = [];
            try {
                readingsData = await readingsAPI.getLatest(200);
                console.log('[Map] Loaded:', readingsData.length, 'readings');
            } catch (e) {
                console.warn('[Map] Failed to fetch readings:', e.message);
            }

            const hasRealData = sensorsData.length > 0;

            if (hasRealData) {
                console.log('[Map] ✅ Using REAL DATA');
                setUseMockData(false);
                setSensors(sensorsData);

                // Organize readings by sensor
                const readings = {};
                const history = {};

                readingsData.forEach(reading => {
                    const sensorId = reading.sensorId || reading.sensor_id || reading.deviceId;

                    // Latest reading per sensor
                    if (!readings[sensorId]) {
                        readings[sensorId] = reading;
                    }

                    // Build history per sensor
                    if (!history[sensorId]) {
                        history[sensorId] = [];
                    }
                    history[sensorId].push(reading);
                });

                // Keep only last 24 readings per sensor for sparklines
                Object.keys(history).forEach(id => {
                    history[id] = history[id].slice(0, 24).reverse();
                });

                setCurrentReadings(readings);
                setHistoricalData(history);

            } else {
                // Fall back to mock data
                console.log('[Map] ⚠️ No real data - using MOCK DATA');
                setUseMockData(true);
                setSensors(mockData.sensors);

                const readings = {};
                const history = {};
                mockData.sensors.forEach(sensor => {
                    readings[sensor.deviceId] = mockData.generateReading(sensor.deviceId);
                    history[sensor.deviceId] = mockData.generateReadingsHistory(sensor.deviceId, 24);
                });
                setCurrentReadings(readings);
                setHistoricalData(history);
            }
        } catch (err) {
            console.error('[Map] Error loading data:', err);
            setError(err.message);

            // Fall back to mock data on error
            setUseMockData(true);
            setSensors(mockData.sensors);

            const readings = {};
            const history = {};
            mockData.sensors.forEach(sensor => {
                readings[sensor.deviceId] = mockData.generateReading(sensor.deviceId);
                history[sensor.deviceId] = mockData.generateReadingsHistory(sensor.deviceId, 24);
            });
            setCurrentReadings(readings);
            setHistoricalData(history);
        } finally {
            setIsLoading(false);
        }
    };

    // Refresh data
    const refreshData = async () => {
        setIsRefreshing(true);

        try {
            if (!useMockData) {
                // Fetch fresh real data
                const readingsData = await readingsAPI.getLatest(100);

                const readings = {};
                readingsData.forEach(reading => {
                    const sensorId = reading.sensorId || reading.sensor_id || reading.deviceId;
                    if (!readings[sensorId]) {
                        readings[sensorId] = reading;
                    }
                });
                setCurrentReadings(readings);
            } else {
                // Update mock readings
                const readings = {};
                sensors.forEach(sensor => {
                    const sensorId = sensor.deviceId || sensor.sensorId;
                    readings[sensorId] = mockData.generateReading(sensorId);
                });
                setCurrentReadings(readings);
            }
        } catch (err) {
            console.error('[Map] Refresh failed:', err);
        } finally {
            setTimeout(() => setIsRefreshing(false), 500);
        }
    };

    // Filter sensors
    const filteredSensors = sensors.filter(s => {
        if (filterStatus === 'all') return true;
        return s.status === filterStatus;
    });

    // Handle sensor selection
    const handleSensorSelect = (sensor) => {
        setSelectedSensor(sensor);
        setShowPanel(true);
    };

    // Selected sensor data
    const selectedSensorId = selectedSensor?.deviceId || selectedSensor?.sensorId;
    const selectedReading = selectedSensor ? currentReadings[selectedSensorId] : null;
    const selectedHistory = selectedSensor ? historicalData[selectedSensorId] || [] : [];

    // Loading state
    if (isLoading) {
        return (
            <div className="h-[calc(100vh-8rem)] lg:h-[calc(100vh-6rem)] flex items-center justify-center">
                <div className="text-center">
                    <RefreshCw className="w-12 h-12 text-air-600 animate-spin mx-auto mb-4" />
                    <p className="text-slate-600 dark:text-slate-400">Loading map...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="h-[calc(100vh-8rem)] lg:h-[calc(100vh-6rem)] relative">
            {/* Data Source Indicator */}
            {useMockData && (
                <div className="absolute top-4 left-1/2 -translate-x-1/2 z-[1000] bg-yellow-50 dark:bg-yellow-900/90 border border-yellow-200 dark:border-yellow-800 rounded-xl px-4 py-2 shadow-lg">
                    <div className="flex items-center gap-2 text-sm">
                        <WifiOff className="w-4 h-4 text-yellow-600" />
                        <span className="text-yellow-800 dark:text-yellow-200 font-medium">Demo Mode</span>
                        <button onClick={loadData} className="ml-2 text-yellow-600 hover:text-yellow-700 underline">
                            Retry
                        </button>
                    </div>
                </div>
            )}

            {/* Map */}
            <SensorMap
                sensors={filteredSensors}
                readings={currentReadings}
                selectedSensor={selectedSensor}
                onSensorSelect={handleSensorSelect}
                height="100%"
                showControls={true}
            />

            {/* Filter Controls */}
            <div className="absolute top-4 right-4 z-[1000] flex gap-2">
                <button
                    onClick={refreshData}
                    disabled={isRefreshing}
                    className="glass-card p-2 rounded-xl"
                >
                    <RefreshCw className={`w-5 h-5 text-slate-600 ${isRefreshing ? 'animate-spin' : ''}`} />
                </button>

                <select
                    value={filterStatus}
                    onChange={(e) => setFilterStatus(e.target.value)}
                    className="glass-card px-4 py-2 rounded-xl text-sm font-medium border-0 focus:ring-2 focus:ring-air-500"
                >
                    <option value="all">All Sensors</option>
                    <option value="ACTIVE">Active Only</option>
                    <option value="OFFLINE">Offline Only</option>
                </select>

                <button className="glass-card p-2 rounded-xl">
                    <Layers className="w-5 h-5 text-slate-600" />
                </button>
            </div>

            {/* Sensor List (Desktop) */}
            <div className="absolute top-4 left-4 z-[1000] hidden lg:block w-80">
                <div className="glass-card p-4 max-h-[60vh] overflow-y-auto">
                    <h3 className="font-semibold text-slate-900 dark:text-white mb-3">
                        Sensors ({filteredSensors.length})
                        {!useMockData && (
                            <span className="ml-2 text-xs font-normal text-green-600 bg-green-100 dark:bg-green-900/30 px-2 py-0.5 rounded-full">
                LIVE
              </span>
                        )}
                    </h3>
                    <div className="space-y-2">
                        {filteredSensors.map(sensor => {
                            const sensorId = sensor.deviceId || sensor.sensorId;
                            const reading = currentReadings[sensorId];
                            const { aqi, color } = calculateAQI(reading?.pm25 || 0);
                            const isSelected = selectedSensor?.id === sensor.id || selectedSensorId === sensorId;

                            return (
                                <button
                                    key={sensor.id || sensorId}
                                    onClick={() => handleSensorSelect(sensor)}
                                    className={`
                    w-full flex items-center gap-3 p-3 rounded-xl text-left transition-all
                    ${isSelected
                                        ? 'bg-air-100 dark:bg-air-900/30 ring-2 ring-air-500'
                                        : 'hover:bg-slate-100 dark:hover:bg-slate-800'
                                    }
                  `}
                                >
                                    <div
                                        className="w-3 h-3 rounded-full flex-shrink-0"
                                        style={{
                                            backgroundColor: sensor.status === 'ACTIVE' ? color : '#94a3b8',
                                            boxShadow: sensor.status === 'ACTIVE' ? `0 0 8px ${color}` : 'none'
                                        }}
                                    />
                                    <div className="flex-1 min-w-0">
                                        <p className="font-medium text-sm text-slate-900 dark:text-white truncate">
                                            {sensorId}
                                        </p>
                                        <p className="text-xs text-slate-500 truncate">
                                            {sensor.location?.city || 'Unknown'}
                                        </p>
                                    </div>
                                    {sensor.status === 'ACTIVE' && (
                                        <span
                                            className="text-sm font-bold"
                                            style={{ color }}
                                        >
                      {aqi}
                    </span>
                                    )}
                                </button>
                            );
                        })}
                    </div>
                </div>
            </div>

            {/* Sensor Detail Panel */}
            <motion.div
                initial={{ x: '100%' }}
                animate={{ x: showPanel && selectedSensor ? 0 : '100%' }}
                transition={{ type: 'spring', damping: 25, stiffness: 200 }}
                className="absolute top-0 right-0 bottom-0 w-full sm:w-96 z-[1001] bg-white dark:bg-slate-900 shadow-2xl"
            >
                {selectedSensor && (
                    <div className="h-full flex flex-col">
                        {/* Header */}
                        <div className="flex items-center justify-between p-4 border-b border-slate-200 dark:border-slate-800">
                            <div>
                                <h2 className="font-display font-bold text-lg text-slate-900 dark:text-white">
                                    {selectedSensorId}
                                </h2>
                                <p className="text-sm text-slate-500">
                                    {selectedSensor.description || selectedSensor.name}
                                </p>
                            </div>
                            <button
                                onClick={() => setShowPanel(false)}
                                className="p-2 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-xl"
                            >
                                <X className="w-5 h-5 text-slate-500" />
                            </button>
                        </div>

                        {/* Content */}
                        <div className="flex-1 overflow-y-auto p-4 space-y-6">
                            {/* Status & Location */}
                            <div className="flex items-center gap-4">
                <span
                    className={`
                    px-3 py-1 rounded-full text-sm font-medium
                    ${getSensorStatus(selectedSensor.status).bgClass}
                  `}
                    style={{ color: getSensorStatus(selectedSensor.status).color }}
                >
                  {getSensorStatus(selectedSensor.status).label}
                </span>
                                <div className="flex items-center gap-1 text-sm text-slate-500">
                                    <MapPin className="w-4 h-4" />
                                    {selectedSensor.location?.city || 'Unknown'}, {selectedSensor.location?.country || 'Tunisia'}
                                </div>
                            </div>

                            {/* AQI */}
                            {selectedReading && (
                                <div className="flex justify-center">
                                    <AQIGauge
                                        value={selectedReading.pm25}
                                        size={160}
                                        showRecommendation={false}
                                    />
                                </div>
                            )}

                            {/* Metrics */}
                            {selectedReading && (
                                <div className="glass-card p-4 bg-slate-50 dark:bg-slate-800/50">
                                    <h3 className="text-sm font-semibold text-slate-700 dark:text-slate-300 mb-3">
                                        Current Readings
                                        {!useMockData && (
                                            <span className="ml-2 text-xs font-normal text-green-600">LIVE</span>
                                        )}
                                    </h3>
                                    <div className="grid grid-cols-2 gap-4">
                                        <div>
                                            <span className="text-xs text-slate-500">Temperature</span>
                                            <p className="font-semibold">{formatNumber(selectedReading.temperature)}°C</p>
                                        </div>
                                        <div>
                                            <span className="text-xs text-slate-500">Humidity</span>
                                            <p className="font-semibold">{formatNumber(selectedReading.humidity)}%</p>
                                        </div>
                                        <div>
                                            <span className="text-xs text-slate-500">CO₂</span>
                                            <p className="font-semibold">{formatNumber(selectedReading.co2, 0)} ppm</p>
                                        </div>
                                        <div>
                                            <span className="text-xs text-slate-500">VOC</span>
                                            <p className="font-semibold">{formatNumber(selectedReading.voc)} mg/m³</p>
                                        </div>
                                        <div>
                                            <span className="text-xs text-slate-500">PM2.5</span>
                                            <p className="font-semibold">{formatNumber(selectedReading.pm25)} µg/m³</p>
                                        </div>
                                        <div>
                                            <span className="text-xs text-slate-500">PM10</span>
                                            <p className="font-semibold">{formatNumber(selectedReading.pm10)} µg/m³</p>
                                        </div>
                                    </div>
                                </div>
                            )}

                            {/* Trend Chart */}
                            {selectedHistory.length > 0 && (
                                <div className="glass-card p-4 bg-slate-50 dark:bg-slate-800/50">
                                    <h3 className="text-sm font-semibold text-slate-700 dark:text-slate-300 mb-3">
                                        24h Trend
                                    </h3>
                                    <div className="space-y-3">
                                        <div className="flex items-center justify-between">
                                            <span className="text-sm text-slate-500">PM2.5</span>
                                            <Sparkline
                                                data={selectedHistory}
                                                metric="pm25"
                                                width={150}
                                                height={40}
                                            />
                                        </div>
                                        <div className="flex items-center justify-between">
                                            <span className="text-sm text-slate-500">CO₂</span>
                                            <Sparkline
                                                data={selectedHistory}
                                                metric="co2"
                                                width={150}
                                                height={40}
                                            />
                                        </div>
                                    </div>
                                </div>
                            )}

                            {/* Sensor Info */}
                            <div className="glass-card p-4 bg-slate-50 dark:bg-slate-800/50">
                                <h3 className="text-sm font-semibold text-slate-700 dark:text-slate-300 mb-3">
                                    Sensor Details
                                </h3>
                                <div className="space-y-2 text-sm">
                                    <div className="flex justify-between">
                                        <span className="text-slate-500">Model</span>
                                        <span className="font-medium">{selectedSensor.model || 'Raspberry Pi'}</span>
                                    </div>
                                    <div className="flex justify-between">
                                        <span className="text-slate-500">Latitude</span>
                                        <span className="font-mono">{selectedSensor.location?.latitude?.toFixed(4) || 'N/A'}</span>
                                    </div>
                                    <div className="flex justify-between">
                                        <span className="text-slate-500">Longitude</span>
                                        <span className="font-mono">{selectedSensor.location?.longitude?.toFixed(4) || 'N/A'}</span>
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* Actions */}
                        <div className="p-4 border-t border-slate-200 dark:border-slate-800">
                            <button className="btn btn-primary w-full">
                                <Activity className="w-4 h-4" />
                                View Full Analytics
                                <ChevronRight className="w-4 h-4" />
                            </button>
                        </div>
                    </div>
                )}
            </motion.div>
        </div>
    );
}