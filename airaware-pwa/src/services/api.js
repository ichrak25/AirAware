/**
 * AirAware API Service
 * Handles all communication with the backend APIs
 */

const API_BASE = '/api/api';
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
  
  const response = await fetch(url, {
    ...options,
    headers: {
      ...defaultHeaders,
      ...options.headers,
    },
  });
  
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'An error occurred' }));
    throw new Error(error.message || error.error || `HTTP ${response.status}`);
  }
  
  return response.json();
}

// ============================================
// SENSORS API
// ============================================

export const sensorsAPI = {
  // Get all sensors
  getAll: () => fetchAPI(`${API_BASE}/sensors`),
  
  // Get sensor by ID
  getById: (id) => fetchAPI(`${API_BASE}/sensors/${id}`),
  
  // Get active sensors
  getActive: () => fetchAPI(`${API_BASE}/sensors/active`),
  
  // Get sensors overview
  getOverview: () => fetchAPI(`${API_BASE}/sensors/overview`),
  
  // Register new sensor
  create: (sensor) => fetchAPI(`${API_BASE}/sensors`, {
    method: 'POST',
    body: JSON.stringify(sensor),
  }),
  
  // Update sensor
  update: (id, sensor) => fetchAPI(`${API_BASE}/sensors/${id}`, {
    method: 'PUT',
    body: JSON.stringify(sensor),
  }),
  
  // Deactivate sensor
  deactivate: (id) => fetchAPI(`${API_BASE}/sensors/${id}/deactivate`, {
    method: 'PUT',
  }),
};

// ============================================
// READINGS API
// ============================================

export const readingsAPI = {
  // Get all readings
  getAll: () => fetchAPI(`${API_BASE}/readings`),
  
  // Get reading by ID
  getById: (id) => fetchAPI(`${API_BASE}/readings/${id}`),
  
  // Get readings by sensor
  getBySensor: (sensorId) => fetchAPI(`${API_BASE}/readings/sensor/${sensorId}`),
  
  // Get latest readings
  getLatest: (limit = 10) => fetchAPI(`${API_BASE}/readings?limit=${limit}`),
  
  // Create new reading
  create: (reading) => fetchAPI(`${API_BASE}/readings`, {
    method: 'POST',
    body: JSON.stringify(reading),
  }),
};

// ============================================
// ALERTS API
// ============================================

export const alertsAPI = {
  // Get all alerts
  getAll: () => fetchAPI(`${API_BASE}/alerts`),
  
  // Get alert by ID
  getById: (id) => fetchAPI(`${API_BASE}/alerts/${id}`),
  
  // Get unresolved alerts
  getUnresolved: () => fetchAPI(`${API_BASE}/alerts/unresolved`),
  
  // Get alerts by severity
  getBySeverity: (severity) => fetchAPI(`${API_BASE}/alerts/severity/${severity}`),
  
  // Get alerts by sensor
  getBySensor: (sensorId) => fetchAPI(`${API_BASE}/alerts/sensor/${sensorId}`),
  
  // Resolve alert
  resolve: (id) => fetchAPI(`${API_BASE}/alerts/${id}/resolve`, {
    method: 'PUT',
  }),
  
  // Delete alert
  delete: (id) => fetchAPI(`${API_BASE}/alerts/${id}`, {
    method: 'DELETE',
  }),
};

// ============================================
// TENANTS API
// ============================================

export const tenantsAPI = {
  // Get all tenants
  getAll: () => fetchAPI(`${API_BASE}/tenants`),
  
  // Get tenant by ID
  getById: (id) => fetchAPI(`${API_BASE}/tenants/${id}`),
  
  // Create tenant
  create: (tenant) => fetchAPI(`${API_BASE}/tenants`, {
    method: 'POST',
    body: JSON.stringify(tenant),
  }),
};

// ============================================
// ML API (Predictions)
// ============================================

export const mlAPI = {
  // Get health status
  health: () => fetchAPI(`${ML_BASE}/health`),
  
  // Get single prediction
  predict: (reading) => fetchAPI(`${ML_BASE}/predict`, {
    method: 'POST',
    body: JSON.stringify(reading),
  }),
  
  // Get batch predictions
  predictBatch: (readings) => fetchAPI(`${ML_BASE}/predict/batch`, {
    method: 'POST',
    body: JSON.stringify(readings),
  }),
  
  // Get model info
  getModelInfo: () => fetchAPI(`${ML_BASE}/models/info`),
};

// ============================================
// AUTH API
// ============================================

