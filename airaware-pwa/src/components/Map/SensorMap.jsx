import React, { useEffect, useRef, useMemo } from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import L from 'leaflet';
import { motion } from 'framer-motion';
import { calculateAQI, formatNumber, getSensorStatus } from '../../utils/helpers';
import { MapPin, Thermometer, Wind, Gauge, Activity, AlertTriangle } from 'lucide-react';

// Fix Leaflet default marker icon issue with Vite/Webpack
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png';
import markerIcon from 'leaflet/dist/images/marker-icon.png';
import markerShadow from 'leaflet/dist/images/marker-shadow.png';

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
    iconRetinaUrl: markerIcon2x,
    iconUrl: markerIcon,
    shadowUrl: markerShadow,
});

// Tunisia center coordinates
const TUNISIA_CENTER = [34.0, 9.5];

// Fallback coordinates for known Tunisian cities (when API doesn't provide location)
const CITY_COORDINATES = {
    'TUNIS': { latitude: 36.8065, longitude: 10.1815, city: 'Tunis' },
    'SFAX': { latitude: 34.7406, longitude: 10.7603, city: 'Sfax' },
    'SOUSSE': { latitude: 35.8256, longitude: 10.6369, city: 'Sousse' },
    'BIZERTE': { latitude: 37.2768, longitude: 9.8642, city: 'Bizerte' },
    'GABES': { latitude: 33.8881, longitude: 10.0975, city: 'Gabes' },
    'KAIROUAN': { latitude: 35.6781, longitude: 10.0963, city: 'Kairouan' },
    'MONASTIR': { latitude: 35.7643, longitude: 10.8113, city: 'Monastir' },
};

// Stable offsets for each sensor index (to prevent markers from jumping around on re-render)
const SENSOR_OFFSETS = [
    { lat: 0, lng: 0 },
    { lat: 0.008, lng: 0.008 },
    { lat: -0.008, lng: 0.008 },
    { lat: 0.008, lng: -0.008 },
    { lat: -0.008, lng: -0.008 },
    { lat: 0.012, lng: 0 },
    { lat: -0.012, lng: 0 },
    { lat: 0, lng: 0.012 },
    { lat: 0, lng: -0.012 },
];

// Try to extract city from sensor ID (e.g., "SENSOR_TUNIS_001" -> "TUNIS")
function extractCityFromSensorId(sensorId) {
    if (!sensorId) return null;
    const upperSensorId = sensorId.toUpperCase();
    for (const city of Object.keys(CITY_COORDINATES)) {
        if (upperSensorId.includes(city)) {
            return city;
        }
    }
    return null;
}

// Get coordinates for a sensor, with fallback logic
function getSensorCoordinates(sensor, index = 0) {
    // 1. Try to use provided location
    if (sensor.location?.latitude && sensor.location?.longitude) {
        return {
            latitude: sensor.location.latitude,
            longitude: sensor.location.longitude,
            city: sensor.location.city || 'Unknown',
            source: 'api'
        };
    }

    // 2. Try to extract city from sensor ID and use fallback coordinates
    const sensorId = sensor.deviceId || sensor.sensorId || sensor.id || '';
    const city = extractCityFromSensorId(sensorId);

    if (city && CITY_COORDINATES[city]) {
        const coords = CITY_COORDINATES[city];
        // Use stable offset based on index to spread out markers in same city
        const offset = SENSOR_OFFSETS[index % SENSOR_OFFSETS.length];
        return {
            latitude: coords.latitude + offset.lat,
            longitude: coords.longitude + offset.lng,
            city: coords.city,
            source: 'fallback'
        };
    }

    // 3. No valid coordinates found
    console.warn(`[SensorMap] No coordinates for sensor: ${sensorId}`);
    return null;
}

