import React from 'react';
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
} from 'lucide-react';
import { useTheme, useSettings, useApp } from '../context/AppContext';

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
        <SettingsItem
          icon={Bell}
          label="Push Notifications"
          description="Receive alerts on your device"
        >
          <Toggle
            enabled={settings.notifications}
            onChange={(enabled) => updateSettings({ notifications: enabled })}
          />
        </SettingsItem>
        
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
