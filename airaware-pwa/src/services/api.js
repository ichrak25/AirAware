/**
 * AirAware API Service
 * Handles all communication with the backend APIs
 *
 * FIXED: Corrected API paths to match Vite proxy configuration
 */

// FIXED: Changed from '/api/api' to '/api' to avoid double /api/api in URL
const API_BASE = '/api';
const IAM_BASE = '/iam/api';
const ML_BASE = '/ml';

// Helper function for API calls
async function fetchAPI(url, options = {}) {
    const token = localStorage.getItem('access_token');

    const defaultHeaders = {
        'Content-Type': 'application/json',
    };

    if (token) {
        defaultHeaders['Authorization'] = `Bearer ${token}`;
    }

    console.log(`[API] Fetching: ${url}`);

    try {
        const response = await fetch(url, {
            ...options,
            headers: {
                ...defaultHeaders,
                ...options.headers,
            },
        });

        console.log(`[API] Response: ${response.status} ${response.statusText}`);

        if (!response.ok) {
            const error = await response.json().catch(() => ({ message: 'An error occurred' }));
            console.error(`[API] Error:`, error);
            throw new Error(error.message || error.error || `HTTP ${response.status}`);
        }

        const data = await response.json();
        console.log(`[API] Data received:`, Array.isArray(data) ? `${data.length} items` : data);
        return data;

    } catch (error) {
        console.error(`[API] Fetch failed for ${url}:`, error);
        throw error;
    }
}

// ============================================
// SENSORS API
// ============================================

export const sensorsAPI = {
    getAll: () => fetchAPI(`${API_BASE}/sensors`),
    getById: (id) => fetchAPI(`${API_BASE}/sensors/${id}`),
    getActive: () => fetchAPI(`${API_BASE}/sensors/active`),
    getOverview: () => fetchAPI(`${API_BASE}/sensors/overview`),
    create: (sensor) => fetchAPI(`${API_BASE}/sensors`, {
        method: 'POST',
        body: JSON.stringify(sensor),
    }),
    update: (id, sensor) => fetchAPI(`${API_BASE}/sensors/${id}`, {
        method: 'PUT',
        body: JSON.stringify(sensor),
    }),
    deactivate: (id) => fetchAPI(`${API_BASE}/sensors/${id}/deactivate`, {
        method: 'PUT',
    }),
};

// ============================================
// READINGS API
// ============================================

export const readingsAPI = {
    getAll: () => fetchAPI(`${API_BASE}/readings`),
    getById: (id) => fetchAPI(`${API_BASE}/readings/${id}`),
    getBySensor: (sensorId) => fetchAPI(`${API_BASE}/readings/sensor/${sensorId}`),
    getLatest: (limit = 10) => fetchAPI(`${API_BASE}/readings?limit=${limit}`),
    create: (reading) => fetchAPI(`${API_BASE}/readings`, {
        method: 'POST',
        body: JSON.stringify(reading),
    }),
};

// ============================================
// ALERTS API
// ============================================

export const alertsAPI = {
    getAll: () => fetchAPI(`${API_BASE}/alerts`),
    getById: (id) => fetchAPI(`${API_BASE}/alerts/${id}`),
    getUnresolved: () => fetchAPI(`${API_BASE}/alerts/unresolved`),
    getBySeverity: (severity) => fetchAPI(`${API_BASE}/alerts/severity/${severity}`),
    getBySensor: (sensorId) => fetchAPI(`${API_BASE}/alerts/sensor/${sensorId}`),
    resolve: (id) => fetchAPI(`${API_BASE}/alerts/${id}/resolve`, { method: 'PUT' }),
    delete: (id) => fetchAPI(`${API_BASE}/alerts/${id}`, { method: 'DELETE' }),
};

// ============================================
// TENANTS API
// ============================================

export const tenantsAPI = {
    getAll: () => fetchAPI(`${API_BASE}/tenants`),
    getById: (id) => fetchAPI(`${API_BASE}/tenants/${id}`),
    create: (tenant) => fetchAPI(`${API_BASE}/tenants`, {
        method: 'POST',
        body: JSON.stringify(tenant),
    }),
};

// ============================================
// ML API (Predictions)
// ============================================

export const mlAPI = {
    health: () => fetchAPI(`${ML_BASE}/health`),
    predict: (reading) => fetchAPI(`${ML_BASE}/predict`, {
        method: 'POST',
        body: JSON.stringify(reading),
    }),
    predictBatch: (readings) => fetchAPI(`${ML_BASE}/predict/batch`, {
        method: 'POST',
        body: JSON.stringify(readings),
    }),
    getModelInfo: () => fetchAPI(`${ML_BASE}/models/info`),
};

// ============================================
// AUTH API
// ============================================