export const authAPI = {
  // Login (OAuth flow - redirect)
  login: () => {
    const clientId = 'airaware-web-client';
    const redirectUri = encodeURIComponent(`${window.location.origin}/callback`);
    const codeChallenge = 'E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM';
    
    window.location.href = `${IAM_BASE}/authorize?` +
      `client_id=${clientId}&` +
      `redirect_uri=${redirectUri}&` +
      `response_type=code&` +
      `code_challenge=${codeChallenge}&` +
      `code_challenge_method=S256&` +
      `scope=sensor:read%20sensor:write%20alert:read%20alert:write%20dashboard:view`;
  },
  
  // Exchange code for token
  exchangeCode: async (code) => {
    const response = await fetch(`${IAM_BASE}/oauth/token`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: new URLSearchParams({
        grant_type: 'authorization_code',
        code: code,
        code_verifier: 'dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk',
      }),
    });
    
    if (!response.ok) {
      throw new Error('Token exchange failed');
    }
    
    return response.json();
  },
  
  // Get user profile
  getProfile: () => fetchAPI(`${IAM_BASE}/identities/profile`),
  
  // Logout
  logout: () => {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    window.location.href = '/login';
  },
  
  // Check if authenticated
  isAuthenticated: () => {
    return !!localStorage.getItem('access_token');
  },
  
  // Get stored token
  getToken: () => localStorage.getItem('access_token'),
  
  // Store tokens
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
    {
      id: '1',
      deviceId: 'SENSOR_TUNIS_001',
      model: 'AirAware Pro v1',
      description: 'Downtown Tunis Office',
      status: 'ACTIVE',
      location: { latitude: 36.8065, longitude: 10.1815, city: 'Tunis', country: 'Tunisia' },
    },
    {
      id: '2',
      deviceId: 'SENSOR_TUNIS_002',
      model: 'AirAware Pro v1',
      description: 'Carthage Industrial Zone',
      status: 'ACTIVE',
      location: { latitude: 36.8500, longitude: 10.1658, city: 'Tunis', country: 'Tunisia' },
    },
    {
      id: '3',
      deviceId: 'SENSOR_TUNIS_003',
      model: 'AirAware Lite v2',
      description: 'La Marsa Residential',
      status: 'ACTIVE',
      location: { latitude: 36.7525, longitude: 10.2084, city: 'Tunis', country: 'Tunisia' },
    },
    {
      id: '4',
      deviceId: 'SENSOR_SFAX_001',
      model: 'AirAware Pro v1',
      description: 'Sfax Port Area',
      status: 'ACTIVE',
      location: { latitude: 34.7406, longitude: 10.7603, city: 'Sfax', country: 'Tunisia' },
    },
    {
      id: '5',
      deviceId: 'SENSOR_SOUSSE_001',
      model: 'AirAware Lite v2',
      description: 'Sousse City Center',
      status: 'OFFLINE',
      location: { latitude: 35.8256, longitude: 10.6369, city: 'Sousse', country: 'Tunisia' },
    },
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
      const timestamp = new Date(now - i * 60 * 60 * 1000);
      readings.push({
        id: Math.random().toString(36).substr(2, 9),
        sensorId,
        temperature: 20 + Math.sin(i / 4) * 5 + Math.random() * 2,
        humidity: 50 + Math.cos(i / 6) * 15 + Math.random() * 5,
        co2: 500 + Math.sin(i / 3) * 200 + Math.random() * 100,
        voc: 0.4 + Math.sin(i / 5) * 0.2 + Math.random() * 0.1,
        pm25: 15 + Math.sin(i / 4) * 10 + Math.random() * 5,
        pm10: 30 + Math.sin(i / 4) * 15 + Math.random() * 10,
        timestamp: timestamp.toISOString(),
      });
    }
    
    return readings;
  },
  
  alerts: [
    {
      id: '1',
      type: 'CO2_HIGH',
      severity: 'WARNING',
      message: 'CO2 levels elevated in Downtown Tunis Office',
      sensorId: 'SENSOR_TUNIS_001',
      triggeredAt: new Date(Date.now() - 30 * 60 * 1000).toISOString(),
      resolved: false,
    },
    {
      id: '2',
      type: 'PM25_HIGH',
      severity: 'CRITICAL',
      message: 'PM2.5 levels exceeding safe threshold in Carthage Industrial Zone',
      sensorId: 'SENSOR_TUNIS_002',
      triggeredAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
      resolved: false,
    },
    {
      id: '3',
      type: 'SENSOR_OFFLINE',
      severity: 'INFO',
      message: 'Sousse City Center sensor went offline',
      sensorId: 'SENSOR_SOUSSE_001',
      triggeredAt: new Date(Date.now() - 5 * 60 * 60 * 1000).toISOString(),
      resolved: true,
    },
  ],
};

export default {
  sensors: sensorsAPI,
  readings: readingsAPI,
  alerts: alertsAPI,
  tenants: tenantsAPI,
  ml: mlAPI,
  auth: authAPI,
  mock: mockData,
};
