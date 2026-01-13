import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { VitePWA } from 'vite-plugin-pwa';

export default defineConfig({
    plugins: [
        react(),
        VitePWA({
            registerType: 'autoUpdate',
            includeAssets: [
                'favicon.svg',
                'apple-touch-icon.png',
                'pwa-72x72.png',
                'pwa-96x96.png',
                'pwa-128x128.png',
                'pwa-144x144.png',
                'pwa-152x152.png',
                'pwa-192x192.png',
                'pwa-384x384.png',
                'pwa-512x512.png',
                'pwa-maskable-512x512.png'
            ],
            manifest: {
                name: 'AirAware - Air Quality Monitor',
                short_name: 'AirAware',
                description: 'Real-time air quality monitoring system for Tunisia. Track PM2.5, CO2, temperature, humidity and more.',
                theme_color: '#0f766e',
                background_color: '#f0fdfa',
                display: 'standalone',
                orientation: 'any',
                scope: '/',
                start_url: '/',
                id: '/airaware-pwa',
                categories: ['utilities', 'health', 'weather'],
                lang: 'en',
                dir: 'ltr',
                icons: [
                    { src: 'pwa-72x72.png', sizes: '72x72', type: 'image/png' },
                    { src: 'pwa-96x96.png', sizes: '96x96', type: 'image/png' },
                    { src: 'pwa-128x128.png', sizes: '128x128', type: 'image/png' },
                    { src: 'pwa-144x144.png', sizes: '144x144', type: 'image/png' },
                    { src: 'pwa-152x152.png', sizes: '152x152', type: 'image/png' },
                    { src: 'pwa-192x192.png', sizes: '192x192', type: 'image/png' },
                    { src: 'pwa-384x384.png', sizes: '384x384', type: 'image/png' },
                    { src: 'pwa-512x512.png', sizes: '512x512', type: 'image/png' },
                    { src: 'pwa-maskable-512x512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' }
                ],
                shortcuts: [
                    {
                        name: 'Dashboard',
                        short_name: 'Dashboard',
                        description: 'View air quality dashboard',
                        url: '/',
                        icons: [{ src: 'pwa-96x96.png', sizes: '96x96' }]
                    },
                    {
                        name: 'Sensor Map',
                        short_name: 'Map',
                        description: 'View sensor locations',
                        url: '/map',
                        icons: [{ src: 'pwa-96x96.png', sizes: '96x96' }]
                    },
                    {
                        name: 'Alerts',
                        short_name: 'Alerts',
                        description: 'View active alerts',
                        url: '/alerts',
                        icons: [{ src: 'pwa-96x96.png', sizes: '96x96' }]
                    }
                ]
            },
            workbox: {
                globPatterns: ['**/*.{js,css,html,ico,png,svg,woff2,woff,ttf}'],
                cleanupOutdatedCaches: true,
                skipWaiting: true,
                clientsClaim: true,
                // Don't cache navigation requests
                navigateFallback: null,
                runtimeCaching: [
                    // Google Fonts - CSS
                    {
                        urlPattern: /^https:\/\/fonts\.googleapis\.com\/.*/i,
                        handler: 'StaleWhileRevalidate',
                        options: {
                            cacheName: 'google-fonts-stylesheets',
                            expiration: {
                                maxEntries: 10,
                                maxAgeSeconds: 60 * 60 * 24 * 365 // 1 year
                            }
                        }
                    },
                    // Google Fonts - Font files
                    {
                        urlPattern: /^https:\/\/fonts\.gstatic\.com\/.*/i,
                        handler: 'CacheFirst',
                        options: {
                            cacheName: 'google-fonts-webfonts',
                            expiration: {
                                maxEntries: 30,
                                maxAgeSeconds: 60 * 60 * 24 * 365 // 1 year
                            },
                            cacheableResponse: {
                                statuses: [0, 200]
                            }
                        }
                    },
                    // Unpkg (Leaflet) - FIXED: Use StaleWhileRevalidate and allow opaque responses
                    {
                        urlPattern: /^https:\/\/unpkg\.com\/.*/i,
                        handler: 'StaleWhileRevalidate',
                        options: {
                            cacheName: 'unpkg-cache',
                            expiration: {
                                maxEntries: 30,
                                maxAgeSeconds: 60 * 60 * 24 * 30 // 30 days
                            },
                            cacheableResponse: {
                                statuses: [0, 200] // 0 allows opaque responses
                            }
                        }
                    },
                    // CartoDB map tiles
                    {
                        urlPattern: /^https:\/\/.*\.basemaps\.cartocdn\.com\/.*/i,
                        handler: 'CacheFirst',
                        options: {
                            cacheName: 'map-tiles-cache',
                            expiration: {
                                maxEntries: 500,
                                maxAgeSeconds: 60 * 60 * 24 * 30 // 30 days
                            },
                            cacheableResponse: {
                                statuses: [0, 200]
                            }
                        }
                    },
                    // OpenStreetMap tiles (backup)
                    {
                        urlPattern: /^https:\/\/.*\.tile\.openstreetmap\.org\/.*/i,
                        handler: 'CacheFirst',
                        options: {
                            cacheName: 'osm-tiles-cache',
                            expiration: {
                                maxEntries: 500,
                                maxAgeSeconds: 60 * 60 * 24 * 30
                            },
                            cacheableResponse: {
                                statuses: [0, 200]
                            }
                        }
                    },
                    // API calls - Network first with fallback
                    {
                        urlPattern: /\/api\/.*/i,
                        handler: 'NetworkFirst',
                        options: {
                            cacheName: 'api-cache',
                            expiration: {
                                maxEntries: 100,
                                maxAgeSeconds: 60 * 5 // 5 minutes
                            },
                            cacheableResponse: {
                                statuses: [200] // Only cache successful responses
                            },
                            networkTimeoutSeconds: 10
                        }
                    }
                ]
            },
            devOptions: {
                enabled: false // Disable PWA in development to avoid caching issues
            }
        })
    ],
    server: {
        port: 5173,
        host: true,
        proxy: {
            '/api': {
                target: 'http://localhost:8080/api-1.0-SNAPSHOT',
                changeOrigin: true
            },
            '/iam/api': {
                target: 'http://localhost:8080/iam-1.0-SNAPSHOT',
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/iam\/api/, '/api')
            },
            '/ml': {
                target: 'http://localhost:5000',
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/ml/, '')
            }
        }
    },
    preview: {
        port: 4173,
        host: true
    }
});