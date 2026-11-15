/*
 * TriVoKo service worker (after launch) = a small helper the phone keeps for the shop, so it can be installed
 * like an app and still opens when the network drops.
 *
 *   pages (/, /products/...)  network first; offline -> the saved app shell (React then shows its own messages)
 *   /assets/... (hashed)      cache first - a new build has new file names, so these never go stale
 *   /api/...                  NEVER cached: prices, stock, orders and logins must always be live
 *
 * Change VERSION to drop the old caches on the next visit.
 */
const VERSION = 'trivoko-v1'
const SHELL = '/'

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(VERSION).then((cache) => cache.add(SHELL)).then(() => self.skipWaiting()))
})

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== VERSION).map((k) => caches.delete(k))))
      .then(() => self.clients.claim()),
  )
})

self.addEventListener('fetch', (event) => {
  const request = event.request
  const url = new URL(request.url)
  if (request.method !== 'GET' || url.origin !== self.location.origin || url.pathname.startsWith('/api/')) {
    return // the browser handles it as if there were no service worker
  }
  if (request.mode === 'navigate') {
    event.respondWith(
      fetch(request)
        .then((response) => {
          const copy = response.clone()
          if (response.ok) caches.open(VERSION).then((cache) => cache.put(SHELL, copy))
          return response
        })
        .catch(() => caches.match(SHELL)),
    )
    return
  }
  if (url.pathname.startsWith('/assets/')) {
    event.respondWith(
      caches.match(request).then((hit) => hit ?? fetch(request).then((response) => {
        const copy = response.clone()
        if (response.ok) caches.open(VERSION).then((cache) => cache.put(request, copy))
        return response
      })),
    )
  }
})
