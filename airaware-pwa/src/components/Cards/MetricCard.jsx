import React from 'react';
import { motion } from 'framer-motion';
import { 
  Thermometer, 
  Droplets, 
  Wind, 
  CloudFog,
  Gauge,
  TrendingUp,
  TrendingDown,
  Minus
} from 'lucide-react';
import { formatNumber } from '../../utils/helpers';

// Metric configuration
const METRIC_CONFIG = {
  temperature: {
    icon: Thermometer,
    label: 'Temperature',
    unit: '°C',
    color: '#ef4444',
    bgColor: 'bg-red-50 dark:bg-red-900/20',
    iconColor: 'text-red-500',
  },
  humidity: {
    icon: Droplets,
    label: 'Humidity',
    unit: '%',
    color: '#3b82f6',
    bgColor: 'bg-blue-50 dark:bg-blue-900/20',
    iconColor: 'text-blue-500',
  },
  co2: {
    icon: Wind,
    label: 'CO₂',
    unit: 'ppm',
    color: '#8b5cf6',
    bgColor: 'bg-violet-50 dark:bg-violet-900/20',
    iconColor: 'text-violet-500',
  },
  voc: {
    icon: CloudFog,
    label: 'VOC',
    unit: 'mg/m³',
    color: '#f97316',
    bgColor: 'bg-orange-50 dark:bg-orange-900/20',
    iconColor: 'text-orange-500',
  },
  pm25: {
    icon: Gauge,
    label: 'PM2.5',
    unit: 'µg/m³',
    color: '#22c55e',
    bgColor: 'bg-green-50 dark:bg-green-900/20',
    iconColor: 'text-green-500',
  },
  pm10: {
    icon: Gauge,
    label: 'PM10',
    unit: 'µg/m³',
    color: '#06b6d4',
    bgColor: 'bg-cyan-50 dark:bg-cyan-900/20',
    iconColor: 'text-cyan-500',
  },
};

/**
 * Metric Card Component
 * Displays a single metric with icon, value, and optional trend
 */
export default function MetricCard({ 
  type, 
  value, 
  previousValue,
  showTrend = true,
  size = 'md',
  animated = true,
  onClick,
}) {
  const config = METRIC_CONFIG[type] || {};
  const Icon = config.icon || Gauge;
  
  // Calculate trend
  const trend = previousValue !== undefined ? value - previousValue : 0;
  const trendPercent = previousValue ? ((trend / previousValue) * 100).toFixed(1) : 0;
  
  const TrendIcon = trend > 0 ? TrendingUp : trend < 0 ? TrendingDown : Minus;
  const trendColor = trend > 0 
    ? (type === 'humidity' ? 'text-green-500' : 'text-red-500')
    : trend < 0 
    ? (type === 'humidity' ? 'text-red-500' : 'text-green-500')
    : 'text-slate-400';
  
  const sizeClasses = {
    sm: {
      card: 'p-3',
      icon: 'w-8 h-8',
      iconInner: 'w-4 h-4',
      value: 'text-xl',
      label: 'text-xs',
    },
    md: {
      card: 'p-5',
      icon: 'w-12 h-12',
      iconInner: 'w-6 h-6',
      value: 'text-3xl',
      label: 'text-sm',
    },
    lg: {
      card: 'p-6',
      icon: 'w-14 h-14',
      iconInner: 'w-7 h-7',
      value: 'text-4xl',
      label: 'text-base',
    },
  };
  
  const classes = sizeClasses[size];
  
  const CardContent = (
    <>
      {/* Header with icon and label */}
      <div className="flex items-center justify-between mb-3">
        <div className={`${classes.icon} rounded-xl ${config.bgColor} flex items-center justify-center`}>
          <Icon className={`${classes.iconInner} ${config.iconColor}`} />
        </div>
        
        {showTrend && previousValue !== undefined && (
          <div className={`flex items-center gap-1 ${trendColor}`}>
            <TrendIcon className="w-4 h-4" />
            <span className="text-xs font-medium">
              {Math.abs(trendPercent)}%
            </span>
          </div>
        )}
      </div>
      
      {/* Value */}
      <div className="flex items-baseline gap-1">
        <motion.span
          className={`${classes.value} font-display font-bold text-slate-900 dark:text-white tabular-nums`}
          initial={animated ? { opacity: 0, y: 10 } : {}}
          animate={{ opacity: 1, y: 0 }}
          key={value}
        >
          {formatNumber(value, type === 'temperature' ? 1 : 0)}
        </motion.span>
        <span className="text-slate-400 dark:text-slate-500 text-lg">
          {config.unit}
        </span>
      </div>
      
      {/* Label */}
      <span className={`${classes.label} font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wider mt-1`}>
        {config.label}
      </span>
    </>
  );
  
  const cardClasses = `
    glass-card ${classes.card} flex flex-col
    ${onClick ? 'cursor-pointer hover:scale-[1.02] active:scale-[0.98]' : ''}
    transition-all duration-200
  `;
  
  if (animated) {
    return (
      <motion.div
        className={cardClasses}
        onClick={onClick}
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        whileHover={onClick ? { y: -4 } : {}}
        transition={{ duration: 0.3 }}
      >
        {CardContent}
      </motion.div>
    );
  }
  
  return (
    <div className={cardClasses} onClick={onClick}>
      {CardContent}
    </div>
  );
}

/**
 * Metric Grid - displays all metrics in a responsive grid
 */
export function MetricGrid({ reading, previousReading, size = 'md' }) {
  const metrics = ['temperature', 'humidity', 'co2', 'voc', 'pm25', 'pm10'];
  
  return (
    <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
      {metrics.map((metric, index) => (
        <motion.div
          key={metric}
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: index * 0.1 }}
        >
          <MetricCard
            type={metric}
            value={reading?.[metric]}
            previousValue={previousReading?.[metric]}
            size={size}
          />
        </motion.div>
      ))}
    </div>
  );
}

/**
 * Compact Metric Row - inline display
 */
export function MetricRow({ reading }) {
  return (
    <div className="flex flex-wrap gap-4 text-sm">
      <div className="flex items-center gap-1.5">
        <Thermometer className="w-4 h-4 text-red-500" />
        <span className="font-medium">{formatNumber(reading?.temperature, 1)}°C</span>
      </div>
      <div className="flex items-center gap-1.5">
        <Droplets className="w-4 h-4 text-blue-500" />
        <span className="font-medium">{formatNumber(reading?.humidity, 0)}%</span>
      </div>
      <div className="flex items-center gap-1.5">
        <Wind className="w-4 h-4 text-violet-500" />
        <span className="font-medium">{formatNumber(reading?.co2, 0)} ppm</span>
      </div>
      <div className="flex items-center gap-1.5">
        <Gauge className="w-4 h-4 text-green-500" />
        <span className="font-medium">{formatNumber(reading?.pm25, 1)} µg/m³</span>
      </div>
    </div>
  );
}
