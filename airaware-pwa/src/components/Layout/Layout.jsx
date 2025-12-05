import React from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Home,
  Map,
  Activity,
  Bell,
  Settings,
  Menu,
  X,
  Sun,
  Moon,
  LogOut,
  User,
  Wifi,
  WifiOff,
  Download,
} from 'lucide-react';
import { useApp, useTheme, useAlerts } from '../../context/AppContext';
import { AlertBadge } from '../Alerts/AlertList';

// Navigation items
const NAV_ITEMS = [
  { path: '/', icon: Home, label: 'Dashboard' },
  { path: '/map', icon: Map, label: 'Map' },
  { path: '/sensors', icon: Activity, label: 'Sensors' },
  { path: '/alerts', icon: Bell, label: 'Alerts', badge: true },
  { path: '/analytics', icon: Activity, label: 'Analytics' },
  { path: '/settings', icon: Settings, label: 'Settings' },
];

/**
 * Navbar Component
 */
export function Navbar() {
  const { state, actions } = useApp();
  const { isDark, toggleTheme } = useTheme();
  const { unresolvedAlerts } = useAlerts();
  const navigate = useNavigate();
  const [isOnline, setIsOnline] = React.useState(navigator.onLine);
  const [showInstallPrompt, setShowInstallPrompt] = React.useState(false);
  const [deferredPrompt, setDeferredPrompt] = React.useState(null);
  
  // Online/offline detection
  React.useEffect(() => {
    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);
    
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    
    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);
  
  // PWA install prompt
  React.useEffect(() => {
    const handleBeforeInstallPrompt = (e) => {
      e.preventDefault();
      setDeferredPrompt(e);
      setShowInstallPrompt(true);
    };
    
    window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
    return () => window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
  }, []);
  
  const handleInstall = async () => {
    if (!deferredPrompt) return;
    deferredPrompt.prompt();
    const { outcome } = await deferredPrompt.userChoice;
    if (outcome === 'accepted') {
      setShowInstallPrompt(false);
    }
    setDeferredPrompt(null);
  };
  
  return (
    <header className="fixed top-0 left-0 right-0 z-50 safe-top">
      <div className="glass-card mx-4 mt-4 rounded-2xl border border-white/20 dark:border-slate-700/50">
        <div className="flex items-center justify-between px-4 py-3">
          {/* Logo & Menu Toggle */}
          <div className="flex items-center gap-3">
            <button
              onClick={actions.toggleSidebar}
              className="lg:hidden p-2 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
            >
              <Menu className="w-5 h-5 text-slate-600 dark:text-slate-300" />
            </button>
            
            <Link to="/" className="flex items-center gap-2">
              <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-air-500 to-teal-500 flex items-center justify-center">
                <Activity className="w-5 h-5 text-white" />
              </div>
              <span className="font-display font-bold text-xl text-slate-900 dark:text-white hidden sm:block">
                AirAware
              </span>
            </Link>
          </div>
          
          {/* Status & Actions */}
          <div className="flex items-center gap-2">
            {/* Connection Status */}
            <div className={`
              flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-medium
              ${isOnline 
                ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
                : 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400'
              }
            `}>
              {isOnline ? <Wifi className="w-3.5 h-3.5" /> : <WifiOff className="w-3.5 h-3.5" />}
              <span className="hidden sm:inline">{isOnline ? 'Online' : 'Offline'}</span>
            </div>
            
            {/* Install PWA Button */}
            {showInstallPrompt && (
              <motion.button
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                onClick={handleInstall}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-medium bg-air-100 text-air-700 dark:bg-air-900/30 dark:text-air-400 hover:bg-air-200 dark:hover:bg-air-900/50 transition-colors"
              >
                <Download className="w-3.5 h-3.5" />
                <span className="hidden sm:inline">Install</span>
              </motion.button>
            )}
            
            {/* Alerts */}
            <Link 
              to="/alerts"
              className="relative p-2 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
            >
              <Bell className="w-5 h-5 text-slate-600 dark:text-slate-300" />
              <AlertBadge 
                count={unresolvedAlerts.length} 
                className="absolute -top-1 -right-1"
              />
            </Link>
            
            {/* Theme Toggle */}
            <button
              onClick={toggleTheme}
              className="p-2 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
            >
              {isDark ? (
                <Sun className="w-5 h-5 text-yellow-500" />
              ) : (
                <Moon className="w-5 h-5 text-slate-600" />
              )}
            </button>
            
            {/* User Menu */}
            <button
              onClick={() => navigate('/settings')}
              className="p-2 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
            >
              <User className="w-5 h-5 text-slate-600 dark:text-slate-300" />
            </button>
          </div>
        </div>
      </div>
    </header>
  );
}

/**
 * Sidebar Component
 */
