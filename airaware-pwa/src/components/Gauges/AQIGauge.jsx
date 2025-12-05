import React, { useMemo } from 'react';
import { motion } from 'framer-motion';
import { calculateAQI, getHealthRecommendation } from '../../utils/helpers';

/**
 * AQI Gauge Component
 * Circular gauge displaying Air Quality Index with animated fill
 */
export default function AQIGauge({ 
  value, 
  size = 200, 
  strokeWidth = 12,
  showLabel = true,
  showRecommendation = true,
  animated = true,
}) {
  const { aqi, category, color } = useMemo(() => calculateAQI(value), [value]);
  const recommendation = useMemo(() => getHealthRecommendation(aqi), [aqi]);
  
  // SVG calculations
  const radius = (size - strokeWidth) / 2;
  const circumference = radius * 2 * Math.PI;
  const progress = Math.min(aqi / 500, 1); // Max AQI is 500
  const offset = circumference - (progress * circumference);
  
  // Center point
  const center = size / 2;
  
  return (
    <div className="flex flex-col items-center gap-4">
      <div className="relative" style={{ width: size, height: size }}>
        {/* Background Ring */}
        <svg
          width={size}
          height={size}
          className="transform -rotate-90"
        >
          {/* Background track */}
          <circle
            cx={center}
            cy={center}
            r={radius}
            fill="none"
            stroke="currentColor"
            strokeWidth={strokeWidth}
            className="text-slate-200 dark:text-slate-700"
          />
          
          {/* Progress arc */}
          <motion.circle
            cx={center}
            cy={center}
            r={radius}
            fill="none"
            stroke={color}
            strokeWidth={strokeWidth}
            strokeLinecap="round"
            strokeDasharray={circumference}
            initial={animated ? { strokeDashoffset: circumference } : { strokeDashoffset: offset }}
            animate={{ strokeDashoffset: offset }}
            transition={{ duration: 1.5, ease: "easeOut" }}
            style={{
              filter: `drop-shadow(0 0 8px ${color}40)`,
            }}
          />
          
          {/* Gradient glow effect */}
          <defs>
            <filter id="glow">
              <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
              <feMerge>
                <feMergeNode in="coloredBlur"/>
                <feMergeNode in="SourceGraphic"/>
              </feMerge>
            </filter>
          </defs>
        </svg>
        
        {/* Center content */}
        <div className="absolute inset-0 flex flex-col items-center justify-center">
          <motion.span
            className="text-5xl font-display font-bold"
            style={{ color }}
            initial={animated ? { scale: 0, opacity: 0 } : {}}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ delay: 0.5, duration: 0.5, type: "spring" }}
          >
            {aqi}
          </motion.span>
          <span className="text-sm font-medium text-slate-500 dark:text-slate-400 mt-1">
            AQI
          </span>
        </div>
        
        {/* Animated pulse ring */}
        {aqi > 100 && (
          <motion.div
            className="absolute inset-0 rounded-full"
            style={{
              border: `2px solid ${color}`,
              opacity: 0.3,
            }}
            animate={{
              scale: [1, 1.1, 1],
              opacity: [0.3, 0, 0.3],
            }}
            transition={{
              duration: 2,
              repeat: Infinity,
              ease: "easeInOut",
            }}
          />
        )}
      </div>
      
      {/* Category Label */}
      {showLabel && (
        <motion.div
          className="text-center"
          initial={animated ? { y: 20, opacity: 0 } : {}}
          animate={{ y: 0, opacity: 1 }}
          transition={{ delay: 0.8, duration: 0.5 }}
        >
          <div
            className="inline-flex items-center gap-2 px-4 py-2 rounded-full text-sm font-semibold"
            style={{
              backgroundColor: `${color}20`,
              color: color,
            }}
          >
            <span className="text-lg">{recommendation.icon}</span>
            <span>{category}</span>
          </div>
        </motion.div>
      )}
      
      {/* Health Recommendation */}
      {showRecommendation && (
        <motion.p
          className="text-sm text-slate-500 dark:text-slate-400 text-center max-w-xs"
          initial={animated ? { y: 20, opacity: 0 } : {}}
          animate={{ y: 0, opacity: 1 }}
          transition={{ delay: 1, duration: 0.5 }}
        >
          {recommendation.message}
        </motion.p>
      )}
    </div>
  );
}

/**
 * Mini AQI Badge - compact version for cards
 */
export function AQIBadge({ value, size = 'md' }) {
  const { aqi, category, color } = useMemo(() => calculateAQI(value), [value]);
  
  const sizeClasses = {
    sm: 'text-xs px-2 py-0.5',
    md: 'text-sm px-3 py-1',
    lg: 'text-base px-4 py-1.5',
  };
  
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full font-semibold ${sizeClasses[size]}`}
      style={{
        backgroundColor: `${color}20`,
        color: color,
      }}
    >
      <span className="font-bold">{aqi}</span>
      <span className="opacity-75">{category}</span>
    </span>
  );
}

/**
 * Linear AQI Bar - horizontal progress bar
 */
export function AQIBar({ value, height = 8 }) {
  const { aqi, color } = useMemo(() => calculateAQI(value), [value]);
  const progress = Math.min(aqi / 500, 1) * 100;
  
  return (
    <div className="w-full">
      <div 
        className="w-full rounded-full overflow-hidden bg-slate-200 dark:bg-slate-700"
        style={{ height }}
      >
        <motion.div
          className="h-full rounded-full"
          style={{ backgroundColor: color }}
          initial={{ width: 0 }}
          animate={{ width: `${progress}%` }}
          transition={{ duration: 1, ease: "easeOut" }}
        />
      </div>
      {/* Scale markers */}
      <div className="flex justify-between mt-1 text-xs text-slate-400">
        <span>0</span>
        <span>100</span>
        <span>200</span>
        <span>300</span>
        <span>500</span>
      </div>
    </div>
  );
}