export const authAPI = {
    login: () => {
        const clientId = 'airaware-web-client';
        const redirectUri = encodeURIComponent(`${window.location.origin}/callback`);
        const codeChallenge = 'E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM';
        window.location.href = `${IAM_BASE}/authorize?client_id=${clientId}&redirect_uri=${redirectUri}&response_type=code&code_challenge=${codeChallenge}&code_challenge_method=S256&scope=sensor:read%20sensor:write%20alert:read%20alert:write%20dashboard:view`;
    },
    exchangeCode: async (code) => {
        const response = await fetch(`${IAM_BASE}/oauth/token`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({
                grant_type: 'authorization_code',
                code: code,
                code_verifier: 'dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk',
            }),
        });
        if (!response.ok) throw new Error('Token exchange failed');
        return response.json();
    },
    getProfile: () => fetchAPI(`${IAM_BASE}/identities/profile`),
    logout: () => {
        localStorage.removeItem('access_token');
        localStorage.removeItem('refresh_token');
        window.location.href = '/login';
    },
    isAuthenticated: () => !!localStorage.getItem('access_token'),
    getToken: () => localStorage.getItem('access_token'),
    storeTokens: (accessToken, refreshToken) => {
        localStorage.setItem('access_token', accessToken);
        localStorage.setItem('refresh_token', refreshToken);
    },
};

// ============================================
// MOCK DATA (for development/offline)
// ============================================

export const mockData = {
    sensors: [
        { id: '1', sensorId: 'SENSOR-TN-001', deviceId: 'SENSOR_TUNIS_001', name: 'Tunis Downtown', status: 'ACTIVE', location: { latitude: 36.8065, longitude: 10.1815, city: 'Tunis', country: 'Tunisia' } },
        { id: '2', sensorId: 'SENSOR-TN-002', deviceId: 'SENSOR_SFAX_001', name: 'Sfax Industrial', status: 'ACTIVE', location: { latitude: 34.7406, longitude: 10.7603, city: 'Sfax', country: 'Tunisia' } },
        { id: '3', sensorId: 'SENSOR-TN-003', deviceId: 'SENSOR_SOUSSE_001', name: 'Sousse City', status: 'ACTIVE', location: { latitude: 35.8256, longitude: 10.6369, city: 'Sousse', country: 'Tunisia' } },
        { id: '4', sensorId: 'SENSOR-TN-004', deviceId: 'SENSOR_BIZERTE_001', name: 'Bizerte Port', status: 'ACTIVE', location: { latitude: 37.2768, longitude: 9.8642, city: 'Bizerte', country: 'Tunisia' } },
        { id: '5', sensorId: 'SENSOR-TN-005', deviceId: 'SENSOR_GABES_001', name: 'Gabes Chemical', status: 'OFFLINE', location: { latitude: 33.8881, longitude: 10.0975, city: 'Gabes', country: 'Tunisia' } },
    ],
    generateReading: (sensorId) => ({
        id: Math.random().toString(36).substr(2, 9),
        sensorId,
        temperature: 20 + Math.random() * 15,
        humidity: 40 + Math.random() * 40,
        co2: 400 + Math.random() * 800,
        voc: 0.2 + Math.random() * 0.8,
        pm25: 5 + Math.random() * 30,
        pm10: 10 + Math.random() * 50,
        timestamp: new Date().toISOString(),
    }),
    generateReadingsHistory: (sensorId, hours = 24) => {
        const readings = [];
        const now = new Date();
        for (let i = hours; i >= 0; i--) {
            readings.push({
                id: Math.random().toString(36).substr(2, 9),
                sensorId,
                temperature: 20 + Math.sin(i / 4) * 5 + Math.random() * 2,
                humidity: 50 + Math.cos(i / 6) * 15 + Math.random() * 5,
                co2: 500 + Math.sin(i / 3) * 200 + Math.random() * 100,
                voc: 0.4 + Math.sin(i / 5) * 0.2 + Math.random() * 0.1,
                pm25: 15 + Math.sin(i / 4) * 10 + Math.random() * 5,
                pm10: 30 + Math.sin(i / 4) * 15 + Math.random() * 10,
                timestamp: new Date(now - i * 60 * 60 * 1000).toISOString(),
            });
        }
        return readings;
    },
    alerts: [
        { id: '1', type: 'CO2_HIGH', severity: 'WARNING', message: 'CO2 levels elevated', sensorId: 'SENSOR_TUNIS_001', triggeredAt: new Date(Date.now() - 30 * 60 * 1000).toISOString(), resolved: false },
        { id: '2', type: 'PM25_HIGH', severity: 'CRITICAL', message: 'PM2.5 exceeding threshold', sensorId: 'SENSOR_SFAX_001', triggeredAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(), resolved: false },
    ],
};

export default { sensors: sensorsAPI, readings: readingsAPI, alerts: alertsAPI, tenants: tenantsAPI, ml: mlAPI, auth: authAPI, mock: mockData };