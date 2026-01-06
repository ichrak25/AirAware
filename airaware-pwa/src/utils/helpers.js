/**
 * AirAware Utility Functions
 * FIXED: Proper timestamp handling
 */

// ============================================
// TIMESTAMP UTILITIES
// ============================================

/**
 * Convert any timestamp to JavaScript Date object
 */
export function toDate(timestamp) {
    if (!timestamp) return null;

    // Already a Date object
    if (timestamp instanceof Date) return timestamp;

    // String - ISO format like "2025-12-06T19:46:34.506592Z"
    if (typeof timestamp === 'string') {
        const date = new Date(timestamp);
        if (!isNaN(date.getTime())) return date;

        // Try parsing as a number string
        const num = parseFloat(timestamp);
        if (!isNaN(num)) {
            return new Date(num < 10000000000 ? num * 1000 : num);
        }
        return null;
    }

    // Number - Unix timestamp
    if (typeof timestamp === 'number') {
        // If less than 10 billion, it's seconds - convert to milliseconds
        if (timestamp < 10000000000) {
            return new Date(timestamp * 1000);
        }
        return new Date(timestamp);
    }

    return null;
}

/**
 * Format relative time (e.g., "5m ago", "2h ago")
 */
export function formatRelativeTime(timestamp) {
    const date = toDate(timestamp);

    // DEBUG: Log to see what's happening
    console.log('[formatRelativeTime] Input:', timestamp, '-> Date:', date);

    if (!date || isNaN(date.getTime())) {
        console.warn('[formatRelativeTime] Invalid date for:', timestamp);
        return 'Unknown';
    }

    const now = new Date();
    const diffMs = now - date;
    const diffSec = Math.floor(diffMs / 1000);
    const diffMin = Math.floor(diffSec / 60);
    const diffHour = Math.floor(diffMin / 60);
    const diffDay = Math.floor(diffHour / 24);

    if (diffSec < 0) return 'Just now';
    if (diffSec < 60) return 'Just now';
    if (diffMin < 60) return `${diffMin}m ago`;
    if (diffHour < 24) return `${diffHour}h ago`;
    if (diffDay < 7) return `${diffDay}d ago`;
    if (diffDay < 30) return `${diffDay}d ago`;

    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}

/**
 * Format timestamp as time
 */
export function formatTimestamp(timestamp, options = {}) {
    const date = toDate(timestamp);
    if (!date || isNaN(date.getTime())) return '—';

    return date.toLocaleTimeString(undefined, {
        hour: '2-digit',
        minute: '2-digit',
        ...options,
    });
}

/**
 * Format date
 */
export function formatDate(timestamp, options = {}) {
    const date = toDate(timestamp);
    if (!date || isNaN(date.getTime())) return '—';

    return date.toLocaleDateString(undefined, {
        month: 'short',
        day: 'numeric',
        ...options,
    });
}

/**
 * Format full date and time
 */
export function formatDateTime(timestamp) {
    const date = toDate(timestamp);
    if (!date || isNaN(date.getTime())) return '—';

    return date.toLocaleString(undefined, {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    });
}

// ============================================
// AQI CALCULATION
// ============================================

const PM25_BREAKPOINTS = [
    { cLow: 0.0, cHigh: 12.0, iLow: 0, iHigh: 50, category: 'Good', color: '#22c55e' },
    { cLow: 12.1, cHigh: 35.4, iLow: 51, iHigh: 100, category: 'Moderate', color: '#eab308' },
    { cLow: 35.5, cHigh: 55.4, iLow: 101, iHigh: 150, category: 'Unhealthy for Sensitive Groups', color: '#f97316' },
    { cLow: 55.5, cHigh: 150.4, iLow: 151, iHigh: 200, category: 'Unhealthy', color: '#ef4444' },
    { cLow: 150.5, cHigh: 250.4, iLow: 201, iHigh: 300, category: 'Very Unhealthy', color: '#dc2626' },
    { cLow: 250.5, cHigh: 500.4, iLow: 301, iHigh: 500, category: 'Hazardous', color: '#7c2d12' },
];

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

    return { aqi: 500, category: 'Hazardous', color: '#7c2d12' };
}

