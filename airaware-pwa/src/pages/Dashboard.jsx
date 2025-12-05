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
} from 'lucide-react';
import { useApp, useSensors, useAlerts } from '../context/AppContext';
import { mockData } from '../services/api';
import { calculateAQI, formatRelativeTime } from '../utils/helpers';
import AQIGauge, { AQIBadge } from '../components/Gauges/AQIGauge';
import MetricCard, { MetricGrid } from '../components/Cards/MetricCard';
import SensorMap from '../components/Map/SensorMap';
import { TimeSeriesChart, ChartCard } from '../components/Charts/TimeSeriesChart';
import AlertList, { AlertSummary } from '../components/Alerts/AlertList';

/**
 * Dashboard Page
 * Main overview with real-time air quality data
 */
export default function Dashboard() {
  const { state, actions } = useApp();
  const { sensors, setSensors, setSelectedSensor } = useSensors();
  const { alerts, setAlerts, resolveAlert } = useAlerts();
  
  const [currentReadings, setCurrentReadings] = useState({});
  const [historicalData, setHistoricalData] = useState([]);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [lastUpdate, setLastUpdate] = useState(new Date());
  
  // Load initial data
  useEffect(() => {
    loadData();
    
    // Set up auto-refresh
    const interval = setInterval(refreshData, 30000); // Every 30 seconds
    return () => clearInterval(interval);
  }, []);
  
  const loadData = async () => {
    try {
      // Use mock data for now - replace with API calls
      setSensors(mockData.sensors);
      setAlerts(mockData.alerts);
      
      // Generate readings for each sensor
      const readings = {};
      mockData.sensors.forEach(sensor => {
        readings[sensor.deviceId] = mockData.generateReading(sensor.deviceId);
      });
      setCurrentReadings(readings);
      
      // Generate historical data for the first sensor
      const history = mockData.generateReadingsHistory('SENSOR_TUNIS_001', 24);
      setHistoricalData(history);
      
      setLastUpdate(new Date());
    } catch (error) {
      console.error('Failed to load data:', error);
    }
  };
  
  const refreshData = async () => {
    setIsRefreshing(true);
    
    // Update readings
    const readings = {};
    sensors.forEach(sensor => {
      readings[sensor.deviceId] = mockData.generateReading(sensor.deviceId);
    });
    setCurrentReadings(readings);
    
    // Add new reading to history
    const newReading = mockData.generateReading('SENSOR_TUNIS_001');
    setHistoricalData(prev => [...prev.slice(-47), newReading]);
    
    setLastUpdate(new Date());
    setTimeout(() => setIsRefreshing(false), 500);
  };
  
  // Calculate overall AQI (average of all sensors)
  const overallAQI = useMemo(() => {
    const pm25Values = Object.values(currentReadings)
      .map(r => r?.pm25)
      .filter(Boolean);
    
    if (!pm25Values.length) return 0;
    return pm25Values.reduce((a, b) => a + b, 0) / pm25Values.length;
  }, [currentReadings]);
  
  // Get primary sensor reading
  const primaryReading = currentReadings['SENSOR_TUNIS_001'];
  
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
  
  return (
    <motion.div
      variants={containerVariants}
      initial="hidden"
      animate="visible"
      className="space-y-6"
    >
      {/* Header */}
      <motion.div variants={itemVariants} className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-display font-bold text-slate-900 dark:text-white">
            Dashboard
          </h1>
          <p className="text-slate-500 dark:text-slate-400">
            Real-time air quality monitoring across Tunisia
          </p>
        </div>
        
        <div className="flex items-center gap-3">
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
                {historicalData.length * sensors.length}
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
                  Downtown Tunis
                </span>
              </div>
            </div>
          </div>
        </motion.div>
        
        {/* Metric Cards */}
        <motion.div variants={itemVariants} className="lg:col-span-2">
          <div className="glass-card p-6">
            <h2 className="font-semibold text-slate-900 dark:text-white mb-4">
              Sensor Readings
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
            subtitle="PM2.5 levels over the last 24 hours"
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
                const reading = currentReadings[sensor.deviceId];
                const { aqi, color } = calculateAQI(reading?.pm25 || 0);
                const isActive = sensor.status === 'ACTIVE';
                
                return (
                  <div
                    key={sensor.id}
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
                        {sensor.description}
                      </p>
                      <p className="text-sm text-slate-500">
                        {sensor.location?.city}
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
            </div>
          </div>
        </motion.div>
      </div>
    </motion.div>
  );
}
