const CACHE_NAME = 'zaviro-pwa-v5';
const STATIC_ASSETS = [
  './',
  './index.html',
  './manifest.json',
  './images/zaviro_logo.jpg?v=5',
  './images/zaviro_hero_banner.jpg',
  './images/zaviro_creamy_pasta.jpg',
  './images/zaviro_wrap.jpg',
  './images/zaviro_crispy_chicken.jpg',
  './images/zaviro_wings.jpg',
  './images/zaviro_loaded_fries.jpg'
];

self.addEventListener('install', (event) => {
  self.skipWaiting();
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(STATIC_ASSETS).catch(() => {});
    })
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(
        keys.map((key) => {
          if (key !== CACHE_NAME) {
            return caches.delete(key);
          }
        })
      )
    ).then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const req = event.request;
  if (req.method !== 'GET') return;

  if (req.url.includes('firestore.googleapis.com') || req.url.includes('gstatic.com')) {
    return;
  }

  event.respondWith(
    fetch(req)
      .then((res) => {
        if (res && res.status === 200 && res.type === 'basic') {
          const clone = res.clone();
          caches.open(CACHE_NAME).then((cache) => cache.put(req, clone));
        }
        return res;
      })
      .catch(() => caches.match(req))
  );
});
