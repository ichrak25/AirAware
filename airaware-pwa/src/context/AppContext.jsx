import React, { createContext, useContext, useReducer, useEffect } from 'react';
import { storage } from '../utils/helpers';

// ============================================
// INITIAL STATE
// ============================================

const initialState = {
  // Theme
  theme: 'light',
  
  // User
  user: null,
  isAuthenticated: false,
  
  // Data
  sensors: [],
  readings: {},
  alerts: [],
  
  // UI State
  selectedSensor: null,
  sidebarOpen: false,
  
  // Loading States
  loading: {
    sensors: false,
    readings: false,
    alerts: false,
  },
  
  // Errors
  errors: {},
  
  // Settings
  settings: {
    temperatureUnit: 'C',
    refreshInterval: 30000, // 30 seconds
    notifications: true,
    soundAlerts: false,
  },
};

// ============================================
// ACTION TYPES
// ============================================

const ActionTypes = {
  // Theme
  SET_THEME: 'SET_THEME',
  TOGGLE_THEME: 'TOGGLE_THEME',
  
  // Auth
  SET_USER: 'SET_USER',
  LOGOUT: 'LOGOUT',
  
  // Sensors
  SET_SENSORS: 'SET_SENSORS',
  UPDATE_SENSOR: 'UPDATE_SENSOR',
  SET_SELECTED_SENSOR: 'SET_SELECTED_SENSOR',
  
  // Readings
  SET_READINGS: 'SET_READINGS',
  ADD_READING: 'ADD_READING',
  
  // Alerts
  SET_ALERTS: 'SET_ALERTS',
  ADD_ALERT: 'ADD_ALERT',
  RESOLVE_ALERT: 'RESOLVE_ALERT',
  
  // UI
  TOGGLE_SIDEBAR: 'TOGGLE_SIDEBAR',
  SET_SIDEBAR: 'SET_SIDEBAR',
  
  // Loading
  SET_LOADING: 'SET_LOADING',
  
  // Errors
  SET_ERROR: 'SET_ERROR',
  CLEAR_ERROR: 'CLEAR_ERROR',
  
  // Settings
  UPDATE_SETTINGS: 'UPDATE_SETTINGS',
};

// ============================================
// REDUCER
// ============================================

function appReducer(state, action) {
  switch (action.type) {
    // Theme
    case ActionTypes.SET_THEME:
      return { ...state, theme: action.payload };
    
    case ActionTypes.TOGGLE_THEME:
      return { ...state, theme: state.theme === 'light' ? 'dark' : 'light' };
    
    // Auth
    case ActionTypes.SET_USER:
      return { ...state, user: action.payload, isAuthenticated: !!action.payload };
    
    case ActionTypes.LOGOUT:
      return { ...state, user: null, isAuthenticated: false };
    
    // Sensors
    case ActionTypes.SET_SENSORS:
      return { ...state, sensors: action.payload };
    
    case ActionTypes.UPDATE_SENSOR:
      return {
        ...state,
        sensors: state.sensors.map(s =>
          s.id === action.payload.id ? { ...s, ...action.payload } : s
        ),
      };
    
    case ActionTypes.SET_SELECTED_SENSOR:
      return { ...state, selectedSensor: action.payload };
    
    // Readings
    case ActionTypes.SET_READINGS:
      return {
        ...state,
        readings: {
          ...state.readings,
          [action.payload.sensorId]: action.payload.readings,
        },
      };
    
    case ActionTypes.ADD_READING:
      const sensorId = action.payload.sensorId;
      const currentReadings = state.readings[sensorId] || [];
      return {
        ...state,
        readings: {
          ...state.readings,
          [sensorId]: [...currentReadings, action.payload].slice(-100), // Keep last 100
        },
      };
    
    // Alerts
    case ActionTypes.SET_ALERTS:
      return { ...state, alerts: action.payload };
    
    case ActionTypes.ADD_ALERT:
      return { ...state, alerts: [action.payload, ...state.alerts] };
    
    case ActionTypes.RESOLVE_ALERT:
      return {
        ...state,
        alerts: state.alerts.map(a =>
          a.id === action.payload ? { ...a, resolved: true } : a
        ),
      };
    
    // UI
    case ActionTypes.TOGGLE_SIDEBAR:
      return { ...state, sidebarOpen: !state.sidebarOpen };
    
    case ActionTypes.SET_SIDEBAR:
      return { ...state, sidebarOpen: action.payload };
    
    // Loading
    case ActionTypes.SET_LOADING:
      return {
        ...state,
        loading: { ...state.loading, [action.payload.key]: action.payload.value },
      };
    
    // Errors
    case ActionTypes.SET_ERROR:
      return {
        ...state,
        errors: { ...state.errors, [action.payload.key]: action.payload.error },
      };
    
    case ActionTypes.CLEAR_ERROR:
      const { [action.payload]: _, ...remainingErrors } = state.errors;
      return { ...state, errors: remainingErrors };
    
    // Settings
    case ActionTypes.UPDATE_SETTINGS:
      return {
        ...state,
        settings: { ...state.settings, ...action.payload },
      };
    
    default:
      return state;
  }
}

