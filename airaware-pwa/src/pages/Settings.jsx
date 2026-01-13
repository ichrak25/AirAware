import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import {
  User,
  Bell,
  Palette,
  Globe,
  Shield,
  Smartphone,
  Moon,
  Sun,
  Volume2,
  VolumeX,
  RefreshCw,
  LogOut,
  ChevronRight,
  Check,
  BellRing,
  BellOff,
  AlertTriangle,
  MessageSquare,
} from 'lucide-react';
import { useTheme, useSettings, useApp } from '../context/AppContext';
import notificationService from '../services/notifications';

/**
 * Settings Section Component
 */
function SettingsSection({ title, children }) {
  return (
    <div className="glass-card overflow-hidden">
      <div className="px-5 py-4 bg-slate-50 dark:bg-slate-800/50 border-b border-slate-200 dark:border-slate-700">
        <h2 className="font-semibold text-slate-900 dark:text-white">{title}</h2>
      </div>
      <div className="divide-y divide-slate-200 dark:divide-slate-700">
        {children}
      </div>
    </div>
  );
}

/**
 * Settings Item Component
 */
function SettingsItem({ icon: Icon, label, description, children, onClick }) {
  return (
    <div 
      className={`
        flex items-center justify-between px-5 py-4
        ${onClick ? 'cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-800/50' : ''}
      `}
      onClick={onClick}
    >
      <div className="flex items-center gap-4">
        {Icon && (
          <div className="w-10 h-10 rounded-xl bg-slate-100 dark:bg-slate-800 flex items-center justify-center">
            <Icon className="w-5 h-5 text-slate-600 dark:text-slate-400" />
          </div>
        )}
        <div>
          <p className="font-medium text-slate-900 dark:text-white">{label}</p>
          {description && (
            <p className="text-sm text-slate-500">{description}</p>
          )}
        </div>
      </div>
      <div className="flex items-center gap-2">
        {children}
        {onClick && <ChevronRight className="w-5 h-5 text-slate-400" />}
      </div>
    </div>
  );
}

/**
 * Toggle Switch Component
 */
function Toggle({ enabled, onChange }) {
  return (
    <button
      onClick={() => onChange(!enabled)}
      className={`
        relative w-12 h-7 rounded-full transition-colors
        ${enabled ? 'bg-air-500' : 'bg-slate-300 dark:bg-slate-600'}
      `}
    >
      <div 
        className={`
          absolute top-1 w-5 h-5 rounded-full bg-white shadow-sm transition-transform
          ${enabled ? 'translate-x-6' : 'translate-x-1'}
        `}
      />
    </button>
  );
}

/**
 * Notification Settings Component
 */
