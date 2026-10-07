#!/usr/bin/env node
// tmf-ui — the server behind the page (no dependencies). It is what the tmforum-ui image
// runs, and what `npm start` runs on a laptop:
//
//   npm start                      -> http://localhost:8080
//
// The all-in-one does not serve the UI: this server does, next to it (a sidecar with
// TMF_ENDPOINT=http://localhost:8632) or anywhere else (a laptop, a debug pod). It does
// three things:
//   1. serves the SPA from src/main/resources/tmf-ui/ (TMF_UI_STATIC_DIR in the image)
//   2. answers config.json, which tells the page how this server was started
//   3. acts as a GET proxy towards the TMForum endpoint
//      (required: neither tmforum-api nor its ingresses enable CORS)
//
// Environment variables:
//   PORT=8080               port to listen on
//   TMF_ENDPOINT=<url>      fix the TMForum endpoint instead of typing it in the page
//                           (always set it where somebody else can reach the port)
//   TMF_UI_STATIC_DIR=<dir> where the page's files are (set by the image)
//   TMF_INSECURE=1          accept self-signed certificates (*.nip.io hosts of the local demo)
//   TMF_MOCK=1              expose a fake TMForum at /mock (fixtures/data.json)
//   NODE_USE_ENV_PROXY=1    honour HTTPS_PROXY/HTTP_PROXY (the local demo's squid on :8888)

