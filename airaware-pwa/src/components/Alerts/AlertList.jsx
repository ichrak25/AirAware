import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  AlertTriangle, 
  AlertOctagon, 
  Info, 
  CheckCircle,
  X,
  Clock,
  MapPin,
  ChevronRight,
} from 'lucide-react';
import { formatRelativeTime, getAlertSeverity } from '../../utils/helpers';

/**
 * Single Alert Item
 */
function AlertItem({ alert, onResolve, onDismiss, onClick, compact = false }) {
  const severity = getAlertSeverity(alert.severity);
  
  const SeverityIcon = {
    INFO: Info,
    WARNING: AlertTriangle,
    CRITICAL: AlertOctagon,
    DANGER: AlertOctagon,
  }[alert.severity] || Info;
  
  if (compact) {
    return (
      <motion.div
        layout
        initial={{ opacity: 0, x: -20 }}
        animate={{ opacity: 1, x: 0 }}
        exit={{ opacity: 0, x: 20 }}
        className={`
          flex items-center gap-3 p-3 rounded-xl border cursor-pointer
          ${alert.resolved 
            ? 'bg-slate-50 dark:bg-slate-800/50 border-slate-200 dark:border-slate-700 opacity-60' 
            : severity.bgClass + ' border-current/20'
          }
          hover:scale-[1.02] transition-transform
        `}
        onClick={onClick}
      >
        <SeverityIcon 
          className="w-5 h-5 flex-shrink-0" 
          style={{ color: alert.resolved ? '#94a3b8' : severity.color }} 
        />
        <div className="flex-1 min-w-0">
          <p className={`text-sm font-medium truncate ${alert.resolved ? 'text-slate-500' : ''}`}>
            {alert.message}
          </p>
          <p className="text-xs text-slate-400">{formatRelativeTime(alert.triggeredAt)}</p>
        </div>
        {alert.resolved && (
          <CheckCircle className="w-4 h-4 text-green-500 flex-shrink-0" />
        )}
      </motion.div>
    );
  }
  
  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -20 }}
      className={`
        glass-card p-4 border-l-4
        ${alert.resolved ? 'opacity-60' : ''}
      `}
      style={{ borderLeftColor: alert.resolved ? '#94a3b8' : severity.color }}
    >
      {/* Header */}
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-start gap-3">
          <div 
            className="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0"
            style={{ backgroundColor: `${severity.color}20` }}
          >
            <SeverityIcon className="w-5 h-5" style={{ color: severity.color }} />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span 
                className="text-xs font-semibold uppercase tracking-wider px-2 py-0.5 rounded"
                style={{ backgroundColor: `${severity.color}20`, color: severity.color }}
              >
                {alert.type.replace(/_/g, ' ')}
              </span>
              <span 
                className="text-xs font-medium px-2 py-0.5 rounded-full"
                style={{ backgroundColor: `${severity.color}15`, color: severity.color }}
              >
                {severity.label}
              </span>
            </div>
            <p className="text-slate-900 dark:text-white font-medium mt-1">
              {alert.message}
            </p>
          </div>
        </div>
        
        {/* Actions */}
        {!alert.resolved && onDismiss && (
          <button
            onClick={(e) => { e.stopPropagation(); onDismiss(alert.id); }}
            className="p-1 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg transition-colors"
          >
            <X className="w-4 h-4 text-slate-400" />
          </button>
        )}
      </div>
      
      {/* Meta */}
      <div className="flex items-center gap-4 mt-3 text-sm text-slate-500">
        <div className="flex items-center gap-1">
          <Clock className="w-4 h-4" />
          <span>{formatRelativeTime(alert.triggeredAt)}</span>
        </div>
        <div className="flex items-center gap-1">
          <MapPin className="w-4 h-4" />
          <span>{alert.sensorId}</span>
        </div>
      </div>
      
      {/* Actions Row */}
      <div className="flex items-center justify-between mt-4 pt-3 border-t border-slate-200 dark:border-slate-700">
        {alert.resolved ? (
          <div className="flex items-center gap-2 text-green-600">
            <CheckCircle className="w-4 h-4" />
            <span className="text-sm font-medium">Resolved</span>
          </div>
        ) : (
          <>
            <button
              onClick={(e) => { e.stopPropagation(); onResolve?.(alert.id); }}
              className="btn btn-primary text-sm py-2"
            >
              <CheckCircle className="w-4 h-4" />
              Resolve
            </button>
            <button
              onClick={onClick}
              className="btn btn-ghost text-sm py-2"
            >
              View Details
              <ChevronRight className="w-4 h-4" />
            </button>
          </>
        )}
      </div>
    </motion.div>
  );
}

