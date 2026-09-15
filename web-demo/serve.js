// خادم محلي بسيط للنموذج التجريبي: node web-demo/serve.js
const http = require("http");
const fs = require("fs");
const path = require("path");

const ROOT = __dirname;
const PORT = +process.env.PORT || 8765;
const TYPES = { ".html": "text/html; charset=utf-8", ".js": "text/javascript; charset=utf-8", ".css": "text/css; charset=utf-8", ".otf": "font/otf", ".ttf": "font/ttf", ".png": "image/png" };

http.createServer((req, res) => {
  const urlPath = decodeURIComponent(req.url.split("?")[0]);
  const file = path.join(ROOT, urlPath === "/" ? "index.html" : urlPath);
  if (!file.startsWith(ROOT)) { res.writeHead(403); return res.end(); }
  fs.readFile(file, (err, data) => {
    if (err) { res.writeHead(404); return res.end("Not found"); }
    res.writeHead(200, { "Content-Type": TYPES[path.extname(file)] || "application/octet-stream", "Cache-Control": "no-cache" });
    res.end(data);
  });
}).listen(PORT, () => console.log(`Quran web demo: http://localhost:${PORT}`));
