import React, { useState, useEffect, useMemo } from 'react';
import { motion } from 'framer-motion';
import {
    TrendingUp,
    TrendingDown,
    Calendar,
    Download,
    BarChart3,
    Activity,
    RefreshCw,
    WifiOff,
    Brain,
    AlertCircle,
} from 'lucide-react';
import { useSensors } from '../context/AppContext';
import { sensorsAPI, readingsAPI, mlAPI, mockData } from '../services/api';
import { calculateAQI, formatNumber, getHealthRecommendation } from '../utils/helpers';
import { TimeSeriesChart, MultiMetricChart, ChartCard } from '../components/Charts/TimeSeriesChart';

/**
 * Analytics Page - FIXED VERSION
 * - Sensor selection now filters data
 * - Time range now filters by date
 * - ML prediction uses selected sensor's data
 */
export default function AnalyticsPage() {
    const { sensors, setSensors } = useSensors();
    const [allReadings, setAllReadings] = useState([]); // Store ALL readings
    const [selectedSensorId, setSelectedSensorId] = useState(''); // Store sensor ID string
    const [timeRange, setTimeRange] = useState('24h');
    const [selectedMetric, setSelectedMetric] = useState('pm25');
    const [isLoading, setIsLoading] = useState(true);
    const [isRefreshing, setIsRefreshing] = useState(false);
    const [useMockData, setUseMockData] = useState(false);
    const [error, setError] = useState(null);

    // MLOps predictions state
    const [mlPredictions, setMlPredictions] = useState(null);
    const [mlHealth, setMlHealth] = useState(null);
    const [mlError, setMlError] = useState(null);

    // Load data on mount
    useEffect(() => {
        loadData();
    }, []);

    // ✅ FIX: Filter data when sensor or time range changes
    const filteredData = useMemo(() => {
        let data = [...allReadings];

        // Filter by selected sensor
        if (selectedSensorId) {
            data = data.filter(r =>
                (r.sensorId || r.sensor_id) === selectedSensorId
            );
        }

        // Filter by time range
        const now = new Date();
        let cutoffDate = new Date();

        switch (timeRange) {
            case '24h':
                cutoffDate.setHours(now.getHours() - 24);
                break;
            case '7d':
                cutoffDate.setDate(now.getDate() - 7);
                break;
            case '30d':
                cutoffDate.setDate(now.getDate() - 30);
                break;
            default:
                cutoffDate.setHours(now.getHours() - 24);
        }

        data = data.filter(r => {
            if (!r.timestamp) return true;
            const readingDate = new Date(
                typeof r.timestamp === 'number'
                    ? (r.timestamp < 1e12 ? r.timestamp * 1000 : r.timestamp)
                    : r.timestamp
            );
            return readingDate >= cutoffDate;
        });

        // Sort by timestamp (oldest first for charts)
        data.sort((a, b) => {
            const dateA = new Date(a.timestamp);
            const dateB = new Date(b.timestamp);
            return dateA - dateB;
        });

        return data;
    }, [allReadings, selectedSensorId, timeRange]);

    // ✅ FIX: Update ML predictions when sensor changes
    useEffect(() => {
        if (filteredData.length > 0 && !useMockData) {
            // Get the most recent reading for selected sensor
            const latestReading = filteredData[filteredData.length - 1];
            fetchMLPredictions(latestReading);
        }
    }, [selectedSensorId, filteredData.length]);

    const loadData = async () => {
        setIsLoading(true);
        setError(null);

        try {
            console.log('[Analytics] Fetching data...');

            // Fetch sensors
            let sensorsData = [];
            try {
                sensorsData = await sensorsAPI.getAll();
                console.log('[Analytics] Loaded:', sensorsData.length, 'sensors');
            } catch (e) {
                console.warn('[Analytics] Failed to fetch sensors:', e.message);
            }

            // Fetch ALL readings (we'll filter client-side)
            let readingsData = [];
            try {
                readingsData = await readingsAPI.getLatest(500); // Get more readings
                console.log('[Analytics] Loaded:', readingsData.length, 'readings');
            } catch (e) {
                console.warn('[Analytics] Failed to fetch readings:', e.message);
            }

            const hasRealData = sensorsData.length > 0 && readingsData.length > 0;

            if (hasRealData) {
                console.log('[Analytics] ✅ Using REAL DATA');
                setUseMockData(false);
                setSensors(sensorsData);
                setAllReadings(readingsData);

                // Set default selected sensor
                if (!selectedSensorId && sensorsData.length > 0) {
                    const firstSensorId = sensorsData[0].deviceId || sensorsData[0].sensorId;
                    setSelectedSensorId(firstSensorId);
                }

            } else {
                // Fall back to mock data
                console.log('[Analytics] ⚠️ No real data - using MOCK DATA');
                setUseMockData(true);
                setSensors(mockData.sensors);

                const hours = timeRange === '24h' ? 24 : timeRange === '7d' ? 168 : 720;
                const history = mockData.generateReadingsHistory('SENSOR_TUNIS_001', hours);
                setAllReadings(history);

                if (!selectedSensorId && mockData.sensors.length > 0) {
                    setSelectedSensorId(mockData.sensors[0].deviceId);
                }
            }
        } catch (err) {
            console.error('[Analytics] Error:', err);
            setError(err.message);
            setUseMockData(true);
            setSensors(mockData.sensors);

            const hours = timeRange === '24h' ? 24 : timeRange === '7d' ? 168 : 720;
            setAllReadings(mockData.generateReadingsHistory('SENSOR_TUNIS_001', hours));
        } finally {
            setIsLoading(false);
        }
    };

    // Fetch ML predictions
    const fetchMLPredictions = async (reading) => {
        if (!reading) return;

        setMlError(null);

        try {
            // Check ML service health
            const health = await mlAPI.health();
            setMlHealth(health);
            console.log('[Analytics] ML Service Health:', health);

            // Format the reading data to match MLOps API schema exactly
            const formattedReading = {
                sensorId: String(reading.sensorId || reading.sensor_id || reading.deviceId || 'UNKNOWN'),
                temperature: parseFloat(reading.temperature) || 25.0,
                humidity: parseFloat(reading.humidity) || 60.0,
                co2: parseFloat(reading.co2) || 500.0,
                voc: parseFloat(reading.voc) || 0.5,
                pm25: parseFloat(reading.pm25) || 15.0,
                pm10: parseFloat(reading.pm10) || 30.0,
                timestamp: reading.timestamp ? new Date(reading.timestamp).toISOString() : new Date().toISOString(),
            };

            console.log('[Analytics] Sending to ML API:', formattedReading);

            // Get prediction for latest reading
            const prediction = await mlAPI.predict(formattedReading);

            console.log('[Analytics] ML Prediction:', prediction);
            setMlPredictions(prediction);

        } catch (err) {
            console.warn('[Analytics] ML prediction failed:', err.message);
            setMlError(err.message);
        }
    };

    // Refresh data
    const refreshData = async () => {
        setIsRefreshing(true);
        await loadData();
        setIsRefreshing(false);
    };

    // ✅ FIX: Calculate statistics based on FILTERED data
    const stats = useMemo(() => {
        if (!filteredData.length) return null;

        const pm25Values = filteredData.map(r => r.pm25).filter(v => v != null);
        const tempValues = filteredData.map(r => r.temperature).filter(v => v != null);
        const co2Values = filteredData.map(r => r.co2).filter(v => v != null);

        const avg = (arr) => arr.length ? arr.reduce((a, b) => a + b, 0) / arr.length : 0;
        const min = (arr) => arr.length ? Math.min(...arr) : 0;
        const max = (arr) => arr.length ? Math.max(...arr) : 0;

        return {
            pm25: { avg: avg(pm25Values), min: min(pm25Values), max: max(pm25Values) },
            temperature: { avg: avg(tempValues), min: min(tempValues), max: max(tempValues) },
            co2: { avg: avg(co2Values), min: min(co2Values), max: max(co2Values) },
            count: filteredData.length,
        };
    }, [filteredData]);

    // Calculate trend based on filtered data
    const trend = useMemo(() => {
        if (filteredData.length < 12) return 0;
        const recent = filteredData.slice(-6);
        const older = filteredData.slice(0, 6);
        const recentAvg = recent.reduce((a, b) => a + (b.pm25 || 0), 0) / recent.length;
        const olderAvg = older.reduce((a, b) => a + (b.pm25 || 0), 0) / older.length;
        return olderAvg ? ((recentAvg - olderAvg) / olderAvg) * 100 : 0;
    }, [filteredData]);

    // Get selected sensor object
    const selectedSensor = useMemo(() => {
        return sensors.find(s => (s.deviceId || s.sensorId) === selectedSensorId);
    }, [sensors, selectedSensorId]);

    // Loading state
    if (isLoading) {
        return (
            <div className="flex items-center justify-center min-h-[60vh]">
                <div className="text-center">
                    <RefreshCw className="w-12 h-12 text-air-600 animate-spin mx-auto mb-4" />
                    <p className="text-slate-600 dark:text-slate-400">Loading analytics...</p>
                </div>
            </div>
        );
    }

    return (
        <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="space-y-6"
        >
            {/* Data Source Indicator */}
            {useMockData && (
                <div className="bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-xl p-4">
                    <div className="flex items-center gap-3">
                        <WifiOff className="w-5 h-5 text-yellow-600" />
                        <div>
                            <p className="font-medium text-yellow-800 dark:text-yellow-200">
                                Demo Mode - Using Mock Data
                            </p>
                            <p className="text-sm text-yellow-600 dark:text-yellow-400">
                                Connect your sensors to see real analytics
                            </p>
                        </div>
                        <button onClick={loadData} className="ml-auto btn btn-secondary text-sm">
                            Retry Connection
                        </button>
                    </div>
                </div>
            )}

            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-display font-bold text-slate-900 dark:text-white">
                        Analytics
                    </h1>
                    <p className="text-slate-500 dark:text-slate-400">
                        {useMockData
                            ? 'Demo data - Connect sensors for real analytics'
                            : 'Analyze air quality trends and patterns'
                        }
                    </p>
                </div>

                <div className="flex items-center gap-3">
                    {/* Time Range */}
                    <select
                        value={timeRange}
                        onChange={(e) => setTimeRange(e.target.value)}
                        className="input"
                    >
                        <option value="24h">Last 24 Hours</option>
                        <option value="7d">Last 7 Days</option>
                        <option value="30d">Last 30 Days</option>
                    </select>

                    {/* Sensor Select - ✅ FIXED */}
                    <select
                        value={selectedSensorId}
                        onChange={(e) => {
                            console.log('[Analytics] Sensor changed to:', e.target.value);
                            setSelectedSensorId(e.target.value);
                        }}
                        className="input"
                    >
                        {sensors.map(sensor => (
                            <option key={sensor.id || sensor.deviceId} value={sensor.deviceId || sensor.sensorId}>
                                {sensor.deviceId || sensor.sensorId}
                            </option>
                        ))}
                    </select>

                    <button onClick={refreshData} disabled={isRefreshing} className="btn btn-secondary">
                        <RefreshCw className={`w-4 h-4 ${isRefreshing ? 'animate-spin' : ''}`} />
                    </button>

                    <button className="btn btn-secondary">
                        <Download className="w-4 h-4" />
                        Export
                    </button>
                </div>
            </div>

            {/* ML Predictions Card */}
            <div className="glass-card p-6 bg-gradient-to-r from-violet-500/10 to-purple-500/10 border-violet-200 dark:border-violet-800">
                <div className="flex items-center gap-3 mb-4">
                    <div className="w-10 h-10 rounded-xl bg-violet-100 dark:bg-violet-900/30 flex items-center justify-center">
                        <Brain className="w-5 h-5 text-violet-600" />
                    </div>
                    <div>
                        <h2 className="font-semibold text-slate-900 dark:text-white">AI Prediction</h2>
                        <p className="text-sm text-slate-500">
                            Machine Learning analysis for {selectedSensorId || 'selected sensor'}
                        </p>
                    </div>
                    {mlHealth && (
                        <span className="ml-auto text-xs px-2 py-1 rounded-full bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400">
                            ML Service Online
                        </span>
                    )}
                </div>

                {mlError ? (
                    <div className="flex items-center gap-2 text-red-600 dark:text-red-400">
                        <AlertCircle className="w-4 h-4" />
                        <span className="text-sm">ML prediction unavailable: {mlError}</span>
                    </div>
                ) : mlPredictions ? (
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                        <div>
                            <p className="text-sm text-slate-500">Predicted AQI</p>
                            <p className="text-2xl font-bold text-violet-600">
                                {formatNumber(mlPredictions.predicted_aqi, 0)}
                            </p>
                        </div>
                        <div>
                            <p className="text-sm text-slate-500">Anomaly Score</p>
                            <p className="text-2xl font-bold text-green-600">
                                {formatNumber(mlPredictions.anomaly_score * 100, 0)}%
                            </p>
                        </div>
                        <div>
                            <p className="text-sm text-slate-500">Risk Level</p>
                            <p className={`text-2xl font-bold ${
                                mlPredictions.risk_level === 'LOW' ? 'text-green-600' :
                                    mlPredictions.risk_level === 'MEDIUM' ? 'text-yellow-600' :
                                        mlPredictions.risk_level === 'HIGH' ? 'text-orange-600' :
                                            'text-red-600'
                            }`}>
                                {mlPredictions.risk_level}
                            </p>
                        </div>
                        <div>
                            <p className="text-sm text-slate-500">Status</p>
                            <p className={`text-2xl font-bold flex items-center gap-2 ${
                                mlPredictions.is_anomaly ? 'text-red-600' : 'text-green-600'
                            }`}>
                                <Activity className="w-5 h-5" />
                                {mlPredictions.is_anomaly ? 'Anomaly' : 'Normal'}
                            </p>
                        </div>
                    </div>
                ) : (
                    <div className="text-sm text-slate-500">Loading predictions...</div>
                )}
            </div>

            {/* Stats Cards - ✅ NOW USES FILTERED DATA */}
            {stats && (
                <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
                    <div className="glass-card p-5">
                        <div className="flex items-center justify-between">
                            <div>
                                <p className="text-sm text-slate-500">Avg PM2.5</p>
                                <p className="text-3xl font-display font-bold text-slate-900 dark:text-white">
                                    {formatNumber(stats.pm25.avg)}
                                </p>
                                <p className="text-xs text-slate-400">µg/m³ ({stats.count} readings)</p>
                            </div>
                            <div
                                className={`
                  flex items-center gap-1 px-2 py-1 rounded-full text-sm font-medium
                  ${trend < 0
                                    ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
                                    : 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400'
                                }
                `}
                            >
                                {trend < 0 ? <TrendingDown className="w-4 h-4" /> : <TrendingUp className="w-4 h-4" />}
                                {Math.abs(trend).toFixed(1)}%
                            </div>
                        </div>
                    </div>

                    <div className="glass-card p-5">
                        <p className="text-sm text-slate-500">Avg Temperature</p>
                        <p className="text-3xl font-display font-bold text-slate-900 dark:text-white">
                            {formatNumber(stats.temperature.avg)}°C
                        </p>
                        <p className="text-xs text-slate-400">
                            Range: {formatNumber(stats.temperature.min)}° - {formatNumber(stats.temperature.max)}°
                        </p>
                    </div>

                    <div className="glass-card p-5">
                        <p className="text-sm text-slate-500">Avg CO₂</p>
                        <p className="text-3xl font-display font-bold text-slate-900 dark:text-white">
                            {formatNumber(stats.co2.avg, 0)}
                        </p>
                        <p className="text-xs text-slate-400">ppm</p>
                    </div>

                    <div className="glass-card p-5">
                        <p className="text-sm text-slate-500">Avg AQI</p>
                        <p className="text-3xl font-display font-bold" style={{ color: calculateAQI(stats.pm25.avg).color }}>
                            {calculateAQI(stats.pm25.avg).aqi}
                        </p>
                        <p className="text-xs text-slate-400">
                            {calculateAQI(stats.pm25.avg).category}
                        </p>
                    </div>
                </div>
            )}

            {/* No Data Message */}
            {filteredData.length === 0 && (
                <div className="glass-card p-8 text-center">
                    <Activity className="w-12 h-12 text-slate-400 mx-auto mb-4" />
                    <h3 className="text-lg font-semibold text-slate-900 dark:text-white mb-2">
                        No Data Available
                    </h3>
                    <p className="text-slate-500">
                        No readings found for {selectedSensorId} in the {timeRange === '24h' ? 'last 24 hours' : timeRange === '7d' ? 'last 7 days' : 'last 30 days'}.
                    </p>
                </div>
            )}

            {/* Main Chart - ✅ NOW USES FILTERED DATA */}
            {filteredData.length > 0 && (
                <>
                    <ChartCard
                        title="Air Quality Over Time"
                        subtitle={`${selectedSensorId || 'All Sensors'} - ${
                            timeRange === '24h' ? 'Last 24 Hours' :
                                timeRange === '7d' ? 'Last 7 Days' : 'Last 30 Days'
                        } (${filteredData.length} readings)`}
                    >
                        <TimeSeriesChart
                            data={filteredData}
                            metrics={['pm25', 'pm10']}
                            height={300}
                            showLegend={true}
                        />
                    </ChartCard>

                    {/* Multi-metric Charts */}
                    <MultiMetricChart data={filteredData} height={300} />
                </>
            )}

            {/* Metric Selection Cards */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <div className="glass-card p-5">
                    <h3 className="font-semibold text-slate-900 dark:text-white mb-4">
                        Peak Hours Analysis
                    </h3>
                    <div className="space-y-4">
                        {[
                            { hour: '8:00 - 9:00', label: 'Morning Rush', value: 45 },
                            { hour: '12:00 - 13:00', label: 'Midday', value: 38 },
                            { hour: '17:00 - 18:00', label: 'Evening Rush', value: 52 },
                            { hour: '22:00 - 23:00', label: 'Night', value: 28 },
                        ].map((period) => (
                            <div key={period.hour} className="flex items-center gap-4">
                                <div className="w-24 text-sm text-slate-500">{period.hour}</div>
                                <div className="flex-1">
                                    <div className="flex items-center justify-between mb-1">
                                        <span className="text-sm font-medium">{period.label}</span>
                                        <span className="text-sm text-slate-500">
                      AQI {calculateAQI(period.value).aqi}
                    </span>
                                    </div>
                                    <div className="h-2 bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
                                        <div
                                            className="h-full rounded-full transition-all"
                                            style={{
                                                width: `${(period.value / 60) * 100}%`,
                                                backgroundColor: calculateAQI(period.value).color
                                            }}
                                        />
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                <div className="glass-card p-5">
                    <h3 className="font-semibold text-slate-900 dark:text-white mb-4">
                        Air Quality Distribution
                    </h3>
                    <div className="space-y-3">
                        {[
                            { category: 'Good', color: '#22c55e', percentage: 45 },
                            { category: 'Moderate', color: '#eab308', percentage: 30 },
                            { category: 'Unhealthy for Sensitive', color: '#f97316', percentage: 15 },
                            { category: 'Unhealthy', color: '#ef4444', percentage: 8 },
                            { category: 'Very Unhealthy', color: '#991b1b', percentage: 2 },
                        ].map((item) => (
                            <div key={item.category} className="flex items-center gap-3">
                                <div
                                    className="w-3 h-3 rounded-full flex-shrink-0"
                                    style={{ backgroundColor: item.color }}
                                />
                                <div className="flex-1">
                                    <div className="flex justify-between text-sm mb-1">
                                        <span>{item.category}</span>
                                        <span className="font-medium">{item.percentage}%</span>
                                    </div>
                                    <div className="h-1.5 bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
                                        <div
                                            className="h-full rounded-full"
                                            style={{ width: `${item.percentage}%`, backgroundColor: item.color }}
                                        />
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>

            {/* Health Insights */}
            <div className="glass-card p-6">
                <h3 className="font-semibold text-slate-900 dark:text-white mb-4">
                    Health Insights
                </h3>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    {stats && [
                        getHealthRecommendation(calculateAQI(stats.pm25.min).aqi),
                        getHealthRecommendation(calculateAQI(stats.pm25.avg).aqi),
                        getHealthRecommendation(calculateAQI(stats.pm25.max).aqi),
                    ].map((rec, idx) => (
                        <div
                            key={idx}
                            className="p-4 rounded-xl bg-slate-50 dark:bg-slate-800/50"
                        >
                            <div className="flex items-center gap-2 mb-2">
                                <span className="text-2xl">{rec.icon}</span>
                                <span className="font-medium text-slate-700 dark:text-slate-300">
                  {['Best', 'Average', 'Worst'][idx]} Conditions
                </span>
                            </div>
                            <p className="text-sm text-slate-600 dark:text-slate-400">
                                {rec.message}
                            </p>
                        </div>
                    ))}
                </div>
            </div>
        </motion.div>
    );
}