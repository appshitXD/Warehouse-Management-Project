import http from 'node:http';
import { readFile, readdir } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const root = path.dirname(fileURLToPath(import.meta.url));
const assets = new Map([
  ['/', ['index.html', 'text/html']],
  ['/index.html', ['index.html', 'text/html']],
]);
// Enumerate only public frontend assets. Tests, server code and team folders stay private.
async function registerAssets(directory) {
  for (const entry of await readdir(path.join(root, directory), {
    withFileTypes: true,
  })) {
    const relative = `${directory}/${entry.name}`;
    if (entry.isDirectory()) await registerAssets(relative);
    else if (entry.isFile() && /\.(js|css)$/.test(entry.name))
      assets.set(`/${relative}`, [
        relative,
        entry.name.endsWith('.css') ? 'text/css' : 'text/javascript',
      ]);
  }
}
await registerAssets('src');
await registerAssets('styles');
const port = Number(process.env.PORT || 5173);
http
  .createServer(async (req, res) => {
    const file = assets.get(new URL(req.url, 'http://localhost').pathname);
    if (!file || !['GET', 'HEAD'].includes(req.method)) {
      res.writeHead(404);
      res.end('Not found');
      return;
    }
    try {
      const content = await readFile(path.join(root, file[0]));
      res.writeHead(200, {
        'Content-Type': `${file[1]}; charset=utf-8`,
        'Cache-Control': 'no-store',
      });
      res.end(req.method === 'HEAD' ? undefined : content);
    } catch {
      res.writeHead(500);
      res.end('Could not read file');
    }
  })
  .listen(port, '127.0.0.1', () =>
    console.log(`Warehouse: http://localhost:${port}`),
  );