// Custom marker icon creator based on AQI
function createMarkerIcon(aqi, status, source) {
    const { color } = calculateAQI(aqi || 0);
    const isOffline = status === 'OFFLINE';
    const isFallback = source === 'fallback';

    const markerColor = isOffline ? '#94a3b8' : color;
    // Dashed border for fallback locations
    const borderStyle = isFallback
        ? 'border: 2px dashed rgba(255,255,255,0.8);'
        : 'border: 2px solid white;';

    return L.divIcon({
        className: 'custom-aqi-marker',
        html: `
      <div style="position: relative; width: 32px; height: 40px;">
        <div style="
          position: absolute;
          top: -4px;
          left: -4px;
          right: -4px;
          bottom: 4px;
          border-radius: 50%;
          background: ${markerColor}40;
          ${!isOffline ? 'animation: marker-pulse 2s ease-in-out infinite;' : ''}
        "></div>
        <div style="
          width: 32px;
          height: 32px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          background: ${markerColor};
          ${borderStyle}
          box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);
        ">
          <span style="color: white; font-size: 11px; font-weight: 700;">${aqi || '?'}</span>
        </div>
        <div style="
          position: absolute;
          bottom: 0;
          left: 50%;
          transform: translateX(-50%);
          width: 0;
          height: 0;
          border-left: 6px solid transparent;
          border-right: 6px solid transparent;
          border-top: 8px solid ${markerColor};
        "></div>
      </div>
    `,
        iconSize: [32, 40],
        iconAnchor: [16, 40],
        popupAnchor: [0, -40],
    });
}

// Map bounds adjuster component
function MapBoundsHandler({ markers }) {
    const map = useMap();

    useEffect(() => {
        if (markers && markers.length > 0) {
            const bounds = markers.map(m => m.position);
            console.log('[SensorMap] Fitting bounds to', bounds.length, 'markers');
            map.fitBounds(bounds, { padding: [50, 50], maxZoom: 9 });
        } else {
            // If no markers, show all of Tunisia
            console.log('[SensorMap] No markers, showing Tunisia overview');
            map.setView(TUNISIA_CENTER, 6);
        }
    }, [markers, map]);

    return null;
}

/**
 * Sensor Popup Content
 */