export function Sidebar() {
  const { state, actions } = useApp();
  const { unresolvedAlerts } = useAlerts();
  const location = useLocation();
  
  const sidebarVariants = {
    open: { x: 0 },
    closed: { x: '-100%' },
  };
  
  return (
    <>
      {/* Overlay */}
      <AnimatePresence>
        {state.sidebarOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={() => actions.setSidebar(false)}
            className="fixed inset-0 bg-black/50 z-40 lg:hidden"
          />
        )}
      </AnimatePresence>
      
      {/* Sidebar */}
      <motion.aside
        initial={false}
        animate={state.sidebarOpen ? 'open' : 'closed'}
        variants={sidebarVariants}
        transition={{ type: 'spring', damping: 25, stiffness: 200 }}
        className={`
          fixed top-0 left-0 bottom-0 w-72 z-50
          bg-white dark:bg-slate-900
          border-r border-slate-200 dark:border-slate-800
          flex flex-col
          lg:translate-x-0 lg:static lg:z-auto
          safe-top safe-bottom
        `}
      >
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-slate-200 dark:border-slate-800">
          <Link to="/" className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-air-500 to-teal-500 flex items-center justify-center">
              <Activity className="w-6 h-6 text-white" />
            </div>
            <div>
              <span className="font-display font-bold text-lg text-slate-900 dark:text-white">
                AirAware
              </span>
              <p className="text-xs text-slate-500">Air Quality Monitor</p>
            </div>
          </Link>
          
          <button
            onClick={() => actions.setSidebar(false)}
            className="lg:hidden p-2 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-800"
          >
            <X className="w-5 h-5 text-slate-500" />
          </button>
        </div>
        
        {/* Navigation */}
        <nav className="flex-1 p-4 space-y-1 overflow-y-auto">
          {NAV_ITEMS.map(({ path, icon: Icon, label, badge }) => {
            const isActive = location.pathname === path;
            const alertCount = badge ? unresolvedAlerts.length : 0;
            
            return (
              <Link
                key={path}
                to={path}
                onClick={() => actions.setSidebar(false)}
                className={`nav-item ${isActive ? 'active' : ''}`}
              >
                <Icon className="w-5 h-5" />
                <span className="flex-1">{label}</span>
                {badge && alertCount > 0 && (
                  <AlertBadge count={alertCount} />
                )}
              </Link>
            );
          })}
        </nav>
        
        {/* Footer */}
        <div className="p-4 border-t border-slate-200 dark:border-slate-800">
          <div className="glass-card p-4 bg-gradient-to-br from-air-50 to-teal-50 dark:from-air-900/20 dark:to-teal-900/20">
            <p className="text-sm font-medium text-air-700 dark:text-air-400">
              Air Quality Status
            </p>
            <p className="text-xs text-air-600 dark:text-air-500 mt-1">
              5 sensors active • Last update: Just now
            </p>
          </div>
        </div>
      </motion.aside>
    </>
  );
}

/**
 * Bottom Navigation (Mobile)
 */
export function BottomNav() {
  const location = useLocation();
  const { unresolvedAlerts } = useAlerts();
  
  const items = NAV_ITEMS.slice(0, 5); // Show only first 5 items
  
  return (
    <nav className="fixed bottom-0 left-0 right-0 z-40 lg:hidden safe-bottom">
      <div className="glass-card mx-4 mb-4 rounded-2xl border border-white/20 dark:border-slate-700/50">
        <div className="flex items-center justify-around py-2">
          {items.map(({ path, icon: Icon, label, badge }) => {
            const isActive = location.pathname === path;
            const alertCount = badge ? unresolvedAlerts.length : 0;
            
            return (
              <Link
                key={path}
                to={path}
                className={`
                  relative flex flex-col items-center gap-1 px-3 py-2 rounded-xl transition-colors
                  ${isActive 
                    ? 'text-air-600 dark:text-air-400' 
                    : 'text-slate-400 hover:text-slate-600 dark:hover:text-slate-300'
                  }
                `}
              >
                <Icon className="w-5 h-5" />
                <span className="text-[10px] font-medium">{label}</span>
                {badge && alertCount > 0 && (
                  <AlertBadge count={alertCount} className="absolute -top-1 right-0" />
                )}
              </Link>
            );
          })}
        </div>
      </div>
    </nav>
  );
}

/**
 * Main Layout Component
 */
export default function Layout({ children }) {
  return (
    <div className="min-h-screen bg-air-50 dark:bg-slate-950">
      <Navbar />
      
      <div className="flex pt-20">
        {/* Sidebar - hidden on mobile */}
        <div className="hidden lg:block">
          <Sidebar />
        </div>
        
        {/* Mobile Sidebar */}
        <div className="lg:hidden">
          <Sidebar />
        </div>
        
        {/* Main Content */}
        <main className="flex-1 min-h-[calc(100vh-5rem)] p-4 pb-24 lg:pb-4">
          <div className="max-w-7xl mx-auto">
            {children}
          </div>
        </main>
      </div>
      
      {/* Bottom Navigation - visible on mobile */}
      <BottomNav />
    </div>
  );
}