export function getCO2Quality(co2) {
    if (co2 <= 400) return { category: 'Excellent', color: '#22c55e', description: 'Outdoor air quality' };
    if (co2 <= 600) return { category: 'Good', color: '#84cc16', description: 'Acceptable indoor air' };
    if (co2 <= 1000) return { category: 'Moderate', color: '#eab308', description: 'Complaints of drowsiness' };
    if (co2 <= 2000) return { category: 'Poor', color: '#f97316', description: 'Headaches, sleepiness' };
    if (co2 <= 5000) return { category: 'Unhealthy', color: '#ef4444', description: 'Workplace exposure limit' };
    return { category: 'Hazardous', color: '#7c2d12', description: 'Dangerous levels' };
}

export function getAQIClass(aqi) {
    if (aqi <= 50) return 'aqi-good';
    if (aqi <= 100) return 'aqi-moderate';
    if (aqi <= 150) return 'aqi-unhealthy-sensitive';
    if (aqi <= 200) return 'aqi-unhealthy';
    if (aqi <= 300) return 'aqi-very-unhealthy';
    return 'aqi-hazardous';
}

export function getHealthRecommendation(aqi) {
    if (aqi <= 50) return { level: 'Good', message: 'Air quality is satisfactory. Enjoy outdoor activities!', icon: '😊' };
    if (aqi <= 100) return { level: 'Moderate', message: 'Air quality is acceptable. Sensitive individuals should limit prolonged outdoor exposure.', icon: '😐' };
    if (aqi <= 150) return { level: 'Unhealthy for Sensitive Groups', message: 'Children, elderly, and those with respiratory conditions should reduce outdoor activities.', icon: '😷' };
    if (aqi <= 200) return { level: 'Unhealthy', message: 'Everyone may experience health effects. Limit outdoor activities.', icon: '🤢' };
    if (aqi <= 300) return { level: 'Very Unhealthy', message: 'Health alert! Everyone should avoid outdoor activities.', icon: '⚠️' };
    return { level: 'Hazardous', message: 'Emergency conditions! Stay indoors with air purification.', icon: '🚨' };
}

// ============================================
// FORMATTING UTILITIES
// ============================================

export function formatNumber(value, decimals = 1) {
    if (value === null || value === undefined) return '—';
    return Number(value).toFixed(decimals);
}

export function formatTemperature(temp, unit = 'C') {
    if (temp === null || temp === undefined) return '—';
    const value = unit === 'F' ? (temp * 9/5) + 32 : temp;
    return `${formatNumber(value, 1)}°${unit}`;
}

export function formatHumidity(humidity) {
    if (humidity === null || humidity === undefined) return '—';
    return `${formatNumber(humidity, 0)}%`;
}

export function formatCO2(co2) {
    if (co2 === null || co2 === undefined) return '—';
    return `${formatNumber(co2, 0)} ppm`;
}

export function formatPM(pm) {
    if (pm === null || pm === undefined) return '—';
    return `${formatNumber(pm, 1)} µg/m³`;
}

// ============================================
// SENSOR & ALERT UTILITIES
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
    return readings.map(reading => {
        const date = toDate(reading.timestamp);
        return {
            timestamp: date ? date.getTime() : 0,
            value: reading[field],
            formattedTime: formatTimestamp(reading.timestamp),
        };
    });
}

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
// VALIDATION & STORAGE
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

export const storage = {
    get: (key, defaultValue = null) => {
        try {
            const item = localStorage.getItem(key);
            return item ? JSON.parse(item) : defaultValue;
        } catch { return defaultValue; }
    },
    set: (key, value) => {
        try { localStorage.setItem(key, JSON.stringify(value)); } catch {}
    },
    remove: (key) => { localStorage.removeItem(key); },
};

export function isPWA() {
    return window.matchMedia('(display-mode: standalone)').matches || window.navigator.standalone === true;
}

export function canInstallPWA() {
    return 'BeforeInstallPromptEvent' in window;
}

export default {
    toDate,
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
    formatDateTime,
    getSensorStatus,
    getAlertSeverity,
    generateChartData,
    getChartColor,
    isValidReading,
    storage,
    isPWA,
    canInstallPWA,
};