function SensorPopup({ sensor, reading, coordinates }) {
    const status = getSensorStatus(sensor.status);
    const { aqi, category, color } = calculateAQI(reading?.pm25);

    return (
        <div className="min-w-[280px] p-4 -m-3">
            {/* Header */}
            <div className="flex items-start justify-between mb-3">
                <div>
                    <h3 className="font-semibold text-slate-900 dark:text-white">
                        {sensor.deviceId || sensor.sensorId || sensor.name || 'Unknown Sensor'}
                    </h3>
                    <p className="text-sm text-slate-500 dark:text-slate-400">
                        {sensor.description || sensor.name || 'Air Quality Sensor'}
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
            <div className="flex items-center gap-1.5 text-sm text-slate-500 dark:text-slate-400 mb-2">
                <MapPin className="w-4 h-4" />
                <span>{coordinates?.city || 'Unknown'}, Tunisia</span>
            </div>

            {/* Fallback location warning */}
            {coordinates?.source === 'fallback' && (
                <div className="flex items-center gap-1.5 text-xs text-amber-600 dark:text-amber-400 mb-4 bg-amber-50 dark:bg-amber-900/20 px-2 py-1 rounded">
                    <AlertTriangle className="w-3 h-3" />
                    <span>Approximate location</span>
                </div>
            )}

            {/* AQI */}
            {reading ? (
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
                      className="text-3xl font-bold"
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
                                <p className="font-semibold text-slate-900 dark:text-white">{formatNumber(reading.temperature, 1)}°C</p>
                            </div>
                        </div>
                        <div className="flex items-center gap-2">
                            <Wind className="w-4 h-4 text-violet-500" />
                            <div>
                                <span className="text-xs text-slate-400">CO₂</span>
                                <p className="font-semibold text-slate-900 dark:text-white">{formatNumber(reading.co2, 0)} ppm</p>
                            </div>
                        </div>
                        <div className="flex items-center gap-2">
                            <Gauge className="w-4 h-4 text-green-500" />
                            <div>
                                <span className="text-xs text-slate-400">PM2.5</span>
                                <p className="font-semibold text-slate-900 dark:text-white">{formatNumber(reading.pm25, 1)} µg/m³</p>
                            </div>
                        </div>
                        <div className="flex items-center gap-2">
                            <Gauge className="w-4 h-4 text-cyan-500" />
                            <div>
                                <span className="text-xs text-slate-400">PM10</span>
                                <p className="font-semibold text-slate-900 dark:text-white">{formatNumber(reading.pm10, 1)} µg/m³</p>
                            </div>
                        </div>
                    </div>
                </>
            ) : (
                <div className="text-center py-4 text-slate-400">
                    <Activity className="w-8 h-8 mx-auto mb-2 opacity-50" />
                    <p className="text-sm">No readings available</p>
                </div>
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

    // Process sensors and create markers with coordinates (including fallbacks)
    const markers = useMemo(() => {
        console.log('[SensorMap] Processing', sensors.length, 'sensors');
        console.log('[SensorMap] Readings available for:', Object.keys(readings));

        const processedMarkers = sensors.map((sensor, index) => {
            // Get coordinates with fallback
            const coordinates = getSensorCoordinates(sensor, index);

            if (!coordinates) {
                return null;
            }

            // Get reading - try multiple ID formats
            const sensorId = sensor.deviceId || sensor.sensorId || sensor.id;
            const reading = readings[sensorId] || readings[sensor.deviceId] || readings[sensor.sensorId];
            const aqi = reading ? calculateAQI(reading.pm25).aqi : null;

            console.log(`[SensorMap] Sensor ${sensorId}: coords=${coordinates.latitude.toFixed(4)},${coordinates.longitude.toFixed(4)} (${coordinates.source}), AQI=${aqi || 'N/A'}`);

            return {
                sensor,
                reading,
                coordinates,
                position: [coordinates.latitude, coordinates.longitude],
                icon: createMarkerIcon(aqi, sensor.status, coordinates.source),
                key: sensor.id || sensorId || `sensor-${index}`,
            };
        }).filter(m => m !== null);

        console.log('[SensorMap] Created', processedMarkers.length, 'markers from', sensors.length, 'sensors');
        return processedMarkers;
    }, [sensors, readings]);

    return (
        <motion.div
            className="glass-card overflow-hidden relative"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5 }}
            style={{ height }}
        >
            <MapContainer
                ref={mapRef}
                center={TUNISIA_CENTER}
                zoom={6}
                style={{ height: '100%', width: '100%' }}
                zoomControl={showControls}
                attributionControl={false}
            >
                {/* Map tiles */}
                <TileLayer
                    url="https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png"
                    attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> &copy; <a href="https://carto.com/attributions">CARTO</a>'
                />

                {/* Bounds handler - receives processed markers */}
                <MapBoundsHandler markers={markers} />

                {/* Sensor markers */}
                {markers.map(({ sensor, reading, coordinates, position, icon, key }) => (
                    <Marker
                        key={key}
                        position={position}
                        icon={icon}
                        eventHandlers={{
                            click: () => onSensorSelect?.(sensor),
                        }}
                    >
                        <Popup>
                            <SensorPopup sensor={sensor} reading={reading} coordinates={coordinates} />
                        </Popup>
                    </Marker>
                ))}
            </MapContainer>

            {/* Map Legend */}
            <div className="absolute bottom-4 left-4 z-[1000] bg-white/90 dark:bg-slate-800/90 backdrop-blur-sm rounded-xl p-3 shadow-lg border border-slate-200 dark:border-slate-700">
                <h4 className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">
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
                                className="w-3 h-3 rounded-full flex-shrink-0"
                                style={{ backgroundColor: item.color }}
                            />
                            <span className="text-xs text-slate-600 dark:text-slate-300">
                {item.label}
              </span>
                        </div>
                    ))}
                </div>
            </div>

            {/* Warning when no markers could be created */}
            {markers.length === 0 && sensors.length > 0 && (
                <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 z-[1000] bg-amber-50 dark:bg-amber-900/90 border border-amber-200 dark:border-amber-800 rounded-xl px-4 py-3 shadow-lg text-center max-w-xs">
                    <AlertTriangle className="w-8 h-8 text-amber-600 mx-auto mb-2" />
                    <p className="text-sm font-medium text-amber-800 dark:text-amber-200">
                        Unable to display sensors
                    </p>
                    <p className="text-xs text-amber-600 dark:text-amber-400 mt-1">
                        {sensors.length} sensors found but coordinates could not be determined
                    </p>
                </div>
            )}
        </motion.div>
    );
}

/**
 * Mini Map for cards
 */
export function MiniMap({ latitude, longitude, size = 150 }) {
    if (!latitude || !longitude) return null;

    const miniMarkerIcon = L.divIcon({
        className: 'custom-mini-marker',
        html: `<div style="
      width: 16px;
      height: 16px;
      border-radius: 50%;
      background: #14b8a6;
      border: 2px solid white;
      box-shadow: 0 2px 4px rgba(0,0,0,0.2);
    "></div>`,
        iconSize: [16, 16],
        iconAnchor: [8, 8],
    });

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
                    icon={miniMarkerIcon}
                />
            </MapContainer>
        </div>
    );
}