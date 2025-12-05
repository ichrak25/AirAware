import React, { useMemo } from 'react';
import {
  LineChart,
  Line,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts';
import { motion } from 'framer-motion';
import { formatTimestamp, formatNumber, getChartColor } from '../../utils/helpers';

/**
 * Custom Tooltip Component
 */
function CustomTooltip({ active, payload, label }) {
  if (!active || !payload || !payload.length) return null;
  
  return (
    <div className="glass-card p-3 shadow-lg border border-slate-200 dark:border-slate-700">
      <p className="text-xs text-slate-500 mb-2">
        {formatTimestamp(label, { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
      </p>
      {payload.map((entry, index) => (
        <div key={index} className="flex items-center gap-2">
          <div
            className="w-2 h-2 rounded-full"
            style={{ backgroundColor: entry.color }}
          />
          <span className="text-sm font-medium text-slate-700 dark:text-slate-200">
            {entry.name}: {formatNumber(entry.value, 1)} {entry.unit || ''}
          </span>
        </div>
      ))}
    </div>
  );
}

/**
 * Time Series Line Chart
 * Displays historical sensor data with multiple metrics
 */
export function TimeSeriesChart({
  data,
  metrics = ['pm25'],
  height = 300,
  showGrid = true,
  showLegend = true,
  animated = true,
}) {
  const chartData = useMemo(() => {
    if (!data || !data.length) return [];
    return data.map(reading => ({
      timestamp: new Date(reading.timestamp).getTime(),
      ...reading,
    }));
  }, [data]);
  
  const metricConfig = {
    temperature: { name: 'Temperature', unit: '°C', color: '#ef4444' },
    humidity: { name: 'Humidity', unit: '%', color: '#3b82f6' },
    co2: { name: 'CO₂', unit: 'ppm', color: '#8b5cf6' },
    voc: { name: 'VOC', unit: 'mg/m³', color: '#f97316' },
    pm25: { name: 'PM2.5', unit: 'µg/m³', color: '#22c55e' },
    pm10: { name: 'PM10', unit: 'µg/m³', color: '#06b6d4' },
  };
  
  if (!chartData.length) {
    return (
      <div 
        className="flex items-center justify-center text-slate-400"
        style={{ height }}
      >
        No data available
      </div>
    );
  }
  
  return (
    <motion.div
      initial={animated ? { opacity: 0 } : {}}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.5 }}
    >
      <ResponsiveContainer width="100%" height={height}>
        <LineChart data={chartData} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
          {showGrid && (
            <CartesianGrid 
              strokeDasharray="3 3" 
              stroke="rgba(148, 163, 184, 0.2)"
              vertical={false}
            />
          )}
          <XAxis
            dataKey="timestamp"
            tickFormatter={(ts) => formatTimestamp(ts)}
            stroke="#94a3b8"
            fontSize={12}
            tickLine={false}
            axisLine={false}
          />
          <YAxis
            stroke="#94a3b8"
            fontSize={12}
            tickLine={false}
            axisLine={false}
            width={50}
          />
          <Tooltip content={<CustomTooltip />} />
          {showLegend && (
            <Legend 
              wrapperStyle={{ fontSize: '12px' }}
              iconType="circle"
            />
          )}
          {metrics.map(metric => {
            const config = metricConfig[metric] || { name: metric, color: '#94a3b8' };
            return (
              <Line
                key={metric}
                type="monotone"
                dataKey={metric}
                name={config.name}
                stroke={config.color}
                strokeWidth={2}
                dot={false}
                activeDot={{ r: 4, strokeWidth: 0 }}
                unit={config.unit}
              />
            );
          })}
        </LineChart>
      </ResponsiveContainer>
    </motion.div>
  );
}

/**
 * Area Chart - filled variant
 */
export function AreaChartComponent({
  data,
  metric = 'pm25',
  height = 200,
  gradient = true,
}) {
  const chartData = useMemo(() => {
    if (!data || !data.length) return [];
    return data.map(reading => ({
      timestamp: new Date(reading.timestamp).getTime(),
      value: reading[metric],
    }));
  }, [data, metric]);
  
  const color = getChartColor(metric);
  const gradientId = `gradient-${metric}`;
  
  return (
    <ResponsiveContainer width="100%" height={height}>
      <AreaChart data={chartData} margin={{ top: 0, right: 0, left: 0, bottom: 0 }}>
        {gradient && (
          <defs>
            <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor={color} stopOpacity={0.3} />
              <stop offset="95%" stopColor={color} stopOpacity={0} />
            </linearGradient>
          </defs>
        )}
        <XAxis
          dataKey="timestamp"
          tickFormatter={(ts) => formatTimestamp(ts)}
          stroke="#94a3b8"
          fontSize={10}
          tickLine={false}
          axisLine={false}
          hide
        />
        <YAxis hide />
        <Tooltip content={<CustomTooltip />} />
        <Area
          type="monotone"
          dataKey="value"
          stroke={color}
          strokeWidth={2}
          fill={gradient ? `url(#${gradientId})` : color}
          fillOpacity={gradient ? 1 : 0.1}
        />
      </AreaChart>
    </ResponsiveContainer>
  );
}

/**
 * Sparkline - minimal inline chart
 */
export function Sparkline({ data, metric, width = 100, height = 30, color }) {
  const chartData = useMemo(() => {
    if (!data || !data.length) return [];
    return data.slice(-20).map(reading => ({
      value: reading[metric],
    }));
  }, [data, metric]);
  
  const strokeColor = color || getChartColor(metric);
  
  if (!chartData.length) return null;
  
  return (
    <ResponsiveContainer width={width} height={height}>
      <LineChart data={chartData}>
        <Line
          type="monotone"
          dataKey="value"
          stroke={strokeColor}
          strokeWidth={1.5}
          dot={false}
        />
      </LineChart>
    </ResponsiveContainer>
  );
}

/**
 * Multi-Metric Chart - shows all metrics
 */
export function MultiMetricChart({ data, height = 400 }) {
  return (
    <div className="space-y-6">
      {/* Air Quality Metrics */}
      <div className="glass-card p-5">
        <h3 className="text-sm font-semibold text-slate-700 dark:text-slate-200 mb-4">
          Air Quality
        </h3>
        <TimeSeriesChart
          data={data}
          metrics={['pm25', 'pm10', 'co2']}
          height={height / 2}
        />
      </div>
      
      {/* Environmental Metrics */}
      <div className="glass-card p-5">
        <h3 className="text-sm font-semibold text-slate-700 dark:text-slate-200 mb-4">
          Environmental
        </h3>
        <TimeSeriesChart
          data={data}
          metrics={['temperature', 'humidity', 'voc']}
          height={height / 2}
        />
      </div>
    </div>
  );
}

/**
 * Chart Card - chart with header
 */
export function ChartCard({
  title,
  subtitle,
  data,
  metric,
  height = 200,
  children,
}) {
  return (
    <div className="glass-card p-5">
      <div className="flex items-center justify-between mb-4">
        <div>
          <h3 className="font-semibold text-slate-900 dark:text-white">
            {title}
          </h3>
          {subtitle && (
            <p className="text-sm text-slate-500">{subtitle}</p>
          )}
        </div>
      </div>
      {children || (
        <AreaChartComponent data={data} metric={metric} height={height} />
      )}
    </div>
  );
}

export default TimeSeriesChart;
