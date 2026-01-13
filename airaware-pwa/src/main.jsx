import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './index.css';

// Register service worker for PWA and Push Notifications
// IMPORTANT: Only ONE service worker should be registered for push to work on mobile
if ('serviceWorker' in navigator) {
  window.addEventListener('load', async () => {
    try {
      // Register the push notification service worker (also handles caching)
      // This enables notifications even when the app is closed on mobile & desktop
      const registration = await navigator.serviceWorker.register('/sw-push.js', {
        scope: '/'
      });
      
      console.log('[PWA] Service worker registered:', registration.scope);
      
      // Check for updates
      registration.addEventListener('updatefound', () => {
        console.log('[PWA] New service worker available');
      });
      
    } catch (error) {
      console.error('[PWA] Service worker registration failed:', error);
    }
  });
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
