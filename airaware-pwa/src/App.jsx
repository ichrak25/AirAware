import React, { Suspense, lazy, useState, useEffect, createContext, useContext } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { AppProvider } from './context/AppContext';
import Layout from './components/Layout/Layout';
import ToastContainer from './components/Notifications/ToastContainer';
import notificationService from './services/notifications';
import { alertsAPI } from './services/api';

// Lazy load pages
const Dashboard = lazy(() => import('./pages/Dashboard'));
const MapPage = lazy(() => import('./pages/Map'));
const SensorsPage = lazy(() => import('./pages/Sensors'));
const AlertsPage = lazy(() => import('./pages/Alerts'));
const AnalyticsPage = lazy(() => import('./pages/Analytics'));
const SettingsPage = lazy(() => import('./pages/Settings'));

// API Configuration
const API_CONFIG = { IAM_API: '/iam/api' };

// Auth Context
const AuthContext = createContext(null);

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
}

function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const checkAuth = async () => {
      const token = localStorage.getItem('access_token');
      if (!token) {
        setIsAuthenticated(false);
        setIsLoading(false);
        return;
      }
      try {
        const response = await fetch(`${API_CONFIG.IAM_API}/oauth/userinfo`, {
          headers: { 'Authorization': `Bearer ${token}` }
        });
        if (response.ok) {
          const userData = await response.json();
          setUser(userData);
          setIsAuthenticated(true);
        } else {
          logout();
        }
      } catch (error) {
        console.error('Auth check failed:', error);
        logout();
      }
      setIsLoading(false);
    };
    checkAuth();
  }, []);

  const login = (tokens, userData) => {
    localStorage.setItem('access_token', tokens.access_token);
    if (tokens.refresh_token) localStorage.setItem('refresh_token', tokens.refresh_token);
    setUser(userData);
    setIsAuthenticated(true);
  };

  const logout = () => {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    setUser(null);
    setIsAuthenticated(false);
  };

  return (
    <AuthContext.Provider value={{ user, isAuthenticated, isLoading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

function ProtectedRoute({ children }) {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();
  if (isLoading) return <LoadingSpinner fullScreen />;
  if (!isAuthenticated) return <Navigate to="/login" state={{ from: location }} replace />;
  return children;
}

function LoadingSpinner({ fullScreen = false }) {
  const spinner = (
    <div className="relative">
      <div className="w-16 h-16 rounded-full border-4 border-air-200 dark:border-air-800" />
      <div className="absolute top-0 left-0 w-16 h-16 rounded-full border-4 border-transparent border-t-air-500 animate-spin" />
    </div>
  );
  if (fullScreen) {
    return <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-air-50 to-teal-50 dark:from-slate-950 dark:to-slate-900">{spinner}</div>;
  }
  return <div className="flex items-center justify-center min-h-[60vh]">{spinner}</div>;
}

function AuthPageWrapper({ children }) {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-air-50 via-teal-50 to-cyan-50 dark:from-slate-950 dark:via-slate-900 dark:to-slate-950 p-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="w-20 h-20 mx-auto mb-4 rounded-2xl bg-gradient-to-br from-air-500 to-teal-500 flex items-center justify-center shadow-lg shadow-air-500/30">
            <svg className="w-10 h-10 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <h1 className="text-3xl font-display font-bold text-slate-900 dark:text-white">AirAware</h1>
          <p className="text-slate-500 dark:text-slate-400 mt-1">Air Quality Monitoring System</p>
        </div>
        <div className="bg-white/80 dark:bg-slate-800/80 backdrop-blur-xl rounded-2xl shadow-xl p-8 border border-white/20 dark:border-slate-700/50">
          {children}
        </div>
      </div>
    </div>
  );
}

// LOGIN PAGE
function LoginPage() {
  const { isAuthenticated, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [successMessage] = useState(location.state?.message || null);

  if (isAuthenticated) {
    return <Navigate to={location.state?.from?.pathname || '/'} replace />;
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setError(null);
    try {
      const response = await fetch(`${API_CONFIG.IAM_API}/oauth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      });
      const data = await response.json();
      if (response.ok && data.access_token) {
        login(data, { username, name: data.name || username });
        navigate(location.state?.from?.pathname || '/', { replace: true });
      } else {
        setError(data.error || data.message || 'Invalid username or password');
      }
    } catch (err) {
      setError('Unable to connect to server. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <AuthPageWrapper>
      <h2 className="text-2xl font-bold text-slate-900 dark:text-white text-center mb-6">Welcome Back</h2>
      {successMessage && (
        <div className="mb-6 p-4 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-xl">
          <p className="text-sm text-green-600 dark:text-green-400 flex items-center gap-2">
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" /></svg>
            {successMessage}
          </p>
        </div>
      )}
      {error && (
        <div className="mb-6 p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-xl">
          <p className="text-sm text-red-600 dark:text-red-400 flex items-center gap-2">
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
            {error}
          </p>
        </div>
      )}
      <form onSubmit={handleSubmit} className="space-y-5">
        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">Username</label>
          <input type="text" value={username} onChange={(e) => setUsername(e.target.value)} className="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-600 bg-white dark:bg-slate-700 text-slate-900 dark:text-white focus:ring-2 focus:ring-air-500 focus:border-transparent" placeholder="Enter your username" required autoFocus />
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">Password</label>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} className="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-600 bg-white dark:bg-slate-700 text-slate-900 dark:text-white focus:ring-2 focus:ring-air-500 focus:border-transparent" placeholder="Enter your password" required />
        </div>
        <button type="submit" disabled={isLoading} className="w-full py-3 px-4 bg-gradient-to-r from-air-500 to-teal-500 hover:from-air-600 hover:to-teal-600 text-white font-semibold rounded-xl shadow-lg shadow-air-500/30 disabled:opacity-50">
          {isLoading ? <span className="flex items-center justify-center gap-2"><svg className="animate-spin h-5 w-5" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" /><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" /></svg>Signing in...</span> : 'Sign In'}
        </button>
      </form>
      <div className="mt-6 text-center">
        <p className="text-slate-500 dark:text-slate-400">Don't have an account? <button onClick={() => navigate('/register')} className="text-air-600 hover:text-air-700 dark:text-air-400 font-semibold">Create Account</button></p>
      </div>
      <div className="mt-8 pt-6 border-t border-slate-200 dark:border-slate-700">
        <div className="grid grid-cols-3 gap-4 text-center">
          {[{ icon: '📊', label: 'Real-time Data' }, { icon: '🗺️', label: 'Interactive Maps' }, { icon: '🔔', label: 'Smart Alerts' }].map((f) => (
            <div key={f.label} className="text-slate-400"><span className="text-2xl">{f.icon}</span><p className="text-xs mt-1">{f.label}</p></div>
          ))}
        </div>
      </div>
    </AuthPageWrapper>
  );
}

// REGISTER PAGE
function RegisterPage() {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [formData, setFormData] = useState({ username: '', email: '', password: '', confirmPassword: '' });
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  if (isAuthenticated) return <Navigate to="/" replace />;

  const handleChange = (e) => setFormData({ ...formData, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setError(null);
    if (formData.password !== formData.confirmPassword) { setError('Passwords do not match'); setIsLoading(false); return; }
    if (formData.password.length < 8) { setError('Password must be at least 8 characters'); setIsLoading(false); return; }
    try {
      const response = await fetch(`${API_CONFIG.IAM_API}/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: formData.username, email: formData.email, password: formData.password })
      });
      const data = await response.json();
      if (response.ok) {
        navigate('/activate', { state: { email: formData.email, username: formData.username, message: data.message || 'Check your email for activation code' } });
      } else {
        setError(data.error || data.message || 'Registration failed');
      }
    } catch (err) {
      setError('Unable to connect to server');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <AuthPageWrapper>
      <h2 className="text-2xl font-bold text-slate-900 dark:text-white text-center mb-6">Create Account</h2>
      {error && <div className="mb-6 p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-xl"><p className="text-sm text-red-600 dark:text-red-400">{error}</p></div>}
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">Username</label>
          <input type="text" name="username" value={formData.username} onChange={handleChange} className="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-600 bg-white dark:bg-slate-700 text-slate-900 dark:text-white focus:ring-2 focus:ring-air-500" placeholder="Choose a username" required autoFocus />
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">Email</label>
          <input type="email" name="email" value={formData.email} onChange={handleChange} className="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-600 bg-white dark:bg-slate-700 text-slate-900 dark:text-white focus:ring-2 focus:ring-air-500" placeholder="your@email.com" required />
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">Password</label>
          <input type="password" name="password" value={formData.password} onChange={handleChange} className="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-600 bg-white dark:bg-slate-700 text-slate-900 dark:text-white focus:ring-2 focus:ring-air-500" placeholder="At least 8 characters" required minLength={8} />
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">Confirm Password</label>
          <input type="password" name="confirmPassword" value={formData.confirmPassword} onChange={handleChange} className="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-600 bg-white dark:bg-slate-700 text-slate-900 dark:text-white focus:ring-2 focus:ring-air-500" placeholder="Confirm password" required />
        </div>
        <button type="submit" disabled={isLoading} className="w-full py-3 px-4 bg-gradient-to-r from-air-500 to-teal-500 hover:from-air-600 hover:to-teal-600 text-white font-semibold rounded-xl shadow-lg shadow-air-500/30 disabled:opacity-50">
          {isLoading ? 'Creating Account...' : 'Create Account'}
        </button>
      </form>
      <div className="mt-6 text-center">
        <p className="text-slate-500 dark:text-slate-400">Already have an account? <button onClick={() => navigate('/login')} className="text-air-600 hover:text-air-700 dark:text-air-400 font-semibold">Sign In</button></p>
      </div>
    </AuthPageWrapper>
  );
}

// ACTIVATION PAGE
function ActivationPage() {
  const { isAuthenticated, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [activationCode, setActivationCode] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const email = location.state?.email || '';
  const username = location.state?.username || '';
  const message = location.state?.message || 'Enter the activation code sent to your email';

  if (isAuthenticated) return <Navigate to="/" replace />;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setError(null);
    setSuccess(null);
    try {
      const response = await fetch(`${API_CONFIG.IAM_API}/register/activate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code: activationCode, email, username })
      });
      const data = await response.json();
      if (response.ok) {
        setSuccess('Account activated! Redirecting...');
        if (data.access_token) {
          login(data, { username, email });
          setTimeout(() => navigate('/', { replace: true }), 1000);
        } else {
          setTimeout(() => navigate('/login', { state: { message: 'Account activated! Please sign in.' } }), 1500);
        }
      } else {
        setError(data.error || data.message || 'Invalid activation code');
      }
    } catch (err) {
      setError('Unable to connect to server');
    } finally {
      setIsLoading(false);
    }
  };

  const handleResendCode = async () => {
    if (!email) { setError('Email not found. Please register again.'); return; }
    setIsLoading(true);
    setError(null);
    try {
      const response = await fetch(`${API_CONFIG.IAM_API}/register/resend`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email })
      });
      if (response.ok) setSuccess('New activation code sent!');
      else setError('Failed to resend code');
    } catch (err) {
      setError('Unable to connect to server');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <AuthPageWrapper>
      <div className="text-center mb-6">
        <div className="w-16 h-16 mx-auto mb-4 rounded-full bg-air-100 dark:bg-air-900/30 flex items-center justify-center">
          <svg className="w-8 h-8 text-air-500" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" /></svg>
        </div>
        <h2 className="text-2xl font-bold text-slate-900 dark:text-white">Activate Your Account</h2>
        <p className="text-slate-500 dark:text-slate-400 mt-2">{message}</p>
        {email && <p className="text-sm text-air-600 dark:text-air-400 mt-1">{email}</p>}
      </div>
      {error && <div className="mb-6 p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-xl"><p className="text-sm text-red-600 dark:text-red-400">{error}</p></div>}
      {success && <div className="mb-6 p-4 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-xl"><p className="text-sm text-green-600 dark:text-green-400 flex items-center gap-2"><svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" /></svg>{success}</p></div>}
      <form onSubmit={handleSubmit} className="space-y-5">
        <div>
          <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">Activation Code</label>
          <input type="text" value={activationCode} onChange={(e) => setActivationCode(e.target.value.toUpperCase())} className="w-full px-4 py-4 rounded-xl border border-slate-200 dark:border-slate-600 bg-white dark:bg-slate-700 text-slate-900 dark:text-white text-center text-2xl tracking-widest font-mono focus:ring-2 focus:ring-air-500" placeholder="XXXXXX" maxLength={10} required autoFocus />
        </div>
        <button type="submit" disabled={isLoading || !activationCode} className="w-full py-3 px-4 bg-gradient-to-r from-air-500 to-teal-500 hover:from-air-600 hover:to-teal-600 text-white font-semibold rounded-xl shadow-lg shadow-air-500/30 disabled:opacity-50">
          {isLoading ? 'Activating...' : 'Activate Account'}
        </button>
      </form>
      <div className="mt-6 flex flex-col items-center gap-3">
        <button onClick={handleResendCode} disabled={isLoading} className="text-air-600 hover:text-air-700 dark:text-air-400 text-sm font-medium">Didn't receive the code? Resend</button>
        <button onClick={() => navigate('/login')} className="text-slate-500 hover:text-slate-700 dark:text-slate-400 text-sm">← Back to Sign In</button>
      </div>
    </AuthPageWrapper>
  );
}

// Notification Initializer - starts alert polling and Web Push when authenticated
function NotificationInitializer() {
  const navigate = useNavigate();

  useEffect(() => {
    // Initialize Web Push notifications
    // This sets up the service worker message listener for notification clicks
    notificationService.initializePushNotifications((event) => {
      console.log('[App] Notification clicked:', event);
      // Navigate to the URL specified in the notification
      if (event.url) {
        navigate(event.url);
      }
    });

    // Request notification permission on first load
    if (notificationService.isNotificationSupported() && 
        notificationService.getPermissionStatus() === 'default') {
      // Delay permission request to not be intrusive
      const timer = setTimeout(() => {
        notificationService.requestPermission();
      }, 5000);
      return () => clearTimeout(timer);
    }
  }, [navigate]);

  useEffect(() => {
    // Start polling for new alerts
    const fetchAlerts = async () => {
      try {
        const alerts = await alertsAPI.getAll();
        return alerts;
      } catch (error) {
        console.error('[Notifications] Failed to fetch alerts:', error);
        return [];
      }
    };

    // Poll every 30 seconds
    const stopPolling = notificationService.startAlertPolling(fetchAlerts, 30000);
    
    return () => {
      stopPolling();
    };
  }, []);

  return null; // This component doesn't render anything
}

// MAIN APP
export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppProvider>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/activate" element={<ActivationPage />} />
            <Route path="/*" element={
              <ProtectedRoute>
                <NotificationInitializer />
                <Layout>
                  <Suspense fallback={<LoadingSpinner />}>
                    <Routes>
                      <Route path="/" element={<Dashboard />} />
                      <Route path="/map" element={<MapPage />} />
                      <Route path="/sensors" element={<SensorsPage />} />
                      <Route path="/alerts" element={<AlertsPage />} />
                      <Route path="/analytics" element={<AnalyticsPage />} />
                      <Route path="/settings" element={<SettingsPage />} />
                      <Route path="*" element={<Navigate to="/" replace />} />
                    </Routes>
                  </Suspense>
                </Layout>
              </ProtectedRoute>
            } />
          </Routes>
        </AppProvider>
      </AuthProvider>
      {/* Global Toast Notifications */}
      <ToastContainer />
    </BrowserRouter>
  );
}
