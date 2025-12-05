import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Search,
  Filter,
  Grid,
  List,
  Plus,
  MapPin,
  Activity,
  Settings,
  MoreVertical,
  Wifi,
  WifiOff,
  RefreshCw,
  Thermometer,
  Wind,
  Droplets,
} from 'lucide-react';
import { useSensors } from '../context/AppContext';
import { mockData } from '../services/api';
import { calculateAQI, formatNumber, formatRelativeTime, getSensorStatus } from '../utils/helpers';
import { AQIBadge } from '../components/Gauges/AQIGauge';
import { Sparkline } from '../components/Charts/TimeSeriesChart';

/**
 * Sensor Card Component
 */
function SensorCard({ sensor, reading, history, onClick }) {
  const status = getSensorStatus(sensor.status);
  const { aqi, color, category } = calculateAQI(reading?.pm25 || 0);
  const isActive = sensor.status === 'ACTIVE';
  
  return (
    <motion.div
      layout
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      exit={{ opacity: 0, scale: 0.95 }}
      whileHover={{ y: -4 }}
      className="glass-card overflow-hidden cursor-pointer group"
      onClick={onClick}
    >
      {/* Header */}
      <div className="p-4 pb-0">
        <div className="flex items-start justify-between">
          <div>
            <div className="flex items-center gap-2">
              <div 
                className="w-2.5 h-2.5 rounded-full"
                style={{ 
                  backgroundColor: status.color,
                  boxShadow: isActive ? `0 0 8px ${status.color}` : 'none'
                }}
              />
              <h3 className="font-semibold text-slate-900 dark:text-white">
                {sensor.deviceId}
              </h3>
            </div>
            <p className="text-sm text-slate-500 mt-0.5">{sensor.description}</p>
          </div>
          
          <button 
            className="p-1.5 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg opacity-0 group-hover:opacity-100 transition-opacity"
            onClick={(e) => { e.stopPropagation(); }}
          >
            <MoreVertical className="w-4 h-4 text-slate-400" />
          </button>
        </div>
        
        {/* Location */}
        <div className="flex items-center gap-1.5 text-sm text-slate-500 mt-2">
          <MapPin className="w-4 h-4" />
          <span>{sensor.location?.city}, {sensor.location?.country}</span>
        </div>
      </div>
      
      {/* AQI Display */}
      {isActive && reading && (
        <div className="px-4 py-4">
          <div 
            className="rounded-xl p-4"
            style={{ backgroundColor: `${color}15` }}
          >
            <div className="flex items-center justify-between">
              <div>
                <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">
                  Air Quality Index
                </span>
                <div className="flex items-baseline gap-2 mt-1">
                  <span 
                    className="text-4xl font-display font-bold"
                    style={{ color }}
                  >
                    {aqi}
                  </span>
                  <span 
                    className="text-sm font-medium"
                    style={{ color }}
                  >
                    {category}
                  </span>
                </div>
              </div>
              <Activity className="w-10 h-10" style={{ color, opacity: 0.6 }} />
            </div>
          </div>
        </div>
      )}
      
      {/* Offline State */}
      {!isActive && (
        <div className="px-4 py-6">
          <div className="flex items-center justify-center gap-2 text-slate-400">
            <WifiOff className="w-5 h-5" />
            <span className="font-medium">Sensor Offline</span>
          </div>
        </div>
      )}
      
      {/* Quick Metrics */}
      {isActive && reading && (
        <div className="px-4 pb-4">
          <div className="grid grid-cols-3 gap-2">
            <div className="text-center p-2 rounded-lg bg-slate-50 dark:bg-slate-800/50">
              <Thermometer className="w-4 h-4 text-red-500 mx-auto mb-1" />
              <span className="text-sm font-semibold">{formatNumber(reading.temperature)}°</span>
            </div>
            <div className="text-center p-2 rounded-lg bg-slate-50 dark:bg-slate-800/50">
              <Droplets className="w-4 h-4 text-blue-500 mx-auto mb-1" />
              <span className="text-sm font-semibold">{formatNumber(reading.humidity)}%</span>
            </div>
            <div className="text-center p-2 rounded-lg bg-slate-50 dark:bg-slate-800/50">
              <Wind className="w-4 h-4 text-violet-500 mx-auto mb-1" />
              <span className="text-sm font-semibold">{formatNumber(reading.co2, 0)}</span>
            </div>
          </div>
        </div>
      )}
      
      {/* Trend Line */}
      {isActive && history && history.length > 0 && (
        <div className="px-4 pb-4">
          <div className="flex items-center justify-between text-xs text-slate-500 mb-1">
            <span>24h PM2.5 Trend</span>
            <span>Last: {formatNumber(reading?.pm25)} µg/m³</span>
          </div>
          <Sparkline 
            data={history} 
            metric="pm25" 
            width="100%" 
            height={40}
            color={color}
          />
        </div>
      )}
      
      {/* Footer */}
      <div className="px-4 py-3 bg-slate-50 dark:bg-slate-800/30 border-t border-slate-200 dark:border-slate-700">
        <div className="flex items-center justify-between text-xs">
          <span className="text-slate-500">
            Model: {sensor.model}
          </span>
          <span 
            className={`
              px-2 py-0.5 rounded-full font-medium
              ${status.bgClass}
            `}
            style={{ color: status.color }}
          >
            {status.label}
          </span>
        </div>
      </div>
    </motion.div>
  );
}

