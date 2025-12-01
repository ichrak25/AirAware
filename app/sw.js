const CACHE_NAME = "airaware-v1"
const URLS_TO_CACHE = ["/", "/index.html", "/styles.css", "/app.js", "/manifest.json"]

// Install
self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(URLS_TO_CACHE).catch(() => {
        console.log("[v0] Some files failed to cache")
      })
    }),
  )
  self.skipWaiting()
})

// Activate
self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames.map((cacheName) => {
          if (cacheName !== CACHE_NAME) {
            return caches.delete(cacheName)
          }
        }),
      )
    }),
  )
  self.clients.claim()
})

// Fetch - Stale While Revalidate
self.addEventListener("fetch", (event) => {
  if (event.request.method !== "GET") {
    return
  }

  event.respondWith(
    caches
      .open(CACHE_NAME)
      .then((cache) => {
        return cache.match(event.request).then((response) => {
          const fetchPromise = fetch(event.request)
            .then((res) => {
              if (res && res.status === 200) {
                cache.put(event.request, res.clone())
              }
              return res
            })
            .catch(() => response)

          return response || fetchPromise
        })
      })
      .catch(() => {
        return caches.match("/index.html")
      }),
  )
})
