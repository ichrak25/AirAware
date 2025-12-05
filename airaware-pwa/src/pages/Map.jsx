import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { 
  Layers, 
  Filter, 
  MapPin, 
  Activity,
  X,
  ChevronRight,
} from 'lucide-react';
import { useSensors } from '../context/AppContext';
import { mockData } from '../services/api';
import { calculateAQI, formatNumber, getSensorStatus } from '../utils/helpers';
import SensorMap from '../components/Map/SensorMap';
import AQIGauge from '../components/Gauges/AQIGauge';
import { MetricRow } from '../components/Cards/MetricCard';
import { Sparkline } from '../components/Charts/TimeSeriesChart';

/**
 * Map Page
 * Full-screen interactive map with sensor details panel
 */
export default function MapPage() {
  const { sensors, setSensors, selectedSensor, setSelectedSensor } = useSensors();
  const [currentReadings, setCurrentReadings] = useState({});
  const [historicalData, setHistoricalData] = useState({});
  const [filterStatus, setFilterStatus] = useState('all');
  const [showPanel, setShowPanel] = useState(false);
  
  // Load data
  useEffect(() => {
    // Load sensors
    setSensors(mockData.sensors);
    
    // Generate readings
    const readings = {};
    const history = {};
    mockData.sensors.forEach(sensor => {
      readings[sensor.deviceId] = mockData.generateReading(sensor.deviceId);
      history[sensor.deviceId] = mockData.generateReadingsHistory(sensor.deviceId, 24);
    });
    setCurrentReadings(readings);
    setHistoricalData(history);
    
    // Auto-refresh
    const interval = setInterval(() => {
      const newReadings = {};
      mockData.sensors.forEach(sensor => {
        newReadings[sensor.deviceId] = mockData.generateReading(sensor.deviceId);
      });
      setCurrentReadings(newReadings);
    }, 30000);
    
    return () => clearInterval(interval);
  }, []);
  
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
  const selectedReading = selectedSensor 
    ? currentReadings[selectedSensor.deviceId] 
    : null;
  const selectedHistory = selectedSensor 
    ? historicalData[selectedSensor.deviceId] 
    : [];
  
  return (
    <div className="h-[calc(100vh-8rem)] lg:h-[calc(100vh-6rem)] relative">
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
          </h3>
          <div className="space-y-2">
            {filteredSensors.map(sensor => {
              const reading = currentReadings[sensor.deviceId];
              const { aqi, color } = calculateAQI(reading?.pm25 || 0);
              const isSelected = selectedSensor?.id === sensor.id;
              
              return (
                <button
                  key={sensor.id}
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
                      {sensor.deviceId}
                    </p>
                    <p className="text-xs text-slate-500 truncate">
                      {sensor.location?.city}
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
                  {selectedSensor.deviceId}
                </h2>
                <p className="text-sm text-slate-500">
                  {selectedSensor.description}
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
                  {selectedSensor.location?.city}, {selectedSensor.location?.country}
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
                    <span className="font-medium">{selectedSensor.model}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500">Latitude</span>
                    <span className="font-mono">{selectedSensor.location?.latitude.toFixed(4)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500">Longitude</span>
                    <span className="font-mono">{selectedSensor.location?.longitude.toFixed(4)}</span>
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