function NotificationSettings({ settings, updateSettings }) {
  const [permissionStatus, setPermissionStatus] = useState(
    notificationService.getPermissionStatus()
  );
  const [pushStatus, setPushStatus] = useState({ supported: false, subscribed: false });
  const [isLoading, setIsLoading] = useState(false);

  // Check push subscription status on mount
  useEffect(() => {
    const checkPushStatus = async () => {
      const status = await notificationService.getWebPushStatus();
      setPushStatus(status);
    };
    checkPushStatus();
  }, [permissionStatus]);

  // Update notification service when settings change
  useEffect(() => {
    notificationService.updateNotificationSettings({
      enabled: settings.notifications,
      pushEnabled: settings.pushNotifications !== false,
      toastEnabled: settings.toastNotifications !== false,
      criticalOnly: settings.criticalOnly || false
    });
  }, [settings.notifications, settings.pushNotifications, settings.toastNotifications, settings.criticalOnly]);

  const handleRequestPermission = async () => {
    setIsLoading(true);
    try {
      const result = await notificationService.requestPermission();
      setPermissionStatus(result);
      // Refresh push status after permission change
      const status = await notificationService.getWebPushStatus();
      setPushStatus(status);
    } finally {
      setIsLoading(false);
    }
  };

  const handleTestNotification = async () => {
    // Show a test toast notification
    notificationService.showToast({
      title: 'Test Notification',
      message: 'This is a test notification. Your notifications are working!',
      type: 'success',
      duration: 5000
    });

    // Also show browser notification if permitted
    if (permissionStatus === 'granted') {
      notificationService.showAlertNotification({
        id: 'test-' + Date.now(),
        type: 'TEST',
        severity: 'INFO',
        message: 'This is a test browser notification!',
        sensorId: 'TEST'
      });
    }
  };

  const handleTestBackgroundPush = async () => {
    setIsLoading(true);
    try {
      // Test via service worker (local test)
      await notificationService.testLocalPush();
      notificationService.showToast({
        title: 'Background Push Test',
        message: 'A push notification was sent! Check your notification tray.',
        type: 'info',
        duration: 5000
      });
    } catch (error) {
      notificationService.showToast({
        title: 'Test Failed',
        message: 'Could not send test push: ' + error.message,
        type: 'error',
        duration: 5000
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <>
      {/* Browser Permission Status */}
      {notificationService.isNotificationSupported() && (
        <SettingsItem
          icon={permissionStatus === 'granted' ? BellRing : BellOff}
          label="Browser Notifications"
          description={
            permissionStatus === 'granted' 
              ? 'Notifications are enabled' 
              : permissionStatus === 'denied'
                ? 'Notifications blocked - enable in browser settings'
                : 'Click to enable browser notifications'
          }
        >
          {permissionStatus === 'granted' ? (
            <span className="text-sm text-green-600 dark:text-green-400 flex items-center gap-1">
              <Check className="w-4 h-4" /> Enabled
            </span>
          ) : permissionStatus === 'denied' ? (
            <span className="text-sm text-red-500">Blocked</span>
          ) : (
            <button
              onClick={handleRequestPermission}
              disabled={isLoading}
              className="btn btn-primary text-sm py-1.5 px-3"
            >
              {isLoading ? 'Enabling...' : 'Enable'}
            </button>
          )}
        </SettingsItem>
      )}

      {/* Background Push Status */}
      {pushStatus.supported && permissionStatus === 'granted' && (
        <SettingsItem
          icon={Smartphone}
          label="Background Notifications"
          description={
            pushStatus.subscribed
              ? '✅ Active - You\'ll receive alerts even when the app is closed'
              : 'Notifications when app is closed/in background'
          }
        >
          {pushStatus.subscribed ? (
            <span className="text-sm text-green-600 dark:text-green-400 flex items-center gap-1">
              <Check className="w-4 h-4" /> Active
            </span>
          ) : (
            <button
              onClick={handleRequestPermission}
              disabled={isLoading}
              className="btn btn-primary text-sm py-1.5 px-3"
            >
              Activate
            </button>
          )}
        </SettingsItem>
      )}

      {/* Master Toggle */}
      <SettingsItem
        icon={Bell}
        label="All Notifications"
        description="Enable or disable all notifications"
      >
        <Toggle
          enabled={settings.notifications}
          onChange={(enabled) => updateSettings({ notifications: enabled })}
        />
      </SettingsItem>

      {/* In-App Toasts */}
      <SettingsItem
        icon={MessageSquare}
        label="In-App Alerts"
        description="Show toast notifications inside the app"
      >
        <Toggle
          enabled={settings.toastNotifications !== false}
          onChange={(enabled) => updateSettings({ toastNotifications: enabled })}
        />
      </SettingsItem>

      {/* Critical Only */}
      <SettingsItem
        icon={AlertTriangle}
        label="Critical Alerts Only"
        description="Only notify for critical air quality issues"
      >
        <Toggle
          enabled={settings.criticalOnly || false}
          onChange={(enabled) => updateSettings({ criticalOnly: enabled })}
        />
      </SettingsItem>

      {/* Sound Alerts */}
      <SettingsItem
        icon={settings.soundAlerts ? Volume2 : VolumeX}
        label="Sound Alerts"
        description="Play sound for critical alerts"
      >
        <Toggle
          enabled={settings.soundAlerts}
          onChange={(enabled) => updateSettings({ soundAlerts: enabled })}
        />
      </SettingsItem>

      {/* Test In-App Notification */}
      <SettingsItem
        icon={Bell}
        label="Test In-App Notification"
        description="Send a test notification inside the app"
      >
        <button
          onClick={handleTestNotification}
          className="btn text-sm py-1.5 px-3 bg-slate-100 dark:bg-slate-700 hover:bg-slate-200 dark:hover:bg-slate-600"
        >
          Test
        </button>
      </SettingsItem>

      {/* Test Background Push - only show if subscribed */}
      {pushStatus.subscribed && (
        <SettingsItem
          icon={Smartphone}
          label="Test Background Push"
          description="Test notification when app is in background"
        >
          <button
            onClick={handleTestBackgroundPush}
            disabled={isLoading}
            className="btn text-sm py-1.5 px-3 bg-air-100 dark:bg-air-900/30 text-air-700 dark:text-air-300 hover:bg-air-200 dark:hover:bg-air-800/50"
          >
            {isLoading ? 'Sending...' : 'Test Push'}
          </button>
        </SettingsItem>
      )}
    </>
  );
}

/**
 * Settings Page
 */
export default function SettingsPage() {
  const { state, actions } = useApp();
  const { theme, isDark, setTheme } = useTheme();
  const { settings, updateSettings } = useSettings();
  
  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="space-y-6 max-w-2xl mx-auto"
    >
      {/* Header */}
      <div>
        <h1 className="text-2xl font-display font-bold text-slate-900 dark:text-white">
          Settings
        </h1>
        <p className="text-slate-500 dark:text-slate-400">
          Customize your AirAware experience
        </p>
      </div>
      
      {/* Profile Section */}
      <SettingsSection title="Profile">
        <SettingsItem
          icon={User}
          label="Account"
          description="Manage your account settings"
          onClick={() => {}}
        />
        <SettingsItem
          icon={Shield}
          label="Privacy & Security"
          description="Configure privacy options"
          onClick={() => {}}
        />
      </SettingsSection>
      
      {/* Appearance Section */}
      <SettingsSection title="Appearance">
        <SettingsItem
          icon={isDark ? Moon : Sun}
          label="Theme"
          description="Choose your preferred theme"
        >
          <select
            value={theme}
            onChange={(e) => setTheme(e.target.value)}
            className="input py-2 text-sm"
          >
            <option value="light">Light</option>
            <option value="dark">Dark</option>
            <option value="system">System</option>
          </select>
        </SettingsItem>
        
        <SettingsItem
          icon={Globe}
          label="Temperature Unit"
          description="Celsius or Fahrenheit"
        >
          <select
            value={settings.temperatureUnit}
            onChange={(e) => updateSettings({ temperatureUnit: e.target.value })}
            className="input py-2 text-sm"
          >
            <option value="C">Celsius (°C)</option>
            <option value="F">Fahrenheit (°F)</option>
          </select>
        </SettingsItem>
      </SettingsSection>
      
      {/* Notifications Section */}
      <SettingsSection title="Notifications">
        <NotificationSettings settings={settings} updateSettings={updateSettings} />
      </SettingsSection>
      
      {/* Data Section */}
      <SettingsSection title="Data & Sync">
        <SettingsItem
          icon={RefreshCw}
          label="Auto-Refresh Interval"
          description="How often to update data"
        >
          <select
            value={settings.refreshInterval}
            onChange={(e) => updateSettings({ refreshInterval: Number(e.target.value) })}
            className="input py-2 text-sm"
          >
            <option value={10000}>10 seconds</option>
            <option value={30000}>30 seconds</option>
            <option value={60000}>1 minute</option>
            <option value={300000}>5 minutes</option>
          </select>
        </SettingsItem>
        
        <SettingsItem
          icon={Smartphone}
          label="Offline Mode"
          description="Save data for offline access"
        >
          <Toggle
            enabled={true}
            onChange={() => {}}
          />
        </SettingsItem>
      </SettingsSection>
      
      {/* About Section */}
      <SettingsSection title="About">
        <SettingsItem
          label="Version"
          description="AirAware PWA"
        >
          <span className="text-sm text-slate-500">1.0.0</span>
        </SettingsItem>
        
        <SettingsItem
          label="API Status"
          description="Backend connection"
        >
          <span className="flex items-center gap-2 text-sm text-green-600">
            <span className="w-2 h-2 rounded-full bg-green-500 animate-pulse" />
            Connected
          </span>
        </SettingsItem>
      </SettingsSection>
      
      {/* Logout */}
      <button 
        onClick={() => actions.logout()}
        className="btn btn-ghost w-full text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20"
      >
        <LogOut className="w-4 h-4" />
        Sign Out
      </button>
    </motion.div>
  );
}