/**
 * Sensor List Item Component
 */
function SensorListItem({ sensor, reading, onClick }) {
  const status = getSensorStatus(sensor.status);
  const { aqi, color, category } = calculateAQI(reading?.pm25 || 0);
  const isActive = sensor.status === 'ACTIVE';
  
  return (
    <motion.tr
      layout
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="hover:bg-slate-50 dark:hover:bg-slate-800/50 cursor-pointer"
      onClick={onClick}
    >
      <td className="px-4 py-4">
        <div className="flex items-center gap-3">
          <div 
            className="w-2.5 h-2.5 rounded-full flex-shrink-0"
            style={{ 
              backgroundColor: status.color,
              boxShadow: isActive ? `0 0 8px ${status.color}` : 'none'
            }}
          />
          <div>
            <p className="font-medium text-slate-900 dark:text-white">
              {sensor.deviceId}
            </p>
            <p className="text-sm text-slate-500">{sensor.description}</p>
          </div>
        </div>
      </td>
      <td className="px-4 py-4">
        <div className="flex items-center gap-1.5 text-sm text-slate-500">
          <MapPin className="w-4 h-4" />
          {sensor.location?.city}
        </div>
      </td>
      <td className="px-4 py-4">
        <span 
          className={`px-2.5 py-1 rounded-full text-xs font-medium ${status.bgClass}`}
          style={{ color: status.color }}
        >
          {status.label}
        </span>
      </td>
      <td className="px-4 py-4">
        {isActive && reading ? (
          <div className="flex items-center gap-2">
            <span 
              className="text-lg font-bold"
              style={{ color }}
            >
              {aqi}
            </span>
            <span className="text-xs text-slate-500">{category}</span>
          </div>
        ) : (
          <span className="text-slate-400">—</span>
        )}
      </td>
      <td className="px-4 py-4 text-sm text-slate-500">
        {isActive && reading ? `${formatNumber(reading.pm25)} µg/m³` : '—'}
      </td>
      <td className="px-4 py-4 text-sm text-slate-500">
        {isActive && reading ? `${formatNumber(reading.temperature)}°C` : '—'}
      </td>
      <td className="px-4 py-4">
        <button 
          className="p-2 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg"
          onClick={(e) => { e.stopPropagation(); }}
        >
          <Settings className="w-4 h-4 text-slate-400" />
        </button>
      </td>
    </motion.tr>
  );
}

/**
 * Sensors Page
 */
