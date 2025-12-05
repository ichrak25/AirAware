import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import {
  TrendingUp,
  TrendingDown,
  Calendar,
  Download,
  BarChart3,
  Activity,
} from 'lucide-react';
import { useSensors } from '../context/AppContext';
import { mockData } from '../services/api';
import { calculateAQI, formatNumber, getHealthRecommendation } from '../utils/helpers';
import { TimeSeriesChart, MultiMetricChart, ChartCard } from '../components/Charts/TimeSeriesChart';

/**
 * Analytics Page
 */
export default function AnalyticsPage() {
  const { sensors, setSensors } = useSensors();
  const [historicalData, setHistoricalData] = useState([]);
  const [selectedSensor, setSelectedSensor] = useState(null);
  const [timeRange, setTimeRange] = useState('24h');
  const [selectedMetric, setSelectedMetric] = useState('pm25');
  
  // Load data
  useEffect(() => {
    setSensors(mockData.sensors);
    
    // Generate historical data
    const hours = timeRange === '24h' ? 24 : timeRange === '7d' ? 168 : 720;
    const history = mockData.generateReadingsHistory('SENSOR_TUNIS_001', hours);
    setHistoricalData(history);
    
    // Set default selected sensor
    if (mockData.sensors.length > 0 && !selectedSensor) {
      setSelectedSensor(mockData.sensors[0]);
    }
  }, [timeRange]);
  
  // Calculate statistics
  const stats = React.useMemo(() => {
    if (!historicalData.length) return null;
    
    const pm25Values = historicalData.map(r => r.pm25);
    const tempValues = historicalData.map(r => r.temperature);
    const co2Values = historicalData.map(r => r.co2);
    
    const avg = (arr) => arr.reduce((a, b) => a + b, 0) / arr.length;
    const min = (arr) => Math.min(...arr);
    const max = (arr) => Math.max(...arr);
    
    return {
      pm25: { avg: avg(pm25Values), min: min(pm25Values), max: max(pm25Values) },
      temperature: { avg: avg(tempValues), min: min(tempValues), max: max(tempValues) },
      co2: { avg: avg(co2Values), min: min(co2Values), max: max(co2Values) },
    };
  }, [historicalData]);
  
  // Calculate trend
  const trend = React.useMemo(() => {
    if (historicalData.length < 2) return 0;
    const recent = historicalData.slice(-6);
    const older = historicalData.slice(0, 6);
    const recentAvg = recent.reduce((a, b) => a + b.pm25, 0) / recent.length;
    const olderAvg = older.reduce((a, b) => a + b.pm25, 0) / older.length;
    return ((recentAvg - olderAvg) / olderAvg) * 100;
  }, [historicalData]);
  
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
            Analytics
          </h1>
          <p className="text-slate-500 dark:text-slate-400">
            Analyze air quality trends and patterns
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
          
          {/* Sensor Select */}
          <select
            value={selectedSensor?.deviceId || ''}
            onChange={(e) => {
              const sensor = sensors.find(s => s.deviceId === e.target.value);
              setSelectedSensor(sensor);
            }}
            className="input"
          >
            {sensors.map(sensor => (
              <option key={sensor.deviceId} value={sensor.deviceId}>
                {sensor.deviceId}
              </option>
            ))}
          </select>
          
          <button className="btn btn-secondary">
            <Download className="w-4 h-4" />
            Export
          </button>
        </div>
      </div>
      
      {/* Stats Overview */}
      {stats && (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="glass-card p-5">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-slate-500">Avg PM2.5</p>
                <p className="text-3xl font-display font-bold text-slate-900 dark:text-white">
                  {formatNumber(stats.pm25.avg)}
                </p>
                <p className="text-xs text-slate-400">µg/m³</p>
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
      
      {/* Main Chart */}
      <ChartCard
        title="Air Quality Over Time"
        subtitle={`${selectedSensor?.deviceId || 'All Sensors'} - ${
          timeRange === '24h' ? 'Last 24 Hours' : 
          timeRange === '7d' ? 'Last 7 Days' : 'Last 30 Days'
        }`}
      >
        <TimeSeriesChart
          data={historicalData}
          metrics={['pm25', 'pm10']}
          height={300}
          showLegend={true}
        />
      </ChartCard>
      
      {/* Multi-metric Charts */}
      <MultiMetricChart data={historicalData} height={300} />
      
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
