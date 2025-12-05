import React, { Suspense, lazy } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AppProvider } from './context/AppContext';
import Layout from './components/Layout/Layout';

// Lazy load pages for better performance
const Dashboard = lazy(() => import('./pages/Dashboard'));
const MapPage = lazy(() => import('./pages/Map'));
const SensorsPage = lazy(() => import('./pages/Sensors'));
const AlertsPage = lazy(() => import('./pages/Alerts'));
const AnalyticsPage = lazy(() => import('./pages/Analytics'));
const SettingsPage = lazy(() => import('./pages/Settings'));

/**
 * Loading Spinner Component
 */
function LoadingSpinner() {
  return (
    <div className="flex items-center justify-center min-h-[60vh]">
      <div className="relative">
        <div className="w-16 h-16 rounded-full border-4 border-air-200 dark:border-air-800" />
        <div className="absolute top-0 left-0 w-16 h-16 rounded-full border-4 border-transparent border-t-air-500 animate-spin" />
      </div>
    </div>
  );
}

/**
 * OAuth Callback Handler
 */
function OAuthCallback() {
  React.useEffect(() => {
    const urlParams = new URLSearchParams(window.location.search);
    const code = urlParams.get('code');
    const state = urlParams.get('state');
    
    if (code) {
      const exchangeCode = async () => {
        try {
          const codeVerifier = sessionStorage.getItem('code_verifier');
          const savedState = sessionStorage.getItem('oauth_state');
          
          if (state !== savedState) {
            throw new Error('State mismatch');
          }
          
          const response = await fetch('/iam/api/oauth/token', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({
              grant_type: 'authorization_code',
              code,
              redirect_uri: `${window.location.origin}/callback`,
              client_id: 'airaware-web-client',
              code_verifier: codeVerifier,
            }),
          });
          
          if (response.ok) {
            const tokens = await response.json();
            localStorage.setItem('access_token', tokens.access_token);
            localStorage.setItem('refresh_token', tokens.refresh_token);
            window.location.href = '/';
          }
        } catch (error) {
          console.error('OAuth error:', error);
          window.location.href = '/login?error=auth_failed';
        }
      };
      
      exchangeCode();
    }
  }, []);
  
  return (
    <div className="min-h-screen flex items-center justify-center bg-air-50 dark:bg-slate-950">
      <div className="text-center">
        <LoadingSpinner />
        <p className="mt-4 text-slate-600 dark:text-slate-400">
          Completing sign in...
        </p>
      </div>
    </div>
  );
}

/**
 * Login Page
 */
function LoginPage() {
  const handleLogin = () => {
    const generateCodeVerifier = () => {
      const array = new Uint8Array(32);
      crypto.getRandomValues(array);
      return btoa(String.fromCharCode(...array))
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=/g, '');
    };
    
    const generateCodeChallenge = async (verifier) => {
      const encoder = new TextEncoder();
      const data = encoder.encode(verifier);
      const hash = await crypto.subtle.digest('SHA-256', data);
      return btoa(String.fromCharCode(...new Uint8Array(hash)))
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=/g, '');
    };
    
    const initiateOAuth = async () => {
      const codeVerifier = generateCodeVerifier();
      const codeChallenge = await generateCodeChallenge(codeVerifier);
      const state = generateCodeVerifier();
      
      sessionStorage.setItem('code_verifier', codeVerifier);
      sessionStorage.setItem('oauth_state', state);
      
      const params = new URLSearchParams({
        client_id: 'airaware-web-client',
        redirect_uri: `${window.location.origin}/callback`,
        response_type: 'code',
        scope: 'openid profile',
        code_challenge: codeChallenge,
        code_challenge_method: 'S256',
        state,
      });
      
      window.location.href = `/iam/api/oauth/authorize?${params}`;
    };
    
    initiateOAuth();
  };
  
  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-air-50 to-teal-50 dark:from-slate-950 dark:to-slate-900 p-4">
      <div className="glass-card p-8 w-full max-w-md text-center">
        <div className="w-20 h-20 mx-auto mb-6 rounded-2xl bg-gradient-to-br from-air-500 to-teal-500 flex items-center justify-center shadow-lg shadow-air-500/30">
          <svg className="w-10 h-10 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
          </svg>
        </div>
        
        <h1 className="text-2xl font-display font-bold text-slate-900 dark:text-white mb-2">
          Welcome to AirAware
        </h1>
        <p className="text-slate-500 dark:text-slate-400 mb-8">
          Real-time air quality monitoring for Tunisia
        </p>
        
        <button
          onClick={handleLogin}
          className="btn btn-primary w-full text-lg py-3"
        >
          Sign In
        </button>
        
        <div className="mt-6 pt-6 border-t border-slate-200 dark:border-slate-700">
          <p className="text-sm text-slate-500 mb-3">Or continue without signing in</p>
          <a 
            href="/"
            className="btn btn-ghost w-full"
          >
            View Demo
          </a>
        </div>
        
        <div className="mt-8 grid grid-cols-3 gap-4 text-center">
          {[
            { icon: '📊', label: 'Real-time Data' },
            { icon: '🗺️', label: 'Interactive Maps' },
            { icon: '🔔', label: 'Smart Alerts' },
          ].map((feature) => (
            <div key={feature.label} className="text-slate-500">
              <span className="text-2xl">{feature.icon}</span>
              <p className="text-xs mt-1">{feature.label}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

/**
 * Main App Component
 */
export default function App() {
  return (
    <BrowserRouter>
      <AppProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/callback" element={<OAuthCallback />} />
          
          <Route
            path="/*"
            element={
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
            }
          />
        </Routes>
      </AppProvider>
    </BrowserRouter>
  );
}