// ============================================
// CONTEXT
// ============================================

const AppContext = createContext(null);

export function AppProvider({ children }) {
  // Load initial state from storage
  const loadInitialState = () => {
    const savedTheme = storage.get('theme', 'light');
    const savedSettings = storage.get('settings', initialState.settings);
    
    return {
      ...initialState,
      theme: savedTheme,
      settings: { ...initialState.settings, ...savedSettings },
    };
  };
  
  const [state, dispatch] = useReducer(appReducer, null, loadInitialState);
  
  // Persist theme to storage and apply to document
  useEffect(() => {
    storage.set('theme', state.theme);
    
    if (state.theme === 'dark') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [state.theme]);
  
  // Persist settings to storage
  useEffect(() => {
    storage.set('settings', state.settings);
  }, [state.settings]);
  
  // Check system preference for theme
  useEffect(() => {
    const savedTheme = storage.get('theme');
    if (!savedTheme) {
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      dispatch({ type: ActionTypes.SET_THEME, payload: prefersDark ? 'dark' : 'light' });
    }
  }, []);
  
  // Action creators
  const actions = {
    // Theme
    setTheme: (theme) => dispatch({ type: ActionTypes.SET_THEME, payload: theme }),
    toggleTheme: () => dispatch({ type: ActionTypes.TOGGLE_THEME }),
    
    // Auth
    setUser: (user) => dispatch({ type: ActionTypes.SET_USER, payload: user }),
    logout: () => dispatch({ type: ActionTypes.LOGOUT }),
    
    // Sensors
    setSensors: (sensors) => dispatch({ type: ActionTypes.SET_SENSORS, payload: sensors }),
    updateSensor: (sensor) => dispatch({ type: ActionTypes.UPDATE_SENSOR, payload: sensor }),
    setSelectedSensor: (sensor) => dispatch({ type: ActionTypes.SET_SELECTED_SENSOR, payload: sensor }),
    
    // Readings
    setReadings: (sensorId, readings) =>
      dispatch({ type: ActionTypes.SET_READINGS, payload: { sensorId, readings } }),
    addReading: (reading) => dispatch({ type: ActionTypes.ADD_READING, payload: reading }),
    
    // Alerts
    setAlerts: (alerts) => dispatch({ type: ActionTypes.SET_ALERTS, payload: alerts }),
    addAlert: (alert) => dispatch({ type: ActionTypes.ADD_ALERT, payload: alert }),
    resolveAlert: (id) => dispatch({ type: ActionTypes.RESOLVE_ALERT, payload: id }),
    
    // UI
    toggleSidebar: () => dispatch({ type: ActionTypes.TOGGLE_SIDEBAR }),
    setSidebar: (open) => dispatch({ type: ActionTypes.SET_SIDEBAR, payload: open }),
    
    // Loading
    setLoading: (key, value) =>
      dispatch({ type: ActionTypes.SET_LOADING, payload: { key, value } }),
    
    // Errors
    setError: (key, error) =>
      dispatch({ type: ActionTypes.SET_ERROR, payload: { key, error } }),
    clearError: (key) => dispatch({ type: ActionTypes.CLEAR_ERROR, payload: key }),
    
    // Settings
    updateSettings: (settings) =>
      dispatch({ type: ActionTypes.UPDATE_SETTINGS, payload: settings }),
  };
  
  return (
    <AppContext.Provider value={{ state, actions }}>
      {children}
    </AppContext.Provider>
  );
}

// Custom hook to use the context
export function useApp() {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useApp must be used within an AppProvider');
  }
  return context;
}

// Specific hooks for common use cases
export function useTheme() {
  const { state, actions } = useApp();
  return {
    theme: state.theme,
    isDark: state.theme === 'dark',
    setTheme: actions.setTheme,
    toggleTheme: actions.toggleTheme,
  };
}

export function useSensors() {
  const { state, actions } = useApp();
  return {
    sensors: state.sensors,
    selectedSensor: state.selectedSensor,
    loading: state.loading.sensors,
    setSensors: actions.setSensors,
    setSelectedSensor: actions.setSelectedSensor,
    updateSensor: actions.updateSensor,
  };
}

export function useAlerts() {
  const { state, actions } = useApp();
  return {
    alerts: state.alerts,
    unresolvedAlerts: state.alerts.filter(a => !a.resolved),
    loading: state.loading.alerts,
    setAlerts: actions.setAlerts,
    addAlert: actions.addAlert,
    resolveAlert: actions.resolveAlert,
  };
}

export function useSettings() {
  const { state, actions } = useApp();
  return {
    settings: state.settings,
    updateSettings: actions.updateSettings,
  };
}

export default AppContext;
