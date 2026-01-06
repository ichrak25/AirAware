import React, { useEffect, useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
    Activity,
    TrendingUp,
    AlertTriangle,
    Map,
    RefreshCw,
    ChevronRight,
    Zap,
    WifiOff,
    Bug,
    X,
} from 'lucide-react';
import { useApp, useSensors, useAlerts } from '../context/AppContext';
import { sensorsAPI, readingsAPI, alertsAPI, mockData } from '../services/api';
import { calculateAQI, formatRelativeTime } from '../utils/helpers';
import AQIGauge, { AQIBadge } from '../components/Gauges/AQIGauge';
import MetricCard, { MetricGrid } from '../components/Cards/MetricCard';
import SensorMap from '../components/Map/SensorMap';
import { TimeSeriesChart, ChartCard } from '../components/Charts/TimeSeriesChart';
import AlertList, { AlertSummary } from '../components/Alerts/AlertList';

/**
 * Dashboard Page
 * Main overview with real-time air quality data from Raspberry Pi sensors
 *
 * Data Flow: Raspberry Pi -> MQTT -> WildFly API -> MongoDB -> This Dashboard
 */
export default function Dashboard() {
    const { state, actions } = useApp();
    const { sensors, setSensors, setSelectedSensor } = useSensors();
    const { alerts, setAlerts, resolveAlert } = useAlerts();

    const [currentReadings, setCurrentReadings] = useState({});
    const [historicalData, setHistoricalData] = useState([]);
    const [isRefreshing, setIsRefreshing] = useState(false);
    const [lastUpdate, setLastUpdate] = useState(new Date());
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);
    const [useMockData, setUseMockData] = useState(false);
    const [showDebug, setShowDebug] = useState(false);
    const [rawApiData, setRawApiData] = useState({ sensors: [], readings: [], alerts: [] });

    // Load initial data from API
    useEffect(() => {
        loadData();

        // Set up auto-refresh every 30 seconds
        const interval = setInterval(refreshData, 30000);
        return () => clearInterval(interval);
    }, []);

    const loadData = async () => {
        setIsLoading(true);
        setError(null);

        try {
            // Try to fetch real data from API
            console.log('Fetching real sensor data from API...');

            // Fetch sensors
            let sensorsData = [];
            try {
                sensorsData = await sensorsAPI.getAll();
                console.log('Sensors loaded:', sensorsData);
            } catch (e) {
                console.warn('Failed to fetch sensors from API:', e.message);
            }

            // Fetch alerts
            let alertsData = [];
            try {
                alertsData = await alertsAPI.getAll();
                console.log('Alerts loaded:', alertsData);
            } catch (e) {
                console.warn('Failed to fetch alerts from API:', e.message);
            }

            // Fetch readings
            let readingsData = [];
            try {
                readingsData = await readingsAPI.getLatest(50);
                console.log('Readings loaded:', readingsData);
            } catch (e) {
                console.warn('Failed to fetch readings from API:', e.message);
            }

            // Check if we got real data
            const hasRealData = sensorsData.length > 0 || readingsData.length > 0;

            if (hasRealData) {
                console.log('✅ Using REAL DATA from Raspberry Pi sensors!');
                setUseMockData(false);

                // Store raw data for debug panel
                setRawApiData({ sensors: sensorsData, readings: readingsData, alerts: alertsData });

                // Set sensors
                setSensors(sensorsData);
                setAlerts(alertsData);

                // Organize readings by sensor
                const readingsBySensor = {};
                readingsData.forEach(reading => {
                    const sensorId = reading.sensorId || reading.sensor_id;
                    if (!readingsBySensor[sensorId]) {
                        readingsBySensor[sensorId] = reading;
                    }
                });
                setCurrentReadings(readingsBySensor);

                // Set historical data (last 24 readings for charts)
                setHistoricalData(readingsData.slice(0, 24).reverse());

            } else {
                // Fall back to mock data if no real data available
                console.log('⚠️ No real data available - using MOCK DATA for demo');
                setUseMockData(true);

                setSensors(mockData.sensors);
                setAlerts(mockData.alerts);

                const readings = {};
                mockData.sensors.forEach(sensor => {
                    readings[sensor.deviceId] = mockData.generateReading(sensor.deviceId);
                });
                setCurrentReadings(readings);

                const history = mockData.generateReadingsHistory('SENSOR_TUNIS_001', 24);
                setHistoricalData(history);
            }

            setLastUpdate(new Date());

        } catch (error) {
            console.error('Failed to load data:', error);
            setError(error.message);

            // Fall back to mock data on error
            setUseMockData(true);
            setSensors(mockData.sensors);
            setAlerts(mockData.alerts);

            const readings = {};
            mockData.sensors.forEach(sensor => {
                readings[sensor.deviceId] = mockData.generateReading(sensor.deviceId);
            });
            setCurrentReadings(readings);
            setHistoricalData(mockData.generateReadingsHistory('SENSOR_TUNIS_001', 24));

        } finally {
            setIsLoading(false);
        }
    };

    const refreshData = async () => {
        setIsRefreshing(true);

        try {
            if (!useMockData) {
                // Fetch fresh real data
                const readingsData = await readingsAPI.getLatest(50);

                const readingsBySensor = {};
                readingsData.forEach(reading => {
                    const sensorId = reading.sensorId || reading.sensor_id;
                    if (!readingsBySensor[sensorId]) {
                        readingsBySensor[sensorId] = reading;
                    }
                });
                setCurrentReadings(readingsBySensor);

                // Update historical data
                if (readingsData.length > 0) {
                    setHistoricalData(readingsData.slice(0, 24).reverse());
                }

                // Refresh alerts
                const alertsData = await alertsAPI.getAll();
                setAlerts(alertsData);

            } else {
                // Update mock readings
                const readings = {};
                sensors.forEach(sensor => {
                    const sensorId = sensor.deviceId || sensor.sensorId;
                    readings[sensorId] = mockData.generateReading(sensorId);
                });
                setCurrentReadings(readings);

                const newReading = mockData.generateReading('SENSOR_TUNIS_001');
                setHistoricalData(prev => [...prev.slice(-23), newReading]);
            }

            setLastUpdate(new Date());

        } catch (error) {
            console.error('Failed to refresh data:', error);
        } finally {
            setTimeout(() => setIsRefreshing(false), 500);
        }
    };

    // Calculate overall AQI (average of all sensors)
    const overallAQI = useMemo(() => {
        const pm25Values = Object.values(currentReadings)
            .map(r => r?.pm25)
            .filter(v => v !== undefined && v !== null);

        if (!pm25Values.length) return 0;
        return pm25Values.reduce((a, b) => a + b, 0) / pm25Values.length;
    }, [currentReadings]);

    // Get primary sensor reading (first active sensor)
    const primaryReading = useMemo(() => {
        const sensorIds = Object.keys(currentReadings);
        if (sensorIds.length === 0) return null;

        // Try to find SENSOR-TN-001 first, otherwise use first available
        const primaryId = sensorIds.find(id => id.includes('TN-001')) || sensorIds[0];
        return currentReadings[primaryId];
    }, [currentReadings]);

    // Get primary sensor name
    const primarySensorName = useMemo(() => {
        if (!sensors.length) return 'No Sensor';
        const primary = sensors.find(s =>
            s.sensorId?.includes('TN-001') ||
            s.deviceId?.includes('TN-001') ||
            s.deviceId?.includes('TUNIS_001')
        );
        return primary?.name || primary?.description || sensors[0]?.name || 'Primary Sensor';
    }, [sensors]);

    // Animation variants
    const containerVariants = {
        hidden: { opacity: 0 },
        visible: {
            opacity: 1,
            transition: {
                staggerChildren: 0.1,
            },
        },
    };

    const itemVariants = {
        hidden: { opacity: 0, y: 20 },
        visible: { opacity: 1, y: 0 },
    };

    // Loading state
    if (isLoading) {
        return (
            <div className="flex items-center justify-center min-h-[60vh]">
                <div className="text-center">
                    <RefreshCw className="w-12 h-12 text-air-600 animate-spin mx-auto mb-4" />
                    <p className="text-slate-600 dark:text-slate-400">Loading sensor data...</p>
                </div>
            </div>
        );
    }

    return (
        <motion.div
            variants={containerVariants}
            initial="hidden"
            animate="visible"
            className="space-y-6"
        >
            {/* Data Source Indicator */}
            {useMockData && (
                <motion.div
                    variants={itemVariants}
                    className="bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-xl p-4"
                >
                    <div className="flex items-center gap-3">
                        <WifiOff className="w-5 h-5 text-yellow-600" />
                        <div>
                            <p className="font-medium text-yellow-800 dark:text-yellow-200">
                                Demo Mode - Using Mock Data
                            </p>
                            <p className="text-sm text-yellow-600 dark:text-yellow-400">
                                No data from Raspberry Pi sensors. Make sure sensors are sending data via MQTT.
                            </p>
                        </div>
                        <button
                            onClick={loadData}
                            className="ml-auto btn btn-secondary text-sm"
                        >
                            Retry Connection
                        </button>
                    </div>
                </motion.div>
            )}

            {/* Header */}
            <motion.div variants={itemVariants} className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-display font-bold text-slate-900 dark:text-white">
                        Dashboard
                    </h1>
                    <p className="text-slate-500 dark:text-slate-400">
                        {useMockData
                            ? 'Demo data - Connect Raspberry Pi for real readings'
                            : 'Real-time air quality monitoring across Tunisia'}
                    </p>
                </div>

                <div className="flex items-center gap-3">
                    <button
                        onClick={() => setShowDebug(!showDebug)}
                        className={`btn ${showDebug ? 'btn-primary' : 'btn-ghost'} text-sm`}
                        title="Toggle Debug Panel"
                    >
                        <Bug className="w-4 h-4" />
                    </button>
                    <span className="text-sm text-slate-500">
            Updated {formatRelativeTime(lastUpdate)}
          </span>
                    <button
                        onClick={refreshData}
                        disabled={isRefreshing}
                        className="btn btn-secondary"
                    >
                        <RefreshCw className={`w-4 h-4 ${isRefreshing ? 'animate-spin' : ''}`} />
                        Refresh
                    </button>
                </div>
            </motion.div>

            {/* Debug Panel - Shows raw API data */}
            {showDebug && (
                <motion.div
                    variants={itemVariants}
                    className="glass-card p-4 bg-slate-900 text-green-400 font-mono text-xs overflow-auto max-h-96"
                >
                    <div className="flex items-center justify-between mb-3">
                        <h3 className="text-white font-bold text-sm">🔍 Debug Panel - Raw API Data</h3>
                        <button onClick={() => setShowDebug(false)} className="text-slate-400 hover:text-white">
                            <X className="w-4 h-4" />
                        </button>
                    </div>

                    <div className="space-y-4">
                        <div>
                            <p className="text-yellow-400 mb-1">📡 Data Source: {useMockData ? 'MOCK DATA' : 'REAL RASPBERRY PI DATA'}</p>
                        </div>

                        <div>
                            <p className="text-cyan-400 mb-1">📊 Latest Reading (from API):</p>
                            {rawApiData.readings.length > 0 ? (
                                <pre className="bg-slate-800 p-2 rounded overflow-x-auto">
                  {JSON.stringify(rawApiData.readings[0], null, 2)}
                </pre>
                            ) : (
                                <p className="text-red-400">No readings available</p>
                            )}
                        </div>

                        <div>
                            <p className="text-cyan-400 mb-1">🎯 Current Readings State (displayed on dashboard):</p>
                            <pre className="bg-slate-800 p-2 rounded overflow-x-auto">
                {JSON.stringify(currentReadings, null, 2)}
              </pre>
                        </div>

                        <div>
                            <p className="text-cyan-400 mb-1">📍 Sensors ({rawApiData.sensors.length} total):</p>
                            <pre className="bg-slate-800 p-2 rounded overflow-x-auto max-h-32">
                {JSON.stringify(rawApiData.sensors.map(s => ({ id: s.sensorId || s.deviceId, name: s.name, status: s.status })), null, 2)}
              </pre>
                        </div>

                        <div>
                            <p className="text-cyan-400 mb-1">⚠️ Alerts ({rawApiData.alerts.length} total):</p>
                            <pre className="bg-slate-800 p-2 rounded overflow-x-auto max-h-32">
                {JSON.stringify(rawApiData.alerts, null, 2)}
              </pre>
                        </div>

                        <div className="pt-2 border-t border-slate-700">
                            <p className="text-slate-400">
                                💡 Compare these values with: <br/>
                                1. MQTT: <code>mosquitto_sub -h localhost -t "airaware/#" -v</code><br/>
                                2. MongoDB: <code>mongosh AirAwareDB --eval "db.readings.find().sort({'{'}'timestamp':-1{'}'}).limit(1).pretty()"</code>
                            </p>
                        </div>
                    </div>
                </motion.div>
            )}

            {/* Quick Stats */}
            <motion.div variants={itemVariants} className="grid grid-cols-2 lg:grid-cols-4 gap-4">
                <div className="glass-card p-5">
                    <div className="flex items-center gap-3">
                        <div className="w-12 h-12 rounded-xl bg-air-100 dark:bg-air-900/30 flex items-center justify-center">
                            <Activity className="w-6 h-6 text-air-600" />
                        </div>
                        <div>
                            <p className="text-sm text-slate-500 dark:text-slate-400">Active Sensors</p>
                            <p className="text-2xl font-display font-bold text-slate-900 dark:text-white">
                                {sensors.filter(s => s.status === 'ACTIVE').length}
                            </p>
                        </div>
                    </div>
                </div>

                <div className="glass-card p-5">
                    <div className="flex items-center gap-3">
                        <div className="w-12 h-12 rounded-xl bg-green-100 dark:bg-green-900/30 flex items-center justify-center">
                            <TrendingUp className="w-6 h-6 text-green-600" />
                        </div>
                        <div>
                            <p className="text-sm text-slate-500 dark:text-slate-400">Avg AQI</p>
                            <p className="text-2xl font-display font-bold text-slate-900 dark:text-white">
                                {calculateAQI(overallAQI).aqi}
                            </p>
                        </div>
                    </div>
                </div>

                <div className="glass-card p-5">
                    <div className="flex items-center gap-3">
                        <div className="w-12 h-12 rounded-xl bg-yellow-100 dark:bg-yellow-900/30 flex items-center justify-center">
                            <AlertTriangle className="w-6 h-6 text-yellow-600" />
                        </div>
                        <div>
                            <p className="text-sm text-slate-500 dark:text-slate-400">Active Alerts</p>
                            <p className="text-2xl font-display font-bold text-slate-900 dark:text-white">
                                {alerts.filter(a => !a.resolved).length}
                            </p>
                        </div>
                    </div>
                </div>

                <div className="glass-card p-5">
                    <div className="flex items-center gap-3">
                        <div className="w-12 h-12 rounded-xl bg-violet-100 dark:bg-violet-900/30 flex items-center justify-center">
                            <Zap className="w-6 h-6 text-violet-600" />
                        </div>
                        <div>
                            <p className="text-sm text-slate-500 dark:text-slate-400">Readings Today</p>
                            <p className="text-2xl font-display font-bold text-slate-900 dark:text-white">
                                {rawApiData.readings.filter(r => {
                                    if (!r.timestamp) return false;
                                    const today = new Date().setHours(0,0,0,0);
                                    const readingDate = new Date(typeof r.timestamp === 'number'
                                        ? (r.timestamp > 1e12 ? r.timestamp : r.timestamp * 1000)
                                        : r.timestamp);
                                    return readingDate >= today;
                                }).length}
                            </p>
                        </div>
                    </div>
                </div>
            </motion.div>

            {/* Main Content Grid */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* AQI Gauge + Primary Reading */}
                <motion.div variants={itemVariants} className="lg:col-span-1">
                    <div className="glass-card p-6">
                        <h2 className="font-semibold text-slate-900 dark:text-white mb-6">
                            Current Air Quality
                        </h2>
                        <AQIGauge
                            value={primaryReading?.pm25 || 0}
                            size={180}
                            showRecommendation={true}
                        />

                        <div className="mt-6 pt-6 border-t border-slate-200 dark:border-slate-700">
                            <div className="flex items-center justify-between text-sm">
                                <span className="text-slate-500">Primary Sensor</span>
                                <span className="font-medium text-slate-900 dark:text-white">
                  {primarySensorName}
                </span>
                            </div>
                            {!useMockData && primaryReading?.timestamp && (
                                <div className="flex items-center justify-between text-sm mt-2">
                                    <span className="text-slate-500">Last Reading</span>
                                    <span className="text-slate-600 dark:text-slate-400">
                    {formatRelativeTime(primaryReading.timestamp)}
                  </span>
                                </div>
                            )}
                        </div>
                    </div>
                </motion.div>

                {/* Metric Cards */}
                <motion.div variants={itemVariants} className="lg:col-span-2">
                    <div className="glass-card p-6">
                        <h2 className="font-semibold text-slate-900 dark:text-white mb-4">
                            Sensor Readings
                            {!useMockData && (
                                <span className="ml-2 text-xs font-normal text-green-600 bg-green-100 dark:bg-green-900/30 px-2 py-1 rounded-full">
                  LIVE
                </span>
                            )}
                        </h2>
                        <MetricGrid
                            reading={primaryReading}
                            previousReading={historicalData[historicalData.length - 2]}
                            size="sm"
                        />
                    </div>
                </motion.div>
            </div>

            {/* Map & Chart Row */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Map */}
                <motion.div variants={itemVariants}>
                    <div className="glass-card p-4">
                        <div className="flex items-center justify-between mb-4 px-2">
                            <h2 className="font-semibold text-slate-900 dark:text-white">
                                Sensor Network
                            </h2>
                            <Link to="/map" className="btn btn-ghost text-sm py-1.5">
                                Full Map
                                <ChevronRight className="w-4 h-4" />
                            </Link>
                        </div>
                        <SensorMap
                            sensors={sensors}
                            readings={currentReadings}
                            onSensorSelect={setSelectedSensor}
                            height="350px"
                        />
                    </div>
                </motion.div>

                {/* Chart */}
                <motion.div variants={itemVariants}>
                    <ChartCard
                        title="Air Quality Trend"
                        subtitle={useMockData
                            ? "Demo data - Last 24 hours"
                            : "PM2.5 levels from Raspberry Pi sensors"}
                        data={historicalData}
                        metric="pm25"
                        height={280}
                    />
                </motion.div>
            </div>

            {/* Alerts & Sensors Row */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Recent Alerts */}
                <motion.div variants={itemVariants}>
                    <div className="glass-card p-6">
                        <div className="flex items-center justify-between mb-4">
                            <h2 className="font-semibold text-slate-900 dark:text-white">
                                Recent Alerts
                            </h2>
                            <Link to="/alerts" className="btn btn-ghost text-sm py-1.5">
                                View All
                                <ChevronRight className="w-4 h-4" />
                            </Link>
                        </div>
                        <AlertList
                            alerts={alerts}
                            onResolve={resolveAlert}
                            maxItems={3}
                            compact={true}
                            showResolved={false}
                            emptyMessage="No active alerts"
                        />
                    </div>
                </motion.div>

                {/* Sensor Status */}
                <motion.div variants={itemVariants}>
                    <div className="glass-card p-6">
                        <div className="flex items-center justify-between mb-4">
                            <h2 className="font-semibold text-slate-900 dark:text-white">
                                Sensor Status
                            </h2>
                            <Link to="/sensors" className="btn btn-ghost text-sm py-1.5">
                                Manage
                                <ChevronRight className="w-4 h-4" />
                            </Link>
                        </div>
                        <div className="space-y-3">
                            {sensors.slice(0, 4).map(sensor => {
                                const sensorId = sensor.deviceId || sensor.sensorId;
                                const reading = currentReadings[sensorId];
                                const { aqi, color } = calculateAQI(reading?.pm25 || 0);
                                const isActive = sensor.status === 'ACTIVE';

                                return (
                                    <div
                                        key={sensor.id || sensorId}
                                        className="flex items-center gap-4 p-3 rounded-xl bg-slate-50 dark:bg-slate-800/50 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
                                        onClick={() => setSelectedSensor(sensor)}
                                    >
                                        <div
                                            className="w-3 h-3 rounded-full"
                                            style={{
                                                backgroundColor: isActive ? color : '#94a3b8',
                                                boxShadow: isActive ? `0 0 8px ${color}` : 'none'
                                            }}
                                        />
                                        <div className="flex-1 min-w-0">
                                            <p className="font-medium text-slate-900 dark:text-white truncate">
                                                {sensor.name || sensor.description}
                                            </p>
                                            <p className="text-sm text-slate-500">
                                                {sensor.location?.city || 'Unknown'}
                                            </p>
                                        </div>
                                        {isActive ? (
                                            <AQIBadge value={reading?.pm25 || 0} size="sm" />
                                        ) : (
                                            <span className="text-xs px-2 py-1 rounded-full bg-slate-200 dark:bg-slate-700 text-slate-500">
                        Offline
                      </span>
                                        )}
                                    </div>
                                );
                            })}

                            {sensors.length === 0 && (
                                <div className="text-center py-8 text-slate-500">
                                    <Activity className="w-12 h-12 mx-auto mb-3 opacity-50" />
                                    <p>No sensors registered</p>
                                    <p className="text-sm mt-1">Register sensors via the API</p>
                                </div>
                            )}
                        </div>
                    </div>
                </motion.div>
            </div>
        </motion.div>
    );
}