import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { existsSync, readFileSync } from 'node:fs';
import { extname, join, normalize, dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const PUBLIC_DIR = resolve(process.env.TMF_UI_STATIC_DIR ?? join(HERE, '..', 'src', 'main', 'resources', 'tmf-ui'));
const PORT = Number(process.env.PORT ?? 8080);
const MOCK = process.env.TMF_MOCK === '1';
const TIMEOUT_MS = Number(process.env.TMF_TIMEOUT_MS ?? 20000);

// TMF_ENDPOINT fixes the endpoint, so the page connects on load and nobody types it in. It
// also closes the proxy (see handleProxy): an open GET proxy is a convenience on a laptop and
// an SSRF the moment anybody else can reach this port.
const ENDPOINT = parseEndpoint(process.env.TMF_ENDPOINT);

// Told to the page so it can warn that `localhost` here is the container, not your machine -
// the single most expensive misunderstanding when running this image. /run/.containerenv is
// podman's equivalent of /.dockerenv.
const IN_CONTAINER = existsSync('/.dockerenv') || existsSync('/run/.containerenv');

function parseEndpoint(raw) {
  let s = (raw ?? '').trim().replace(/\/+$/, '');
  if (!s) return null;
  if (!/^https?:\/\//i.test(s)) s = `http://${s}`;
  try {
    return new URL(s);
  } catch {
    console.error(`TMF_ENDPOINT is not a URL: ${raw}`);
    process.exit(1);
  }
}

// Same origin, and under the endpoint's path when it has one.
function allowedUpstream(upstream) {
  if (upstream.origin !== ENDPOINT.origin) return false;
  const prefix = ENDPOINT.pathname.replace(/\/+$/, '');
  return prefix === '' || upstream.pathname === prefix || upstream.pathname.startsWith(`${prefix}/`);
}

if (process.env.TMF_INSECURE === '1') process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
};

function sendJson(res, status, obj) {
  const body = JSON.stringify(obj);
  res.writeHead(status, { 'content-type': 'application/json; charset=utf-8', 'content-length': Buffer.byteLength(body) });
  res.end(body);
}

// ---------------------------------------------------------------- proxy (GET only)

async function handleProxy(req, res, url) {
  if (req.method !== 'GET') return sendJson(res, 405, { error: 'This proxy is read-only: only GET is accepted.' });

  const target = url.searchParams.get('url');
  if (!target) return sendJson(res, 400, { error: 'Missing the ?url= parameter' });

  let upstream;
  try {
    upstream = new URL(target);
  } catch {
    return sendJson(res, 400, { error: `Not a valid URL: ${target}` });
  }
  if (upstream.protocol !== 'http:' && upstream.protocol !== 'https:') {
    return sendJson(res, 400, { error: `Scheme not allowed: ${upstream.protocol}` });
  }
  if (ENDPOINT && !allowedUpstream(upstream)) {
    return sendJson(res, 403, {
      error: `This tmf-ui is fixed to ${ENDPOINT.href} (TMF_ENDPOINT) and will not proxy ${upstream.origin}${upstream.pathname}`,
    });
  }

  let upRes;
  try {
    upRes = await fetch(upstream, {
      method: 'GET',
      headers: { accept: 'application/json' },
      // Following a redirect would step straight out of the allow-list just checked.
      redirect: ENDPOINT ? 'manual' : 'follow',
      signal: AbortSignal.timeout(TIMEOUT_MS),
    });
  } catch (err) {
    const msg = err?.name === 'TimeoutError'
      ? `Timed out after ${TIMEOUT_MS} ms against ${upstream.origin}`
      : `Could not reach ${upstream.origin}: ${err?.cause?.message ?? err?.message ?? err}`;
    return sendJson(res, 502, { error: msg, url: upstream.href });
  }

  const headers = {
    'content-type': upRes.headers.get('content-type') ?? 'application/json; charset=utf-8',
    'x-tmf-url': upstream.href,
  };
  // TMF630 pagination headers that the UI needs to read.
  for (const h of ['x-total-count', 'x-result-count', 'link']) {
    const v = upRes.headers.get(h);
    if (v) headers[h] = v;
  }
  const body = Buffer.from(await upRes.arrayBuffer());
  headers['content-length'] = body.length;
  res.writeHead(upRes.status, headers);
  res.end(body);
}

// ---------------------------------------------------------------- fake TMForum

let FIXTURES = null;
function fixtures() {
  if (FIXTURES) return FIXTURES;
  const f = join(HERE, 'fixtures', 'data.json');
  FIXTURES = existsSync(f) ? JSON.parse(readFileSync(f, 'utf8')) : {};
  return FIXTURES;
}

function handleMock(req, res, url) {
  const path = url.pathname.replace(/^\/mock/, '');
  const data = fixtures();

  if (data[path]) {
    const all = data[path];
    const offset = Number(url.searchParams.get('offset') ?? 0);
    const limit = Number(url.searchParams.get('limit') ?? 10);
    if (offset < 0 || limit < 1) return sendJson(res, 400, { error: 'invalid offset/limit' });

    // Simple equality filters, dotted paths included (relatedParty.id=...), plus the
    // operators the real QueryParser understands - `.regex` above all, because it is what
    // the UI sends for a plain text search, and a mock that ignores it makes the filter box
    // look broken.
    const filters = [...url.searchParams.entries()].filter(([k]) => !['offset', 'limit', 'fields', 'sort'].includes(k));
    const dig = (o, p) => p.split('.').reduce((acc, k) => (Array.isArray(acc) ? acc.map((x) => x?.[k]).flat() : acc?.[k]), o);
    const OPS = {
      // Typing produces half-written patterns ("(" and friends), so a bad regex falls back to
      // a substring match instead of failing the request.
      '.regex': (got, v) => {
        try {
          return new RegExp(v, 'i').test(String(got));
        } catch {
          return String(got).toLowerCase().includes(v.toLowerCase());
        }
      },
      '.eq': (got, v) => String(got) === v,
      '.gt': (got, v) => String(got) > v,
      '.gte': (got, v) => String(got) >= v,
      '.lt': (got, v) => String(got) < v,
      '.lte': (got, v) => String(got) <= v,
    };
    const matched = all.filter((item) => filters.every(([k, v]) => {
      const op = Object.keys(OPS).find((o) => k.endsWith(o));
      const got = dig(item, op ? k.slice(0, -op.length) : k);
      const test = op ? OPS[op] : (g, val) => String(g) === val;
      // `,` is OR between values, as in the backend.
      const any = (g) => v.split(',').some((one) => test(g, one));
      return Array.isArray(got) ? got.some(any) : any(got);
    }));

    const page = matched.slice(offset, offset + limit);
    const link = [`<${url.pathname}?offset=${offset}&limit=${limit}>; rel="self"`];
    if (offset + limit < matched.length) link.push(`<${url.pathname}?offset=${offset + limit}&limit=${limit}>; rel="next"`);
    if (offset > 0) link.push(`<${url.pathname}?offset=${Math.max(0, offset - limit)}&limit=${limit}>; rel="prev"`);

    const body = JSON.stringify(page);
    res.writeHead(page.length < matched.length ? 206 : 200, {
      'content-type': 'application/json; charset=utf-8',
      'x-total-count': String(matched.length),
      link: link.join(', '),
      'content-length': Buffer.byteLength(body),
    });
    return res.end(body);
  }

  const m = /^(.*)\/([^/]+)$/.exec(path);
  if (m && data[m[1]]) {
    const item = data[m[1]].find((x) => x.id === decodeURIComponent(m[2]));
    if (item) return sendJson(res, 200, item);
    return sendJson(res, 404, { code: '404', reason: 'Not Found', message: `No such ${decodeURIComponent(m[2])}` });
  }
  return sendJson(res, 404, { code: '404', reason: 'Not Found', message: `Unknown path: ${path}` });
}

// ---------------------------------------------------------------- static files

async function handleStatic(req, res, url) {
  const rel = normalize(decodeURIComponent(url.pathname)).replace(/^(\.\.[/\\])+/, '');
  const file = join(PUBLIC_DIR, rel === '/' || rel === '\\' ? 'index.html' : rel);
  if (!file.startsWith(PUBLIC_DIR)) return sendJson(res, 403, { error: 'Forbidden' });
  try {
    const body = await readFile(file);
    res.writeHead(200, { 'content-type': MIME[extname(file)] ?? 'application/octet-stream', 'cache-control': 'no-cache' });
    res.end(body);
  } catch {
    res.writeHead(404, { 'content-type': 'text/plain; charset=utf-8' });
    res.end('404');
  }
}

createServer(async (req, res) => {
  const url = new URL(req.url, `http://localhost:${PORT}`);
  try {
    if (url.pathname === '/healthz') return sendJson(res, 200, { status: 'UP' });
    if (url.pathname === '/api/proxy') return await handleProxy(req, res, url);
    if (MOCK && url.pathname.startsWith('/mock')) return handleMock(req, res, url);
    if (url.pathname === '/config.json') {
      return sendJson(res, 200, {
        mock: MOCK,
        mockBase: MOCK ? `http://localhost:${PORT}/mock` : null,
        endpoint: ENDPOINT ? ENDPOINT.href.replace(/\/$/, '') : null,
        locked: Boolean(ENDPOINT),
        container: IN_CONTAINER,
      });
    }
    return await handleStatic(req, res, url);
  } catch (err) {
    sendJson(res, 500, { error: String(err?.message ?? err) });
  }
}).listen(PORT, () => {
  console.log(`tmf-ui  ->  http://localhost:${PORT}`);
  if (ENDPOINT) console.log(`endpoint fixed to ${ENDPOINT.href} (TMF_ENDPOINT); the proxy accepts nothing else`);
  if (MOCK) console.log(`mock mode on: use http://localhost:${PORT}/mock as the TMForum endpoint`);
  if (process.env.TMF_INSECURE === '1') console.log('TLS verification off (TMF_INSECURE=1)');
});
