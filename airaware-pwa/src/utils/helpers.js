/**
 * AirAware Utility Functions
 * AQI calculations, formatting, and helper functions
 */

// ============================================
// AQI CALCULATION (EPA Standards)
// ============================================

const PM25_BREAKPOINTS = [
  { cLow: 0.0, cHigh: 12.0, iLow: 0, iHigh: 50, category: 'Good', color: '#22c55e' },
  { cLow: 12.1, cHigh: 35.4, iLow: 51, iHigh: 100, category: 'Moderate', color: '#eab308' },
  { cLow: 35.5, cHigh: 55.4, iLow: 101, iHigh: 150, category: 'Unhealthy for Sensitive Groups', color: '#f97316' },
  { cLow: 55.5, cHigh: 150.4, iLow: 151, iHigh: 200, category: 'Unhealthy', color: '#ef4444' },
  { cLow: 150.5, cHigh: 250.4, iLow: 201, iHigh: 300, category: 'Very Unhealthy', color: '#dc2626' },
  { cLow: 250.5, cHigh: 500.4, iLow: 301, iHigh: 500, category: 'Hazardous', color: '#7c2d12' },
];

const CO2_THRESHOLDS = [
  { max: 400, category: 'Excellent', color: '#22c55e', description: 'Outdoor air quality' },
  { max: 600, category: 'Good', color: '#84cc16', description: 'Acceptable indoor air' },
  { max: 1000, category: 'Moderate', color: '#eab308', description: 'Complaints of drowsiness' },
  { max: 2000, category: 'Poor', color: '#f97316', description: 'Headaches, sleepiness' },
  { max: 5000, category: 'Unhealthy', color: '#ef4444', description: 'Workplace exposure limit' },
  { max: Infinity, category: 'Hazardous', color: '#7c2d12', description: 'Dangerous levels' },
];

/**
 * Calculate AQI from PM2.5 concentration
 */
export function calculateAQI(pm25) {
  if (pm25 < 0) return { aqi: 0, category: 'Invalid', color: '#94a3b8' };
  
  for (const bp of PM25_BREAKPOINTS) {
    if (pm25 >= bp.cLow && pm25 <= bp.cHigh) {
      const aqi = Math.round(
        ((bp.iHigh - bp.iLow) / (bp.cHigh - bp.cLow)) * (pm25 - bp.cLow) + bp.iLow
      );
      return { aqi, category: bp.category, color: bp.color };
    }
  }
  
  // Beyond scale
  return { aqi: 500, category: 'Hazardous', color: '#7c2d12' };
}

/**
 * Get CO2 quality assessment
 */
export function getCO2Quality(co2) {
  for (const threshold of CO2_THRESHOLDS) {
    if (co2 <= threshold.max) {
      return {
        category: threshold.category,
        color: threshold.color,
        description: threshold.description,
      };
    }
  }
  return CO2_THRESHOLDS[CO2_THRESHOLDS.length - 1];
}

/**
 * Get AQI category class name
 */
export function getAQIClass(aqi) {
  if (aqi <= 50) return 'aqi-good';
  if (aqi <= 100) return 'aqi-moderate';
  if (aqi <= 150) return 'aqi-unhealthy-sensitive';
  if (aqi <= 200) return 'aqi-unhealthy';
  if (aqi <= 300) return 'aqi-very-unhealthy';
  return 'aqi-hazardous';
}

/**
 * Get health recommendation based on AQI
 */
export function getHealthRecommendation(aqi) {
  if (aqi <= 50) {
    return {
      level: 'Good',
      message: 'Air quality is satisfactory. Enjoy outdoor activities!',
      icon: '😊',
    };
  }
  if (aqi <= 100) {
    return {
      level: 'Moderate',
      message: 'Air quality is acceptable. Sensitive individuals should limit prolonged outdoor exposure.',
      icon: '😐',
    };
  }
  if (aqi <= 150) {
    return {
      level: 'Unhealthy for Sensitive Groups',
      message: 'Children, elderly, and those with respiratory conditions should reduce outdoor activities.',
      icon: '😷',
    };
  }
  if (aqi <= 200) {
    return {
      level: 'Unhealthy',
      message: 'Everyone may experience health effects. Limit outdoor activities.',
      icon: '🤢',
    };
  }
  if (aqi <= 300) {
    return {
      level: 'Very Unhealthy',
      message: 'Health alert! Everyone should avoid outdoor activities.',
      icon: '⚠️',
    };
  }
  return {
    level: 'Hazardous',
    message: 'Emergency conditions! Stay indoors with air purification.',
    icon: '🚨',
  };
}

// ============================================
// FORMATTING UTILITIES
// ============================================

/**
 * Format number with specified decimal places
 */
export function formatNumber(value, decimals = 1) {
  if (value === null || value === undefined) return '—';
  return Number(value).toFixed(decimals);
}

/**
 * Format temperature
 */
export function formatTemperature(temp, unit = 'C') {
  if (temp === null || temp === undefined) return '—';
  const value = unit === 'F' ? (temp * 9/5) + 32 : temp;
  return `${formatNumber(value, 1)}°${unit}`;
}

/**
 * Format humidity
 */
export function formatHumidity(humidity) {
  if (humidity === null || humidity === undefined) return '—';
  return `${formatNumber(humidity, 0)}%`;
}

/**
 * Format CO2
 */
export function formatCO2(co2) {
  if (co2 === null || co2 === undefined) return '—';
  return `${formatNumber(co2, 0)} ppm`;
}

