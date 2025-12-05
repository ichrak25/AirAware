import React, { useEffect, useRef, useMemo } from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import L from 'leaflet';
import { motion } from 'framer-motion';
import { calculateAQI, formatNumber, getSensorStatus } from '../../utils/helpers';
import { MapPin, Thermometer, Wind, Gauge, Activity } from 'lucide-react';

// Tunisia center coordinates
const TUNISIA_CENTER = [34.0, 9.5];
const TUNISIA_BOUNDS = [
  [30.2, 7.5], // Southwest
  [37.5, 11.6], // Northeast
];

// Custom marker icon creator based on AQI
function createMarkerIcon(aqi, status) {
  const { color } = calculateAQI(aqi || 0);
  const isOffline = status === 'OFFLINE';
  
  const markerColor = isOffline ? '#94a3b8' : color;
  const pulseClass = isOffline ? '' : 'animate-pulse';
  
  return L.divIcon({
    className: 'custom-marker',
    html: `
      <div class="relative">
        <div class="absolute -inset-2 rounded-full ${pulseClass}" 
             style="background: ${markerColor}40;"></div>
        <div class="w-8 h-8 rounded-full flex items-center justify-center shadow-lg border-2 border-white"
             style="background: ${markerColor};">
          <span class="text-white text-xs font-bold">${aqi || '?'}</span>
        </div>
        <div class="absolute -bottom-1 left-1/2 -translate-x-1/2 w-0 h-0 
                    border-l-4 border-r-4 border-t-6 border-l-transparent border-r-transparent"
             style="border-top-color: ${markerColor};"></div>
      </div>
    `,
    iconSize: [32, 40],
    iconAnchor: [16, 40],
    popupAnchor: [0, -40],
  });
}

// Map bounds adjuster component
function MapBoundsHandler({ sensors }) {
  const map = useMap();
  
  useEffect(() => {
    if (sensors && sensors.length > 0) {
      const validSensors = sensors.filter(s => s.location?.latitude && s.location?.longitude);
      if (validSensors.length > 0) {
        const bounds = validSensors.map(s => [s.location.latitude, s.location.longitude]);
        map.fitBounds(bounds, { padding: [50, 50], maxZoom: 10 });
      }
    }
  }, [sensors, map]);
  
  return null;
}

/**
 * Sensor Popup Content
 */
