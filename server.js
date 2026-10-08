const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || 8080;

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.webmanifest': 'application/manifest+json; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.png': 'image/png',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon'
};

function resolveFilePath(urlPath) {
  const cleanPath = decodeURIComponent(urlPath.split('?')[0]);

  if (cleanPath === '/' || cleanPath === '/index.html') {
    if (fs.existsSync(path.join(__dirname, 'index.html'))) {
      return path.join(__dirname, 'index.html');
    }
    return path.join(__dirname, 'pwa', 'index.html');
  }

  // Serve ZAVIRO original brand & food images directly from drawable-nodpi if not in /images
  if (cleanPath.startsWith('/images/')) {
    const fileName = path.basename(cleanPath);
    const candidates = [
      path.join(__dirname, 'images', fileName),
      path.join(__dirname, 'pwa', 'images', fileName),
      path.join(__dirname, 'app', 'src', 'main', 'res', 'drawable-nodpi', fileName),
      path.join(__dirname, 'app', 'src', 'main', 'res', 'drawable', fileName)
    ];
    for (const candidate of candidates) {
      if (fs.existsSync(candidate)) return candidate;
    }
  }

  const rootCandidate = path.join(__dirname, cleanPath);
  if (fs.existsSync(rootCandidate) && fs.statSync(rootCandidate).isFile()) {
    return rootCandidate;
  }

  const pwaCandidate = path.join(__dirname, 'pwa', cleanPath);
  if (fs.existsSync(pwaCandidate) && fs.statSync(pwaCandidate).isFile()) {
    return pwaCandidate;
  }

  // SPA fallback to index.html
  return path.join(__dirname, 'index.html');
}

const server = http.createServer((req, res) => {
  const filePath = resolveFilePath(req.url || '/');
  const ext = path.extname(filePath).toLowerCase();
  const contentType =
    path.basename(filePath) === 'manifest.json'
      ? 'application/manifest+json; charset=utf-8'
      : MIME_TYPES[ext] || 'application/octet-stream';

  fs.readFile(filePath, (err, content) => {
    if (err) {
      res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
      res.end('Not Found');
      return;
    }

    const headers = {
      'Content-Type': contentType,
      'X-Content-Type-Options': 'nosniff'
    };

    if (path.basename(filePath) === 'sw.js') {
      headers['Service-Worker-Allowed'] = '/';
      headers['Cache-Control'] = 'no-cache';
    } else if (ext === '.jpg' || ext === '.png') {
      headers['Cache-Control'] = 'public, max-age=86400';
    }

    res.writeHead(200, headers);
    res.end(content);
  });
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`ZAVIRO Full-Stack Web App & PWA running on http://0.0.0.0:${PORT}`);
});