/**
 * Format PM2.5/PM10
 */
export function formatPM(pm) {
  if (pm === null || pm === undefined) return '—';
  return `${formatNumber(pm, 1)} µg/m³`;
}

/**
 * Format relative time
 */
export function formatRelativeTime(date) {
  const now = new Date();
  const then = new Date(date);
  const diffMs = now - then;
  const diffSec = Math.floor(diffMs / 1000);
  const diffMin = Math.floor(diffSec / 60);
  const diffHour = Math.floor(diffMin / 60);
  const diffDay = Math.floor(diffHour / 24);
  
  if (diffSec < 60) return 'Just now';
  if (diffMin < 60) return `${diffMin}m ago`;
  if (diffHour < 24) return `${diffHour}h ago`;
  if (diffDay < 7) return `${diffDay}d ago`;
  
  return then.toLocaleDateString();
}

/**
 * Format timestamp
 */
export function formatTimestamp(date, options = {}) {
  const d = new Date(date);
  const defaultOptions = {
    hour: '2-digit',
    minute: '2-digit',
    ...options,
  };
  return d.toLocaleTimeString(undefined, defaultOptions);
}

/**
 * Format date
 */
export function formatDate(date, options = {}) {
  const d = new Date(date);
  const defaultOptions = {
    month: 'short',
    day: 'numeric',
    ...options,
  };
  return d.toLocaleDateString(undefined, defaultOptions);
}

// ============================================
// SENSOR STATUS UTILITIES
// ============================================

export const SENSOR_STATUS = {
  ACTIVE: { label: 'Active', color: '#22c55e', bgClass: 'bg-green-100 dark:bg-green-900/30' },
  INACTIVE: { label: 'Inactive', color: '#94a3b8', bgClass: 'bg-slate-100 dark:bg-slate-800' },
  OFFLINE: { label: 'Offline', color: '#ef4444', bgClass: 'bg-red-100 dark:bg-red-900/30' },
  MAINTENANCE: { label: 'Maintenance', color: '#f97316', bgClass: 'bg-orange-100 dark:bg-orange-900/30' },
};

export function getSensorStatus(status) {
  return SENSOR_STATUS[status] || SENSOR_STATUS.INACTIVE;
}

// ============================================
// ALERT UTILITIES
// ============================================

export const ALERT_SEVERITY = {
  INFO: { label: 'Info', color: '#3b82f6', bgClass: 'alert-info', icon: 'Info' },
  WARNING: { label: 'Warning', color: '#eab308', bgClass: 'alert-warning', icon: 'AlertTriangle' },
  CRITICAL: { label: 'Critical', color: '#ef4444', bgClass: 'alert-critical', icon: 'AlertOctagon' },
  DANGER: { label: 'Danger', color: '#dc2626', bgClass: 'alert-critical', icon: 'XOctagon' },
};

export function getAlertSeverity(severity) {
  return ALERT_SEVERITY[severity] || ALERT_SEVERITY.INFO;
}

// ============================================
// CHART UTILITIES
// ============================================

export function generateChartData(readings, field) {
  return readings.map(reading => ({
    timestamp: new Date(reading.timestamp).getTime(),
    value: reading[field],
    formattedTime: formatTimestamp(reading.timestamp),
  }));
}

/**
 * Get color for chart based on value and field
 */
export function getChartColor(field) {
  const colors = {
    temperature: '#ef4444',
    humidity: '#3b82f6',
    co2: '#8b5cf6',
    voc: '#f97316',
    pm25: '#22c55e',
    pm10: '#06b6d4',
  };
  return colors[field] || '#94a3b8';
}

// ============================================
// VALIDATION UTILITIES
// ============================================

export const VALID_RANGES = {
  temperature: { min: -50, max: 70 },
  humidity: { min: 0, max: 100 },
  co2: { min: 0, max: 10000 },
  voc: { min: 0, max: 10 },
  pm25: { min: 0, max: 1000 },
  pm10: { min: 0, max: 1000 },
};

export function isValidReading(field, value) {
  const range = VALID_RANGES[field];
  if (!range) return true;
  return value >= range.min && value <= range.max;
}

// ============================================
// LOCAL STORAGE UTILITIES
// ============================================

export const storage = {
  get: (key, defaultValue = null) => {
    try {
      const item = localStorage.getItem(key);
      return item ? JSON.parse(item) : defaultValue;
    } catch {
      return defaultValue;
    }
  },
  
  set: (key, value) => {
    try {
      localStorage.setItem(key, JSON.stringify(value));
    } catch {
      // Storage full or unavailable
    }
  },
  
  remove: (key) => {
    localStorage.removeItem(key);
  },
};

// ============================================
// PWA UTILITIES
// ============================================

export function isPWA() {
  return window.matchMedia('(display-mode: standalone)').matches ||
         window.navigator.standalone === true;
}

export function canInstallPWA() {
  return 'BeforeInstallPromptEvent' in window;
}

export default {
  calculateAQI,
  getCO2Quality,
  getAQIClass,
  getHealthRecommendation,
  formatNumber,
  formatTemperature,
  formatHumidity,
  formatCO2,
  formatPM,
  formatRelativeTime,
  formatTimestamp,
  formatDate,
  getSensorStatus,
  getAlertSeverity,
  generateChartData,
  getChartColor,
  isValidReading,
  storage,
  isPWA,
  canInstallPWA,
};
