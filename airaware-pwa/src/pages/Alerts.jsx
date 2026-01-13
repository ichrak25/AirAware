import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
    Bell,
    Filter,
    CheckCircle2,
    AlertTriangle,
    AlertOctagon,
    Search,
    Calendar,
    RefreshCw,
    WifiOff,
    X,
    MessageSquare,
} from 'lucide-react';
import { useAlerts } from '../context/AppContext';
import { alertsAPI, mockData } from '../services/api';
import AlertList, { AlertSummary } from '../components/Alerts/AlertList';

// Predefined resolution note options
const RESOLUTION_PRESETS = [
    'Fixed ventilation system',
    'False alarm',
    'Sensor recalibrated',
    'Environmental conditions normalized',
    'Maintenance completed',
    'Issue acknowledged - monitoring',
];

/**
 * Alerts Page - WITH REAL API INTEGRATION
 */
export default function AlertsPage() {
    const { alerts, setAlerts, resolveAlert } = useAlerts();
    const [searchQuery, setSearchQuery] = useState('');
    const [filterSeverity, setFilterSeverity] = useState('all');
    const [filterStatus, setFilterStatus] = useState('all');
    const [selectedAlert, setSelectedAlert] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isRefreshing, setIsRefreshing] = useState(false);
    const [useMockData, setUseMockData] = useState(false);
    const [error, setError] = useState(null);
    
    // Resolution dialog state
    const [resolveDialogOpen, setResolveDialogOpen] = useState(false);
    const [alertToResolve, setAlertToResolve] = useState(null);
    const [resolutionNotes, setResolutionNotes] = useState('');
    const [isResolving, setIsResolving] = useState(false);

    // Load data from API
    useEffect(() => {
        loadData();

        // Set up auto-refresh every 30 seconds
        const interval = setInterval(refreshData, 30000);
        return () => clearInterval(interval);
    }, []);

    const loadData = async () => {
        setIsLoading(true);
        setError(null);

        try {
            console.log('[Alerts] Fetching real data from API...');

            let alertsData = [];
            try {
                alertsData = await alertsAPI.getAll();
                console.log('[Alerts] Loaded:', alertsData.length, 'alerts');
            } catch (e) {
                console.warn('[Alerts] Failed to fetch alerts:', e.message);
            }

            if (alertsData.length > 0) {
                console.log('[Alerts] ✅ Using REAL DATA');
                setUseMockData(false);
                setAlerts(alertsData);
            } else {
                // Fall back to mock data
                console.log('[Alerts] ⚠️ No real data - using MOCK DATA');
                setUseMockData(true);
                setAlerts(mockData.alerts);
            }
        } catch (err) {
            console.error('[Alerts] Error loading data:', err);
            setError(err.message);
            setUseMockData(true);
            setAlerts(mockData.alerts);
        } finally {
            setIsLoading(false);
        }
    };

    const refreshData = async () => {
        setIsRefreshing(true);

        try {
            if (!useMockData) {
                const alertsData = await alertsAPI.getAll();
                setAlerts(alertsData);
            }
        } catch (err) {
            console.error('[Alerts] Refresh failed:', err);
        } finally {
            setTimeout(() => setIsRefreshing(false), 500);
        }
    };

    // Open resolve dialog
    const openResolveDialog = (alertId) => {
        const alert = alerts.find(a => a.id === alertId);
        setAlertToResolve(alert);
        setResolutionNotes('');
        setResolveDialogOpen(true);
    };

    // Close resolve dialog
    const closeResolveDialog = () => {
        setResolveDialogOpen(false);
        setAlertToResolve(null);
        setResolutionNotes('');
    };

    // Handle resolve alert with notes
    const handleResolveAlert = async () => {
        if (!alertToResolve) return;
        
        setIsResolving(true);
        try {
            if (!useMockData) {
                await alertsAPI.resolve(alertToResolve.id, resolutionNotes || null);
            }
            resolveAlert(alertToResolve.id);
            closeResolveDialog();
        } catch (err) {
            console.error('[Alerts] Failed to resolve alert:', err);
        } finally {
            setIsResolving(false);
        }
    };

    // Handle resolve all alerts (quick resolve without notes)
    const handleResolveAll = async () => {
        const unresolvedAlerts = alerts.filter(a => !a.resolved);

        for (const alert of unresolvedAlerts) {
            try {
                if (!useMockData) {
                    await alertsAPI.resolve(alert.id, 'Bulk resolved');
                }
                resolveAlert(alert.id);
            } catch (err) {
                console.error('[Alerts] Failed to resolve alert:', err);
            }
        }
    };

    // Filter alerts
    const filteredAlerts = alerts.filter(alert => {
        const matchesSearch =
            alert.message?.toLowerCase().includes(searchQuery.toLowerCase()) ||
            alert.sensorId?.toLowerCase().includes(searchQuery.toLowerCase()) ||
            alert.type?.toLowerCase().includes(searchQuery.toLowerCase());

        const matchesSeverity = filterSeverity === 'all' || alert.severity === filterSeverity;

        const matchesStatus = filterStatus === 'all' ||
            (filterStatus === 'active' && !alert.resolved) ||
            (filterStatus === 'resolved' && alert.resolved);

        return matchesSearch && matchesSeverity && matchesStatus;
    });

    // Sort by date (newest first) and unresolved first
    const sortedAlerts = [...filteredAlerts].sort((a, b) => {
        if (a.resolved !== b.resolved) return a.resolved ? 1 : -1;
        return new Date(b.triggeredAt) - new Date(a.triggeredAt);
    });

    // Stats
    const stats = {
        total: alerts.length,
        active: alerts.filter(a => !a.resolved).length,
        critical: alerts.filter(a => !a.resolved && a.severity === 'CRITICAL').length,
        warning: alerts.filter(a => !a.resolved && a.severity === 'WARNING').length,
    };

    // Loading state
    if (isLoading) {
        return (
            <div className="flex items-center justify-center min-h-[60vh]">
                <div className="text-center">
                    <RefreshCw className="w-12 h-12 text-air-600 animate-spin mx-auto mb-4" />
                    <p className="text-slate-600 dark:text-slate-400">Loading alerts...</p>
                </div>
            </div>
        );
    }

    return (
        <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="space-y-6"
        >
            {/* Data Source Indicator */}
            {useMockData && (
                <div className="bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-xl p-4">
                    <div className="flex items-center gap-3">
                        <WifiOff className="w-5 h-5 text-yellow-600" />
                        <div>
                            <p className="font-medium text-yellow-800 dark:text-yellow-200">
                                Demo Mode - Using Mock Data
                            </p>
                            <p className="text-sm text-yellow-600 dark:text-yellow-400">
                                Connect your backend to see real alerts from sensors
                            </p>
                        </div>
                        <button onClick={loadData} className="ml-auto btn btn-secondary text-sm">
                            Retry Connection
                        </button>
                    </div>
                </div>
            )}

            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-display font-bold text-slate-900 dark:text-white">
                        Alerts
                    </h1>
                    <p className="text-slate-500 dark:text-slate-400">
                        {useMockData
                            ? 'Demo data - Connect backend for real alerts'
                            : 'Monitor and manage air quality alerts'
                        }
                    </p>
                </div>

                <div className="flex items-center gap-2">
                    <button
                        onClick={refreshData}
                        disabled={isRefreshing}
                        className="btn btn-secondary"
                    >
                        <RefreshCw className={`w-4 h-4 ${isRefreshing ? 'animate-spin' : ''}`} />
                    </button>
                    <button
                        onClick={handleResolveAll}
                        className="btn btn-secondary"
                        disabled={stats.active === 0}
                    >
                        <CheckCircle2 className="w-4 h-4" />
                        Resolve All
                    </button>
                </div>
            </div>

            {/* Stats Cards */}
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
                <div className="glass-card p-4">
                    <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-xl bg-slate-100 dark:bg-slate-800 flex items-center justify-center">
                            <Bell className="w-5 h-5 text-slate-600" />
                        </div>
                        <div>
                            <p className="text-sm text-slate-500">Total Alerts</p>
                            <p className="text-2xl font-bold">{stats.total}</p>
                        </div>
                    </div>
                </div>

                <div className="glass-card p-4">
                    <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-xl bg-yellow-100 dark:bg-yellow-900/30 flex items-center justify-center">
                            <AlertTriangle className="w-5 h-5 text-yellow-600" />
                        </div>
                        <div>
                            <p className="text-sm text-slate-500">Active</p>
                            <p className="text-2xl font-bold text-yellow-600">{stats.active}</p>
                        </div>
                    </div>
                </div>

                <div className="glass-card p-4">
                    <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-xl bg-red-100 dark:bg-red-900/30 flex items-center justify-center">
                            <AlertOctagon className="w-5 h-5 text-red-600" />
                        </div>
                        <div>
                            <p className="text-sm text-slate-500">Critical</p>
                            <p className="text-2xl font-bold text-red-600">{stats.critical}</p>
                        </div>
                    </div>
                </div>

                <div className="glass-card p-4">
                    <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-xl bg-green-100 dark:bg-green-900/30 flex items-center justify-center">
                            <CheckCircle2 className="w-5 h-5 text-green-600" />
                        </div>
                        <div>
                            <p className="text-sm text-slate-500">Resolved</p>
                            <p className="text-2xl font-bold text-green-600">
                                {alerts.filter(a => a.resolved).length}
                            </p>
                        </div>
                    </div>
                </div>
            </div>

            {/* Filters */}
            <div className="glass-card p-4">
                <div className="flex flex-col sm:flex-row gap-4">
                    <div className="relative flex-1">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
                        <input
                            type="text"
                            placeholder="Search alerts..."
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            className="input pl-10 w-full"
                        />
                    </div>

                    <select
                        value={filterSeverity}
                        onChange={(e) => setFilterSeverity(e.target.value)}
                        className="input w-full sm:w-40"
                    >
                        <option value="all">All Severity</option>
                        <option value="CRITICAL">Critical</option>
                        <option value="WARNING">Warning</option>
                        <option value="INFO">Info</option>
                    </select>

                    <select
                        value={filterStatus}
                        onChange={(e) => setFilterStatus(e.target.value)}
                        className="input w-full sm:w-40"
                    >
                        <option value="all">All Status</option>
                        <option value="active">Active</option>
                        <option value="resolved">Resolved</option>
                    </select>
                </div>
            </div>

            {/* Alert List */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                <div className="lg:col-span-2">
                    <AlertList
                        alerts={sortedAlerts}
                        onResolve={openResolveDialog}
                        onAlertClick={setSelectedAlert}
                        showResolved={true}
                        emptyMessage="No alerts match your filters"
                    />
                </div>

                <div className="space-y-6">
                    <AlertSummary alerts={alerts} />

                    <div className="glass-card p-5">
                        <h3 className="font-semibold text-slate-900 dark:text-white mb-4">
                            Alert Types
                        </h3>
                        <div className="space-y-3">
                            {['PM25_HIGH', 'CO2_HIGH', 'SENSOR_OFFLINE', 'TEMPERATURE_HIGH'].map(type => {
                                const count = alerts.filter(a => a.type === type).length;
                                const percentage = alerts.length ? (count / alerts.length) * 100 : 0;

                                return (
                                    <div key={type} className="space-y-1">
                                        <div className="flex justify-between text-sm">
                      <span className="text-slate-600 dark:text-slate-400">
                        {type.replace(/_/g, ' ')}
                      </span>
                                            <span className="font-medium">{count}</span>
                                        </div>
                                        <div className="h-2 bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
                                            <div
                                                className="h-full bg-air-500 rounded-full transition-all"
                                                style={{ width: `${percentage}%` }}
                                            />
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    </div>

                    <div className="glass-card p-5">
                        <h3 className="font-semibold text-slate-900 dark:text-white mb-4">
                            Quick Actions
                        </h3>
                        <div className="space-y-2">
                            <button className="btn btn-ghost w-full justify-start">
                                <Calendar className="w-4 h-4" />
                                Export Alert History
                            </button>
                            <button className="btn btn-ghost w-full justify-start">
                                <Bell className="w-4 h-4" />
                                Configure Notifications
                            </button>
                            <button className="btn btn-ghost w-full justify-start">
                                <Filter className="w-4 h-4" />
                                Set Alert Thresholds
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            {/* Resolution Notes Dialog */}
            <AnimatePresence>
                {resolveDialogOpen && alertToResolve && (
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4"
                        onClick={closeResolveDialog}
                    >
                        <motion.div
                            initial={{ scale: 0.95, opacity: 0 }}
                            animate={{ scale: 1, opacity: 1 }}
                            exit={{ scale: 0.95, opacity: 0 }}
                            className="bg-white dark:bg-slate-800 rounded-2xl shadow-xl max-w-md w-full p-6"
                            onClick={(e) => e.stopPropagation()}
                        >
                            {/* Header */}
                            <div className="flex items-center justify-between mb-4">
                                <div className="flex items-center gap-3">
                                    <div className="w-10 h-10 rounded-xl bg-green-100 dark:bg-green-900/30 flex items-center justify-center">
                                        <CheckCircle2 className="w-5 h-5 text-green-600" />
                                    </div>
                                    <div>
                                        <h3 className="font-semibold text-slate-900 dark:text-white">
                                            Resolve Alert
                                        </h3>
                                        <p className="text-sm text-slate-500">
                                            {alertToResolve.type?.replace(/_/g, ' ')}
                                        </p>
                                    </div>
                                </div>
                                <button
                                    onClick={closeResolveDialog}
                                    className="p-2 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg transition-colors"
                                >
                                    <X className="w-5 h-5 text-slate-500" />
                                </button>
                            </div>

                            {/* Alert Info */}
                            <div className="bg-slate-50 dark:bg-slate-700/50 rounded-xl p-3 mb-4">
                                <p className="text-sm text-slate-600 dark:text-slate-300">
                                    {alertToResolve.message}
                                </p>
                                <p className="text-xs text-slate-400 mt-1">
                                    Sensor: {alertToResolve.sensorId}
                                </p>
                            </div>

                            {/* Quick Resolution Options */}
                            <div className="mb-4">
                                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">
                                    <MessageSquare className="w-4 h-4 inline mr-1" />
                                    Resolution Notes
                                </label>
                                <div className="flex flex-wrap gap-2 mb-3">
                                    {RESOLUTION_PRESETS.map((preset) => (
                                        <button
                                            key={preset}
                                            onClick={() => setResolutionNotes(preset)}
                                            className={`px-3 py-1.5 text-xs rounded-full border transition-colors ${
                                                resolutionNotes === preset
                                                    ? 'bg-air-500 text-white border-air-500'
                                                    : 'bg-white dark:bg-slate-700 text-slate-600 dark:text-slate-300 border-slate-200 dark:border-slate-600 hover:border-air-300'
                                            }`}
                                        >
                                            {preset}
                                        </button>
                                    ))}
                                </div>
                                <textarea
                                    value={resolutionNotes}
                                    onChange={(e) => setResolutionNotes(e.target.value)}
                                    placeholder="Or enter custom notes..."
                                    className="input w-full h-24 resize-none"
                                />
                            </div>

                            {/* Actions */}
                            <div className="flex gap-3">
                                <button
                                    onClick={closeResolveDialog}
                                    className="btn btn-secondary flex-1"
                                    disabled={isResolving}
                                >
                                    Cancel
                                </button>
                                <button
                                    onClick={handleResolveAlert}
                                    className="btn btn-primary flex-1"
                                    disabled={isResolving}
                                >
                                    {isResolving ? (
                                        <>
                                            <RefreshCw className="w-4 h-4 animate-spin" />
                                            Resolving...
                                        </>
                                    ) : (
                                        <>
                                            <CheckCircle2 className="w-4 h-4" />
                                            Resolve Alert
                                        </>
                                    )}
                                </button>
                            </div>
                        </motion.div>
                    </motion.div>
                )}
            </AnimatePresence>
        </motion.div>
    );
}