export default function SensorsPage() {
  const { sensors, setSensors, setSelectedSensor } = useSensors();
  const [currentReadings, setCurrentReadings] = useState({});
  const [historicalData, setHistoricalData] = useState({});
  const [searchQuery, setSearchQuery] = useState('');
  const [filterStatus, setFilterStatus] = useState('all');
  const [viewMode, setViewMode] = useState('grid');
  const [isRefreshing, setIsRefreshing] = useState(false);
  
  // Load data
  useEffect(() => {
    setSensors(mockData.sensors);
    
    const readings = {};
    const history = {};
    mockData.sensors.forEach(sensor => {
      readings[sensor.deviceId] = mockData.generateReading(sensor.deviceId);
      history[sensor.deviceId] = mockData.generateReadingsHistory(sensor.deviceId, 24);
    });
    setCurrentReadings(readings);
    setHistoricalData(history);
  }, []);
  
  // Filter sensors
  const filteredSensors = sensors.filter(sensor => {
    const matchesSearch = sensor.deviceId.toLowerCase().includes(searchQuery.toLowerCase()) ||
      sensor.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
      sensor.location?.city?.toLowerCase().includes(searchQuery.toLowerCase());
    
    const matchesStatus = filterStatus === 'all' || sensor.status === filterStatus;
    
    return matchesSearch && matchesStatus;
  });
  
  // Refresh data
  const handleRefresh = async () => {
    setIsRefreshing(true);
    
    const readings = {};
    sensors.forEach(sensor => {
      readings[sensor.deviceId] = mockData.generateReading(sensor.deviceId);
    });
    setCurrentReadings(readings);
    
    setTimeout(() => setIsRefreshing(false), 500);
  };
  
  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="space-y-6"
    >
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-display font-bold text-slate-900 dark:text-white">
            Sensors
          </h1>
          <p className="text-slate-500 dark:text-slate-400">
            Manage and monitor your air quality sensors
          </p>
        </div>
        
        <button className="btn btn-primary">
          <Plus className="w-4 h-4" />
          Add Sensor
        </button>
      </div>
      
      {/* Filters */}
      <div className="glass-card p-4">
        <div className="flex flex-col sm:flex-row gap-4">
          {/* Search */}
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
            <input
              type="text"
              placeholder="Search sensors..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="input pl-10 w-full"
            />
          </div>
          
          {/* Status Filter */}
          <select
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            className="input w-full sm:w-40"
          >
            <option value="all">All Status</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
            <option value="OFFLINE">Offline</option>
            <option value="MAINTENANCE">Maintenance</option>
          </select>
          
          {/* View Toggle */}
          <div className="flex items-center gap-1 bg-slate-100 dark:bg-slate-800 rounded-xl p-1">
            <button
              onClick={() => setViewMode('grid')}
              className={`p-2 rounded-lg transition-colors ${
                viewMode === 'grid' 
                  ? 'bg-white dark:bg-slate-700 shadow-sm' 
                  : 'hover:bg-slate-200 dark:hover:bg-slate-600'
              }`}
            >
              <Grid className="w-4 h-4" />
            </button>
            <button
              onClick={() => setViewMode('list')}
              className={`p-2 rounded-lg transition-colors ${
                viewMode === 'list' 
                  ? 'bg-white dark:bg-slate-700 shadow-sm' 
                  : 'hover:bg-slate-200 dark:hover:bg-slate-600'
              }`}
            >
              <List className="w-4 h-4" />
            </button>
          </div>
          
          {/* Refresh */}
          <button
            onClick={handleRefresh}
            disabled={isRefreshing}
            className="btn btn-secondary"
          >
            <RefreshCw className={`w-4 h-4 ${isRefreshing ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>
      
      {/* Stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="glass-card p-4">
          <p className="text-sm text-slate-500">Total Sensors</p>
          <p className="text-2xl font-bold">{sensors.length}</p>
        </div>
        <div className="glass-card p-4">
          <p className="text-sm text-slate-500">Active</p>
          <p className="text-2xl font-bold text-green-600">
            {sensors.filter(s => s.status === 'ACTIVE').length}
          </p>
        </div>
        <div className="glass-card p-4">
          <p className="text-sm text-slate-500">Offline</p>
          <p className="text-2xl font-bold text-red-600">
            {sensors.filter(s => s.status === 'OFFLINE').length}
          </p>
        </div>
        <div className="glass-card p-4">
          <p className="text-sm text-slate-500">Avg AQI</p>
          <p className="text-2xl font-bold text-air-600">
            {Math.round(
              Object.values(currentReadings)
                .filter(r => r?.pm25)
                .reduce((a, b) => a + calculateAQI(b.pm25).aqi, 0) / 
              Object.values(currentReadings).filter(r => r?.pm25).length || 0
            )}
          </p>
        </div>
      </div>
      
      {/* Sensor Grid/List */}
      <AnimatePresence mode="wait">
        {viewMode === 'grid' ? (
          <motion.div
            key="grid"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6"
          >
            {filteredSensors.map(sensor => (
              <SensorCard
                key={sensor.id}
                sensor={sensor}
                reading={currentReadings[sensor.deviceId]}
                history={historicalData[sensor.deviceId]}
                onClick={() => setSelectedSensor(sensor)}
              />
            ))}
          </motion.div>
        ) : (
          <motion.div
            key="list"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="glass-card overflow-hidden"
          >
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-slate-50 dark:bg-slate-800/50 border-b border-slate-200 dark:border-slate-700">
                  <tr>
                    <th className="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">
                      Sensor
                    </th>
                    <th className="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">
                      Location
                    </th>
                    <th className="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">
                      Status
                    </th>
                    <th className="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">
                      AQI
                    </th>
                    <th className="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">
                      PM2.5
                    </th>
                    <th className="text-left px-4 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">
                      Temp
                    </th>
                    <th className="px-4 py-3"></th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 dark:divide-slate-700">
                  {filteredSensors.map(sensor => (
                    <SensorListItem
                      key={sensor.id}
                      sensor={sensor}
                      reading={currentReadings[sensor.deviceId]}
                      onClick={() => setSelectedSensor(sensor)}
                    />
                  ))}
                </tbody>
              </table>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
      
      {/* Empty State */}
      {filteredSensors.length === 0 && (
        <div className="glass-card p-12 text-center">
          <Activity className="w-12 h-12 text-slate-300 mx-auto mb-4" />
          <h3 className="text-lg font-semibold text-slate-700 dark:text-slate-300">
            No sensors found
          </h3>
          <p className="text-slate-500 mt-1">
            Try adjusting your search or filter criteria
          </p>
        </div>
      )}
    </motion.div>
  );
}