/**
 * Alert List Component
 */
export default function AlertList({
  alerts = [],
  onResolve,
  onDismiss,
  onAlertClick,
  showResolved = true,
  compact = false,
  maxItems,
  emptyMessage = 'No alerts',
}) {
  const filteredAlerts = showResolved
    ? alerts
    : alerts.filter(a => !a.resolved);
  
  const displayAlerts = maxItems
    ? filteredAlerts.slice(0, maxItems)
    : filteredAlerts;
  
  if (!displayAlerts.length) {
    return (
      <div className="flex flex-col items-center justify-center py-12 text-slate-400">
        <CheckCircle className="w-12 h-12 mb-3 text-green-500" />
        <p className="text-lg font-medium">{emptyMessage}</p>
        <p className="text-sm">All systems operating normally</p>
      </div>
    );
  }
  
  return (
    <div className={compact ? 'space-y-2' : 'space-y-4'}>
      <AnimatePresence mode="popLayout">
        {displayAlerts.map((alert) => (
          <AlertItem
            key={alert.id}
            alert={alert}
            onResolve={onResolve}
            onDismiss={onDismiss}
            onClick={() => onAlertClick?.(alert)}
            compact={compact}
          />
        ))}
      </AnimatePresence>
      
      {maxItems && filteredAlerts.length > maxItems && (
        <div className="text-center">
          <button className="btn btn-ghost text-sm">
            View all {filteredAlerts.length} alerts
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>
      )}
    </div>
  );
}

/**
 * Alert Summary Card
 */
export function AlertSummary({ alerts = [] }) {
  const unresolvedCount = alerts.filter(a => !a.resolved).length;
  const criticalCount = alerts.filter(a => !a.resolved && a.severity === 'CRITICAL').length;
  const warningCount = alerts.filter(a => !a.resolved && a.severity === 'WARNING').length;
  
  return (
    <div className="glass-card p-5">
      <div className="flex items-center justify-between mb-4">
        <h3 className="font-semibold text-slate-900 dark:text-white">Alert Summary</h3>
        <span 
          className={`
            px-3 py-1 rounded-full text-sm font-semibold
            ${unresolvedCount > 0 
              ? 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400'
              : 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'
            }
          `}
        >
          {unresolvedCount} Active
        </span>
      </div>
      
      <div className="grid grid-cols-3 gap-4">
        <div className="text-center">
          <div className="text-2xl font-bold text-red-600">{criticalCount}</div>
          <div className="text-xs text-slate-500 uppercase tracking-wider">Critical</div>
        </div>
        <div className="text-center">
          <div className="text-2xl font-bold text-yellow-600">{warningCount}</div>
          <div className="text-xs text-slate-500 uppercase tracking-wider">Warning</div>
        </div>
        <div className="text-center">
          <div className="text-2xl font-bold text-green-600">
            {alerts.filter(a => a.resolved).length}
          </div>
          <div className="text-xs text-slate-500 uppercase tracking-wider">Resolved</div>
        </div>
      </div>
    </div>
  );
}

/**
 * Alert Badge - shows count with indicator
 */
export function AlertBadge({ count, className = '' }) {
  if (count === 0) return null;
  
  return (
    <span className={`
      inline-flex items-center justify-center
      min-w-[20px] h-5 px-1.5 rounded-full
      text-xs font-bold text-white bg-red-500
      ${className}
    `}>
      {count > 99 ? '99+' : count}
    </span>
  );
}