function SensorPopup({ sensor, reading }) {
  const status = getSensorStatus(sensor.status);
  const { aqi, category, color } = calculateAQI(reading?.pm25);
  
  return (
    <div className="min-w-[280px] p-4 -m-3">
      {/* Header */}
      <div className="flex items-start justify-between mb-3">
        <div>
          <h3 className="font-display font-semibold text-slate-900 dark:text-white">
            {sensor.deviceId}
          </h3>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            {sensor.description}
          </p>
        </div>
        <span 
          className="px-2 py-1 rounded-full text-xs font-medium"
          style={{ backgroundColor: `${status.color}20`, color: status.color }}
        >
          {status.label}
        </span>
      </div>
      
      {/* Location */}
      <div className="flex items-center gap-1.5 text-sm text-slate-500 dark:text-slate-400 mb-4">
        <MapPin className="w-4 h-4" />
        <span>{sensor.location?.city}, {sensor.location?.country}</span>
      </div>
      
      {/* AQI */}
      {reading && (
        <>
          <div 
            className="rounded-xl p-3 mb-4"
            style={{ backgroundColor: `${color}15` }}
          >
            <div className="flex items-center justify-between">
              <div>
                <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">
                  Air Quality
                </span>
                <div className="flex items-baseline gap-2">
                  <span 
                    className="text-3xl font-display font-bold"
                    style={{ color }}
                  >
                    {aqi}
                  </span>
                  <span className="text-sm font-medium" style={{ color }}>
                    {category}
                  </span>
                </div>
              </div>
              <Activity className="w-8 h-8" style={{ color }} />
            </div>
          </div>
          
          {/* Metrics */}
          <div className="grid grid-cols-2 gap-3">
            <div className="flex items-center gap-2">
              <Thermometer className="w-4 h-4 text-red-500" />
              <div>
                <span className="text-xs text-slate-400">Temp</span>
                <p className="font-semibold">{formatNumber(reading.temperature, 1)}°C</p>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <Wind className="w-4 h-4 text-violet-500" />
              <div>
                <span className="text-xs text-slate-400">CO₂</span>
                <p className="font-semibold">{formatNumber(reading.co2, 0)} ppm</p>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <Gauge className="w-4 h-4 text-green-500" />
              <div>
                <span className="text-xs text-slate-400">PM2.5</span>
                <p className="font-semibold">{formatNumber(reading.pm25, 1)} µg/m³</p>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <Gauge className="w-4 h-4 text-cyan-500" />
              <div>
                <span className="text-xs text-slate-400">PM10</span>
                <p className="font-semibold">{formatNumber(reading.pm10, 1)} µg/m³</p>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}

/**
 * Sensor Map Component
 * Interactive map showing all sensors with AQI-colored markers
 */
export default function SensorMap({ 
  sensors = [], 
  readings = {},
  selectedSensor,
  onSensorSelect,
  height = '400px',
  showControls = true,
}) {
  const mapRef = useRef(null);
  
  // Memoize markers to prevent unnecessary rerenders
  const markers = useMemo(() => {
    return sensors.map(sensor => {
      const reading = readings[sensor.deviceId];
      const aqi = reading ? calculateAQI(reading.pm25).aqi : null;
      
      return {
        sensor,
        reading,
        position: [sensor.location?.latitude || 0, sensor.location?.longitude || 0],
        icon: createMarkerIcon(aqi, sensor.status),
      };
    }).filter(m => m.position[0] !== 0 && m.position[1] !== 0);
  }, [sensors, readings]);
  
  return (
    <motion.div
      className="glass-card overflow-hidden"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5 }}
      style={{ height }}
    >
      <MapContainer
        ref={mapRef}
        center={TUNISIA_CENTER}
        zoom={7}
        style={{ height: '100%', width: '100%' }}
        zoomControl={showControls}
        attributionControl={false}
      >
        {/* Map tiles - using CartoDB Positron for clean look */}
        <TileLayer
          url="https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png"
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> &copy; <a href="https://carto.com/attributions">CARTO</a>'
        />
        
        {/* Dark mode alternative */}
        {/* <TileLayer
          url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
        /> */}
        
        {/* Bounds handler */}
        <MapBoundsHandler sensors={sensors} />
        
        {/* Sensor markers */}
        {markers.map(({ sensor, reading, position, icon }) => (
          <Marker
            key={sensor.id}
            position={position}
            icon={icon}
            eventHandlers={{
              click: () => onSensorSelect?.(sensor),
            }}
          >
            <Popup>
              <SensorPopup sensor={sensor} reading={reading} />
            </Popup>
          </Marker>
        ))}
      </MapContainer>
      
      {/* Map Legend */}
      <div className="absolute bottom-4 left-4 z-[1000] glass-card p-3">
        <h4 className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-2">
          Air Quality Index
        </h4>
        <div className="flex flex-col gap-1.5">
          {[
            { label: 'Good (0-50)', color: '#22c55e' },
            { label: 'Moderate (51-100)', color: '#eab308' },
            { label: 'Unhealthy (101-150)', color: '#f97316' },
            { label: 'Very Unhealthy (151+)', color: '#ef4444' },
          ].map(item => (
            <div key={item.label} className="flex items-center gap-2">
              <div 
                className="w-3 h-3 rounded-full" 
                style={{ backgroundColor: item.color }}
              />
              <span className="text-xs text-slate-600 dark:text-slate-300">
                {item.label}
              </span>
            </div>
          ))}
        </div>
      </div>
    </motion.div>
  );
}

/**
 * Mini Map for cards
 */
export function MiniMap({ latitude, longitude, size = 150 }) {
  if (!latitude || !longitude) return null;
  
  return (
    <div 
      className="rounded-xl overflow-hidden"
      style={{ width: size, height: size }}
    >
      <MapContainer
        center={[latitude, longitude]}
        zoom={12}
        style={{ height: '100%', width: '100%' }}
        zoomControl={false}
        attributionControl={false}
        dragging={false}
        scrollWheelZoom={false}
      >
        <TileLayer
          url="https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png"
        />
        <Marker 
          position={[latitude, longitude]}
          icon={L.divIcon({
            className: 'custom-marker',
            html: `<div class="w-4 h-4 rounded-full bg-air-500 border-2 border-white shadow-lg"></div>`,
            iconSize: [16, 16],
            iconAnchor: [8, 8],
          })}
        />
      </MapContainer>
    </div>
  );
}
