// tmf-ui — read-only SPA over the TMForum APIs of tmforum-api (all-in-one).
//
// It runs in one of two modes, and config.json (next to this file) says which:
//  - served by the all-in-one itself, under /ui/ (config.json is the static one in the jar,
//    `sameOrigin: true`): the API is this page's own origin, so requests go straight to it.
//  - served by dev/server.mjs (which answers config.json itself): everything goes through
//    its /api/proxy, because neither the API nor its ingresses set CORS.

import { APIS, GROUPS } from './catalog.js';
import {
  LOCATIONS, locate, isRefLike, candidatesFor, probeCandidates, refLabel, urnTypeOf, URN_RE,
  referrersOf, undeclaredReferrersOf,
} from './refs.js';

const $ = (s) => document.querySelector(s);
const RECENT_KEY = 'tmf-ui.recent';
const BASE_KEY = 'tmf-ui.base';
const ISO_RE = /^\d{4}-\d{2}-\d{2}T[\d:.]+(Z|[+-]\d{2}:\d{2})$/;

const state = {
  base: '',
  /** `${apiKey}/${resource}` -> { ok, status, total, error } */
  resStatus: new Map(),
  detecting: false,
  columns: new Map(), // `${apiKey}/${resource}` -> string[] | null (null = auto)
  probeCache: new Map(), // urn -> location
  renderSeq: 0, // a render whose fetch comes back late must not paint over a newer one
  filterCaret: null, // where the caret was when a keystroke triggered the re-render
  cfg: {}, // what config.json said: sameOrigin, mock, endpoint, locked, container
};

const LOCAL_HOST_RE = /^(localhost|127\.0\.0\.1|\[::1\]|0\.0\.0\.0)$/i;

/** Inside a container, a port-forward on the host is at host.docker.internal. */
function hostRewrite(base) {
  try {
    const u = new URL(base);
    if (!LOCAL_HOST_RE.test(u.hostname)) return null;
    u.hostname = 'host.docker.internal';
    return u.toString().replace(/\/$/, '');
  } catch { return null; }
}

// ------------------------------------------------------------------ DOM helpers

function el(tag, props = {}, ...kids) {
  const n = document.createElement(tag);
  for (const [k, v] of Object.entries(props)) {
    if (v === undefined || v === null || v === false) continue;
    if (k === 'class') n.className = v;
    else if (k === 'text') n.textContent = v;
    else if (k === 'html') n.innerHTML = v;
    else if (k.startsWith('on')) n.addEventListener(k.slice(2).toLowerCase(), v);
    else n.setAttribute(k, v === true ? '' : v);
  }
  for (const kid of kids.flat()) {
    if (kid === null || kid === undefined || kid === false) continue;
    n.append(kid instanceof Node ? kid : document.createTextNode(String(kid)));
  }
  return n;
}

const clear = (node) => { while (node.firstChild) node.removeChild(node.firstChild); };

function debounce(fn, ms) {
  let t = null;
  const wrapped = (...args) => {
    clearTimeout(t);
    t = setTimeout(() => fn(...args), ms);
  };
  wrapped.cancel = () => clearTimeout(t);
  return wrapped;
}

// The toolbar is rebuilt on every render, so typing in the filter would lose the caret after
// the first keystroke. `applyFilter` records where it was; this puts it back.
function restoreFilterFocus(input) {
  const pos = state.filterCaret;
  state.filterCaret = null;
  if (pos === null || pos === undefined) return;
  input.focus();
  const p = Math.min(pos, input.value.length);
  input.setSelectionRange(p, p);
}

function toast(text) {
  const t = el('div', { class: 'msg', style: 'position:fixed;bottom:18px;right:18px;z-index:50', text });
  document.body.append(t);
  setTimeout(() => t.remove(), 2200);
}

async function copy(text, label = 'Copied') {
  try {
    await navigator.clipboard.writeText(text);
    toast(label);
  } catch {
    toast('Could not copy');
  }
}

// ------------------------------------------------------------------ HTTP access

function normalizeBase(raw) {
  let b = (raw ?? '').trim().replace(/\/+$/, '');
  if (!b) return '';
  if (!/^https?:\/\//i.test(b)) b = `http://${b}`;
  return b;
}

function upstreamUrl(path, params) {
  const url = new URL(state.base + path);
  for (const [k, v] of Object.entries(params ?? {})) {
    if (v !== undefined && v !== null && v !== '') url.searchParams.set(k, v);
  }
  return url.toString();
}

function parseLink(header) {
  const rels = {};
  for (const part of (header ?? '').split(/,\s*(?=<)/)) {
    const m = /^<([^>]+)>\s*;\s*rel="?([a-z]+)"?/i.exec(part.trim());
    if (m) rels[m[2].toLowerCase()] = m[1];
  }
  return rels;
}

/** GET against the TMForum API, directly or through the dev proxy. Never throws: the error comes back inside. */
async function tmfGet(path, params) {
  const url = upstreamUrl(path, params);
  let res;
  try {
    res = state.cfg.sameOrigin
      ? await fetch(url, { headers: { accept: 'application/json' } })
      : await fetch(`/api/proxy?url=${encodeURIComponent(url)}`);
  } catch (err) {
    const who = state.cfg.sameOrigin ? 'The TMForum API' : 'The local server';
    const hint = state.cfg.sameOrigin ? '' : ' Is "npm start" still running?';
    return { ok: false, status: 0, url, error: `${who} is not answering (${err.message}).${hint}` };
  }
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { /* non-JSON response */ }

  // 206 Partial Content is the normal answer to a paginated list.
  const ok = res.status >= 200 && res.status < 300;
  const totalHeader = res.headers.get('x-total-count');
  return {
    ok,
    status: res.status,
    url,
    data,
    raw: text,
    total: totalHeader !== null ? Number(totalHeader) : null,
    links: parseLink(res.headers.get('link')),
    error: ok ? null : (data?.message ?? data?.error ?? data?.reason ?? text.slice(0, 300) ?? `HTTP ${res.status}`),
  };
}

// ------------------------------------------------------------------ detection

function key(apiKey, resource) { return `${apiKey}/${resource}`; }
function statusOf(apiKey, resource) { return state.resStatus.get(key(apiKey, resource)); }
function apiIsUp(apiKey) {
  return APIS.find((a) => a.key === apiKey)?.resources.some((r) => statusOf(apiKey, r.name)?.ok) ?? false;
}

async function detect() {
  if (!state.base) return;
  state.detecting = true;
  state.resStatus.clear();
  renderStatus();
  renderSidebar();

  await Promise.all(LOCATIONS.map(async (loc) => {
    const r = await tmfGet(`${loc.basePath}/${loc.resource}`, { limit: 1 });
    state.resStatus.set(key(loc.apiKey, loc.resource), {
      ok: r.ok, status: r.status, total: r.total, error: r.error,
      hasData: Array.isArray(r.data) && r.data.length > 0,
    });
  }));

  state.detecting = false;
  renderStatus();
  renderSidebar();
  route();
}

function renderStatus() {
  const box = $('#status');
  box.hidden = !state.base;
  clear(box);
  if (!state.base) return;

  if (state.detecting) {
    box.append(el('span', { class: 'dot wait' }), el('span', { text: 'detecting APIs…' }));
    return;
  }
  const up = APIS.filter((a) => apiIsUp(a.key)).length;
  box.append(
    el('span', { class: `dot ${up ? 'ok' : 'err'}` }),
    el('span', { text: up ? `${up}/${APIS.length} APIs` : 'no answer' }),
    el('button', { class: 'link', title: 'Detect again', onclick: detect, text: '⟲' }),
  );
}

// ------------------------------------------------------------------ sidebar

function renderSidebar() {
  const side = $('#sidebar');
  clear(side);
  if (!state.base) {
    side.append(el('p', { class: 'hint', html: 'Enter the endpoint of the all-in-one TMForum and press <b>Connect</b>.' }));
    return;
  }
  const { apiKey, resource } = parseHash();
  side.append(el('a', { href: '#/', class: 'group-title', style: 'display:block', text: '◂ Overview' }));

  for (const group of GROUPS) {
    const apis = APIS.filter((a) => a.group === group);
    if (!apis.length) continue;
    side.append(el('div', { class: 'group-title', text: group }));
    for (const api of apis) {
      const up = apiIsUp(api.key);
      const det = el('details', { class: `api${up || state.detecting ? '' : ' off'}`, open: up && api.key === apiKey ? true : undefined });
      det.append(el('summary', {},
        el('span', { text: api.label }),
        el('span', { class: 'tmf', text: api.tmf }),
      ));
      const ul = el('ul', { class: 'res-list' });
      for (const r of api.resources) {
        const st = statusOf(api.key, r.name);
        const total = st?.total;
        ul.append(el('li', {},
          el('a', {
            href: `#/${api.key}/${r.name}`,
            class: api.key === apiKey && r.name === resource ? 'active' : undefined,
            title: st?.ok ? `HTTP ${st.status}` : (st?.error ?? 'not checked'),
            style: st && !st.ok ? 'opacity:.45' : undefined,
          },
            r.name,
            total !== null && total !== undefined ? el('span', { class: 'tmf', text: ` ${total}` }) : null),
        ));
      }
      det.append(ul);
      side.append(det);
    }
  }
}

// ------------------------------------------------------------------ value formatting

function fmtDate(s) {
  const d = new Date(s);
  if (Number.isNaN(d.getTime())) return s;
  return el('span', { title: s, text: d.toLocaleString('en-GB', { dateStyle: 'medium', timeStyle: 'short' }) });
}

function renderScalar(v) {
  if (v === null || v === undefined || v === '') return el('span', { class: 'empty-val', text: '—' });
  if (typeof v === 'boolean') return el('span', { class: v ? 'bool-true' : 'bool-false', text: v ? 'yes' : 'no' });
  if (typeof v === 'number') return el('span', { text: String(v) });
  const s = String(v);
  if (ISO_RE.test(s)) return fmtDate(s);
  if (/^https?:\/\//.test(s)) return el('a', { href: s, target: '_blank', rel: 'noreferrer', class: 'mono', text: s });
  if (URN_RE.test(s)) return el('span', { class: 'mono', text: s });
  return el('span', { text: s });
}

function detailHref(loc, id) {
  return `#/${loc.apiKey}/${loc.resource}/${encodeURIComponent(id)}`;
}

/** The list of `loc`, filtered — the same `q` the filter box takes. */
function listHref(loc, filter) {
  const q = new URLSearchParams(filter ? { q: filter } : {}).toString();
  return `#/${loc.apiKey}/${loc.resource}${q ? `?${q}` : ''}`;
}

function renderRef(ref, fieldName) {
  const wrap = el('span', { class: 'ref' });
  const label = refLabel(ref);
  const cached = state.probeCache.get(ref.id);
  const cands = cached ? [cached] : candidatesFor(ref, fieldName);

  if (cands.length) {
    const c = cands[0];
    wrap.append(el('a', { href: detailHref(c, ref.id), title: `${ref.id}\n→ ${c.basePath}/${c.resource}` }, label));
    wrap.append(el('span', { class: 'badge', text: ref['@referredType'] ?? c.resource }));
    if (cands.length > 1) {
      const sel = el('select', {
        title: 'This URN exists in more than one API',
        style: 'font-size:11px;padding:1px 4px',
        onchange: (e) => { location.hash = e.target.value; },
      });
      sel.append(el('option', { value: '', text: '↗ another API…' }));
      for (const alt of cands.slice(1)) {
        sel.append(el('option', { value: detailHref(alt, ref.id), text: `${alt.tmf} ${alt.resource}` }));
      }
      wrap.append(sel);
    }
  } else {
    wrap.append(el('span', { text: label }));
    if (ref.role) wrap.append(el('span', { class: 'badge', text: ref.role }));
    const btn = el('button', {
      class: 'link', text: 'find…', title: 'Probe which resource this URN lives in',
      onclick: async () => {
        btn.textContent = 'searching…';
        const found = await probe(ref, fieldName);
        if (found) { location.hash = detailHref(found, ref.id); } else { btn.textContent = 'not found'; }
      },
    });
    wrap.append(btn);
  }
  return wrap;
}

/** Probes resources until one answers 200 for that URN. */
async function probe(ref, fieldName) {
  if (state.probeCache.has(ref.id)) return state.probeCache.get(ref.id);
  const urnType = urnTypeOf(ref.id);
  const order = probeCandidates(ref, fieldName)
    .filter((l) => statusOf(l.apiKey, l.resource)?.ok !== false)
    .sort((a, b) => (b.urnType === urnType) - (a.urnType === urnType));
  for (const loc of order) {
    const r = await tmfGet(`${loc.basePath}/${loc.resource}/${encodeURIComponent(ref.id)}`);
    if (r.ok) { state.probeCache.set(ref.id, loc); return loc; }
  }
  return null;
}

/**
 * Dependency-free JSON syntax highlighting: tokenise the already formatted text and
 * wrap each piece in a <span>. Built out of DOM nodes (no innerHTML), so the entity's
 * own content is never interpreted as HTML.
 */
const JSON_TOKEN_RE = /"(?:\\u[\da-fA-F]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(?:true|false|null)\b|-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?/g;

function highlightJson(text) {
  const frag = document.createDocumentFragment();
  let last = 0;
  for (const m of text.matchAll(JSON_TOKEN_RE)) {
    if (m.index > last) frag.append(document.createTextNode(text.slice(last, m.index)));
    const tok = m[0];
    let cls;
    if (tok.startsWith('"')) cls = m[1] !== undefined ? 'j-key' : 'j-str';
    else if (tok === 'true' || tok === 'false') cls = 'j-bool';
    else if (tok === 'null') cls = 'j-null';
    else cls = 'j-num';
    frag.append(el('span', { class: cls, text: tok }));
    last = m.index + tok.length;
  }
  if (last < text.length) frag.append(document.createTextNode(text.slice(last)));
  return frag;
}

/** Like renderScalar, but clipping the long strings that blow the table up. */
function renderCell(v) {
  if (typeof v === 'string' && v.length > 140 && !ISO_RE.test(v)) {
    return el('span', { title: v, text: `${v.slice(0, 137)}…` });
  }
  return renderScalar(v);
}

function renderValue(value, fieldName) {
  if (Array.isArray(value)) {
    if (!value.length) return el('span', { class: 'empty-val', text: '— (empty)' });
    if (value.every((v) => v === null || typeof v !== 'object')) {
      return el('span', {}, value.map((v, i) => [i ? ', ' : '', renderScalar(v)]).flat());
    }
    if (value.every(isRefLike)) {
      const box = el('span', {});
      value.forEach((v, i) => { if (i) box.append(' · '); box.append(renderRef(v, fieldName)); });
      return box;
    }
    const d = el('details', { class: 'node' });
    d.append(el('summary', {}, `${value.length} items`, el('span', { class: 'tag', text: ` [${fieldName ?? ''}]` })));
    value.forEach((v, i) => {
      const item = el('div', { class: 'node' });
      item.append(el('div', { class: 'hint', text: `#${i}` }), renderValue(v, fieldName));
      d.append(item);
    });
    return d;
  }

  if (value && typeof value === 'object') {
    if (isRefLike(value)) return renderRef(value, fieldName);
    const entries = Object.entries(value);
    if (!entries.length) return el('span', { class: 'empty-val', text: '— (empty)' });
    const d = el('details', { class: 'node', open: entries.length <= 4 ? true : undefined });
    const type = value['@type'] ?? '';
    d.append(el('summary', {}, `${entries.length} fields`, type ? el('span', { class: 'tag', text: ` ${type}` }) : null));
    d.append(kvList(value));
    return d;
  }

  return renderScalar(value);
}

const FIELD_ORDER = ['id', 'href', '@type', '@referredType', '@baseType', 'name', 'fullName', 'givenName', 'familyName',
  'tradingName', 'description', 'version', 'lifecycleStatus', 'status', 'state', 'isBundle', 'isSellable',
  'validFor', 'startDate', 'lastUpdate'];

function sortedEntries(obj) {
  return Object.entries(obj).sort(([a], [b]) => {
    const ia = FIELD_ORDER.indexOf(a); const ib = FIELD_ORDER.indexOf(b);
    if (ia !== -1 || ib !== -1) return (ia === -1 ? 99 : ia) - (ib === -1 ? 99 : ib);
    return a.localeCompare(b);
  });
}

function kvList(obj, { skipHref = false } = {}) {
  const dl = el('dl', { class: 'kv' });
  for (const [k, v] of sortedEntries(obj)) {
    // href == id in tmforum-api; repeating it only takes up room.
    if (skipHref && k === 'href' && v === obj.id) continue;
    dl.append(el('dt', { text: k }), el('dd', {}, renderValue(v, k)));
  }
  return dl;
}

// ------------------------------------------------------------------ routing

function parseHash() {
  const h = location.hash.replace(/^#\/?/, '');
  const [pathPart, queryPart] = h.split('?');
  const segs = pathPart.split('/').filter(Boolean);
  return {
    apiKey: segs[0] ? decodeURIComponent(segs[0]) : null,
    resource: segs[1] ? decodeURIComponent(segs[1]) : null,
    id: segs[2] ? decodeURIComponent(segs[2]) : null,
    query: new URLSearchParams(queryPart ?? ''),
  };
}

function setQuery(patch) {
  const { apiKey, resource, id, query } = parseHash();
  for (const [k, v] of Object.entries(patch)) {
    if (v === null || v === undefined || v === '') query.delete(k); else query.set(k, v);
  }
  const qs = query.toString();
  const path = `#/${apiKey}/${resource}${id ? `/${encodeURIComponent(id)}` : ''}`;
  location.hash = `${path}${qs ? `?${qs}` : ''}`;
}

function crumbs(...parts) {
  const c = el('div', { class: 'crumbs' });
  parts.forEach((p, i) => {
    if (i) c.append(' / ');
    c.append(p.href ? el('a', { href: p.href, text: p.text }) : el('span', { text: p.text }));
  });
  return c;
}

function errorBox(res, extra) {
  return el('div', { class: 'msg error' },
    el('h3', { text: `Error ${res.status || ''}`.trim() }),
    el('p', { text: res.error ?? 'No detail' }),
    el('p', { class: 'mono hint', text: res.url }),
    extra ? el('p', { class: 'hint', text: extra }) : null,
  );
}

async function route() {
  const main = $('#main');
  const { apiKey, resource, id } = parseHash();
  renderSidebar();

  if (!state.base) return; // the welcome screen is already in place
  if (!apiKey || !resource) return renderOverview();

  const loc = locate(apiKey, resource);
  if (!loc) {
    clear(main);
    main.append(el('div', { class: 'msg error' }, el('h3', { text: 'Unknown resource' }),
      el('p', { text: `${apiKey}/${resource} is not in the catalogue.` })));
    return;
  }
  return id ? renderDetail(loc, id) : renderList(loc);
}

// ------------------------------------------------------------------ overview

function renderOverview() {
  const main = $('#main');
  clear(main);
  main.append(el('h1', { text: 'Endpoint overview' }));
  main.append(el('p', { class: 'subtitle' }, el('span', { class: 'mono', text: state.base })));

  if (state.detecting) {
    main.append(el('p', { class: 'spinner', text: 'Detecting which APIs answer…' }));
    return;
  }

  const rows = LOCATIONS.map((l) => ({ l, st: statusOf(l.apiKey, l.resource) }));
  const up = rows.filter((r) => r.st?.ok);
  if (!up.length) {
    // In a container this is nearly always the same misunderstanding, so offer the answer
    // rather than describe it.
    const rewritten = state.cfg.container && !state.cfg.locked ? hostRewrite(state.base) : null;
    if (state.cfg.sameOrigin) {
      main.append(el('div', { class: 'msg error' },
        el('h3', { text: 'No TMForum API answered' }),
        el('p', { text: 'This page is served by the all-in-one itself, yet none of its APIs answered. Check that it has finished starting and that it can reach its context broker.' }),
        rows[0]?.st ? el('p', { class: 'hint mono', text: `example: ${rows[0].st.error ?? ''}` }) : null,
      ));
      return;
    }
    main.append(el('div', { class: 'msg error' },
      el('h3', { text: 'No TMForum API answered' }),
      el('p', { text: 'This endpoint does not look like an all-in-one tmforum-api, or it is not reachable from this machine.' }),
      rewritten ? el('p', {},
        'This tmf-ui runs in a container, so ', el('code', { text: new URL(state.base).hostname }),
        ' is the container itself. If the endpoint is a port-forward on your machine, try ',
        el('button', { class: 'link', text: rewritten, onclick: () => connect(rewritten) }), '.',
      ) : null,
      el('p', { html: 'Check the <b>base path</b> (it must be the root, with no <code>/tmf-api/...</code>), that the port-forward is alive, and for <code>*.nip.io</code> hosts start with <code>TMF_INSECURE=1 NODE_USE_ENV_PROXY=1 HTTPS_PROXY=http://localhost:8888 npm start</code>.' }),
      rows[0]?.st ? el('p', { class: 'hint mono', text: `example: ${rows[0].st.error ?? ''}` }) : null,
    ));
    return;
  }

  const withData = up.filter((r) => r.st.total === null ? r.st.hasData : r.st.total > 0);
  main.append(el('p', { class: 'subtitle', text: `${up.length} resources reachable, ${withData.length} with data.` }));

  const table = el('table');
  table.append(el('thead', {}, el('tr', {},
    el('th', { text: 'API' }), el('th', { text: 'TMF' }), el('th', { text: 'Resource' }),
    el('th', { text: 'Entities' }), el('th', { text: '' }))));
  const tbody = el('tbody');
  for (const { l, st } of up) {
    tbody.append(el('tr', {},
      el('td', { text: l.apiLabel }),
      el('td', { class: 'mono', text: l.tmf }),
      el('td', {}, el('a', { href: `#/${l.apiKey}/${l.resource}`, text: l.resource })),
      el('td', { text: st.total !== null && st.total !== undefined ? String(st.total) : (st.hasData ? '≥1' : '0') }),
      el('td', { class: 'mono hint', text: `${l.basePath}/${l.resource}` }),
    ));
  }
  table.append(tbody);
  main.append(table);

  const failed = rows.filter((r) => r.st && !r.st.ok);
  if (failed.length) {
    const d = el('details', { class: 'node', style: 'margin-top:18px' });
    d.append(el('summary', { text: `${failed.length} resources with no answer` }));
    for (const { l, st } of failed) {
      d.append(el('div', { class: 'hint mono', text: `${l.basePath}/${l.resource} — ${st.status || 'network'} ${st.error ?? ''}`.trim() }));
    }
    main.append(d);
  }
}

// ------------------------------------------------------------------ list view

function pickColumns(items, loc) {
  const chosen = state.columns.get(key(loc.apiKey, loc.resource));
  const present = new Set();
  for (const it of items) {
    for (const [k, v] of Object.entries(it ?? {})) {
      if (v === null || typeof v !== 'object') present.add(k);
    }
  }
  const all = [...present].sort((a, b) => {
    const ia = FIELD_ORDER.indexOf(a); const ib = FIELD_ORDER.indexOf(b);
    if (ia !== -1 || ib !== -1) return (ia === -1 ? 99 : ia) - (ib === -1 ? 99 : ib);
    return a.localeCompare(b);
  });
  if (chosen) return { all, cols: chosen.filter((c) => present.has(c)) };
  const auto = all.filter((c) => c !== 'href' && c !== '@baseType' && c !== '@schemaLocation').slice(0, 7);
  return { all, cols: auto.includes('id') ? auto : ['id', ...auto].slice(0, 7) };
}

async function renderList(loc) {
  const seq = ++state.renderSeq;
  const main = $('#main');
  const { query } = parseHash();
  const limit = Number(query.get('limit') ?? 20);
  const offset = Number(query.get('offset') ?? 0);
  const filter = query.get('q') ?? '';
  const sort = query.get('sort') ?? '';

  clear(main);
  main.append(crumbs(
    { text: 'Overview', href: '#/' },
    { text: `${loc.tmf} ${loc.apiLabel}` },
    { text: loc.resource },
  ));
  main.append(el('h1', { text: loc.resource }));

  const params = { offset, limit, sort: sort || undefined };
  // The filter accepts the backend's own syntax (QueryParser): a=b&c.d=e, the
  // .regex/.gt/.lt operators, `,` as OR. With no `=` it is read as a search by name.
  if (filter) {
    if (filter.includes('=')) {
      for (const pair of filter.split('&')) {
        const i = pair.indexOf('=');
        if (i > 0) params[pair.slice(0, i).trim()] = pair.slice(i + 1).trim();
      }
    } else {
      params['name.regex'] = filter.trim();
    }
  }

  const toolbar = el('div', { class: 'toolbar' });
  const filterInput = el('input', {
    class: 'grow', value: filter, placeholder: 'filter: text, or lifecycleStatus=Launched&relatedParty.id=urn:…',
    title: 'Without "=" it searches by name (name.regex). With "=" it is sent to the backend as is.',
  });
  // Filters as you type, and unfilters when the box is emptied - no Enter needed. Enter still
  // works and skips the wait. `renderList` rebuilds this element on every render, so the
  // caret has to be put back afterwards; see restoreFilterFocus below.
  const applyFilter = () => {
    state.filterCaret = filterInput.selectionStart;
    setQuery({ q: filterInput.value, offset: null });
  };
  const applyFilterSoon = debounce(applyFilter, 300);
  filterInput.addEventListener('input', applyFilterSoon);
  filterInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') { applyFilterSoon.cancel(); applyFilter(); }
  });
  const sortInput = el('input', {
    value: sort, placeholder: 'sort: name, -lastUpdate', style: 'max-width:180px;flex:0 1 180px',
    title: "The backend's sort=. Prefix with '-' for descending.",
  });
  sortInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') setQuery({ sort: sortInput.value, offset: null });
  });
  const limitSel = el('select', { onchange: (e) => setQuery({ limit: e.target.value, offset: null }) });
  for (const n of [10, 20, 50, 100]) {
    limitSel.append(el('option', { value: n, selected: n === limit ? true : undefined, text: `${n}/page` }));
  }
  toolbar.append(filterInput, sortInput, el('span', { class: 'sep' }), limitSel);
  main.append(toolbar);
  restoreFilterFocus(filterInput);

  const body = el('div', {}, el('p', { class: 'spinner', text: 'Loading…' }));
  main.append(body);

  const res = await tmfGet(`${loc.basePath}/${loc.resource}`, params);
  if (seq !== state.renderSeq) return; // a newer render already owns #main
  clear(body);

  main.insertBefore(el('p', { class: 'subtitle' },
    el('span', { class: 'mono', text: res.url }), ' ',
    el('button', { class: 'link', text: 'copy curl', onclick: () => copy(`curl -s '${res.url}' | jq`) }),
  ), toolbar);

  if (!res.ok) { body.append(errorBox(res)); return; }
  const items = Array.isArray(res.data) ? res.data : (res.data ? [res.data] : []);

  // Pagination: X-Total-Count only shows up when the broker returns
  // NGSILD-Results-Count; otherwise we lean on the Link header (rel="next").
  const hasNext = res.links.next !== undefined || (res.total === null && items.length === limit);
  const totalTxt = res.total !== null && res.total !== undefined
    ? `${offset + 1}–${offset + items.length} of ${res.total}`
    : `${offset + 1}–${offset + items.length} (total unknown)`;
  toolbar.append(
    el('span', { class: 'sep' }),
    el('button', { text: '‹ previous', disabled: offset <= 0 ? true : undefined, onclick: () => setQuery({ offset: Math.max(0, offset - limit) }) }),
    el('button', { text: 'next ›', disabled: hasNext ? undefined : true, onclick: () => setQuery({ offset: offset + limit }) }),
    el('span', { class: 'count', text: totalTxt, title: res.total === null ? 'The backend did not return X-Total-Count' : `HTTP ${res.status}` }),
    // Spread, not a `: null`: el() drops null children but Node.append() renders the string
    // "null", which is what used to sit next to the counter on every 200.
    ...(res.status === 206
      ? [el('span', { class: 'pill', title: '206 Partial Content: a partial result, which is normal when paginating', text: '206' })]
      : []),
  );

  if (!items.length) {
    // Bare text is sent as `name.regex=`, but whether that is a pattern or a plain equality
    // is up to the broker: on the one measured, the full name matches and any fragment returns
    // empty. Without this note that reads as "there is nothing here".
    const bareText = filter && !filter.includes('=');
    body.append(el('div', { class: 'msg' },
      el('p', { text: 'No results for this query.' }),
      bareText ? el('p', { class: 'hint' },
        'Text is searched as ', el('code', { text: `name.regex=${filter}` }),
        '. Some brokers match that exactly instead of as a pattern, so part of a name finds '
        + 'nothing while the whole name works. Search by a field you know exactly '
        + '(', el('code', { text: 'lifecycleStatus=Launched' }), '), or ',
        el('button', { class: 'link', text: 'try it as an exact name', onclick: () => setQuery({ q: `name=${filter}`, offset: null }) }), '.',
      ) : null,
    ));
    return;
  }

  const { all, cols } = pickColumns(items, loc);

  const picker = el('details', { class: 'node', style: 'margin-bottom:10px' });
  picker.append(el('summary', { text: `columns (${cols.length}/${all.length})` }));
  const pickBox = el('div', { style: 'display:flex;flex-wrap:wrap;gap:10px;padding:6px 0' });
  for (const c of all) {
    const id = `col-${c.replace(/\W/g, '')}`;
    const cb = el('input', { type: 'checkbox', id, checked: cols.includes(c) ? true : undefined });
    cb.addEventListener('change', () => {
      const next = all.filter((x) => (x === c ? cb.checked : cols.includes(x)));
      state.columns.set(key(loc.apiKey, loc.resource), next);
      route();
    });
    pickBox.append(el('label', { style: 'display:flex;gap:4px;align-items:center;font-size:13px' }, cb, c));
  }
  picker.append(pickBox);
  body.append(picker);

  const table = el('table');
  table.append(el('thead', {}, el('tr', {}, cols.map((c) => el('th', { text: c })))));
  const tbody = el('tbody');
  for (const item of items) {
    const tr = el('tr', {});
    for (const c of cols) {
      if (c === 'id' || c === 'href') {
        // Full URNs are enormous: in a table the identifying part is enough.
        const full = item[c] ?? '';
        const short = String(full).split(':').pop() || full;
        tr.append(el('td', { class: 'id' },
          el('a', { href: detailHref(loc, item.id), title: full, text: short || '—' })));
      } else {
        tr.append(el('td', { class: 'wrap' }, renderCell(item[c])));
      }
    }
    tbody.append(tr);
  }
  table.append(tbody);
  body.append(table);
}

// ------------------------------------------------------------------ referenced by

// TMForum keeps no backlinks, so "what names this entity" is a question, not a field: it is
// the query `?<field>.id=<urn>`, which the backend matches against the NGSI-LD relationship.
// These are the resources that *can* name it, straight from the generated metadata — each one
// a link to its list already filtered. Nothing is requested until one is clicked, which is why
// an organization can offer thirty of them without costing anything.
function referencedBy(urn, ownLoc) {
  const type = urnTypeOf(urn);
  const declared = referrersOf(type);
  if (!type || !declared.length) return null;

  const rank = (r) => (r.loc.apiKey === ownLoc.apiKey ? 0 : 1);
  const sorted = [...declared].sort((a, b) =>
    rank(a) - rank(b) || a.loc.resource.localeCompare(b.loc.resource) || a.field.localeCompare(b.field));

  const chip = ({ loc, field }, faint) => el('a', {
    class: `chip${faint ? ' faint' : ''}`,
    href: listHref(loc, `${field}.id=${urn}`),
    title: `${loc.tmf} ${loc.apiLabel}\nGET ${loc.basePath}/${loc.resource}?${field}.id=${urn}`,
  }, el('b', { text: loc.resource }), ` · ${field}`);

  const box = el('div', { class: 'card refby' },
    el('h3', { text: 'Referenced by' }),
    el('p', { class: 'hint', text: 'Resources whose model can point at this entity. Each one is a query, not a stored link: it may well come back empty.' }),
    el('div', { class: 'chips' }, sorted.map((r) => chip(r))),
  );

  // The API stores attributes its model does not declare, so a deployment fed by the BAE has
  // `relatedParty` on a productOffering although TMF620 has no such field. Offered apart,
  // because here the chip really is a guess.
  if (type === 'organization' || type === 'individual') {
    const others = undeclaredReferrersOf(type).filter((r) => r.loc.urnType);
    if (others.length) {
      box.append(el('details', { class: 'refby-more' },
        el('summary', { text: `Try also, on resources that do not declare it (${others.length})` }),
        el('p', { class: 'hint', text: 'These have no relatedParty in the TMForum model. It still finds something when the deployment writes one anyway — which the marketplace does.' }),
        el('div', { class: 'chips' }, others.map((r) => chip(r, true))),
      ));
    }
  }
  return box;
}

// ------------------------------------------------------------------ detail view

async function renderDetail(loc, id) {
  const seq = ++state.renderSeq;
  const main = $('#main');
  clear(main);
  main.append(crumbs(
    { text: 'Overview', href: '#/' },
    { text: `${loc.tmf} ${loc.apiLabel}` },
    { text: loc.resource, href: `#/${loc.apiKey}/${loc.resource}` },
    { text: id.length > 42 ? `${id.slice(0, 39)}…` : id },
  ));
  const body = el('div', {}, el('p', { class: 'spinner', text: 'Loading…' }));
  main.append(body);

  const res = await tmfGet(`${loc.basePath}/${loc.resource}/${encodeURIComponent(id)}`);
  if (seq !== state.renderSeq) return;
  clear(body);

  if (!res.ok) {
    const extra = res.status === 404
      ? 'A 404 here also appears when the URN does not match the NGSI-LD pattern, or when it belongs to another resource.'
      : null;
    body.append(errorBox(res, extra));
    if (res.status === 404) {
      body.append(el('p', {}, el('button', {
        text: 'Look for this URN in the other APIs',
        onclick: async (e) => {
          e.target.textContent = 'searching…';
          const found = await probe({ id }, loc.resource);
          if (found) location.hash = detailHref(found, id);
          else e.target.textContent = 'not in any reachable API';
        },
      })));
    }
    return;
  }

  const entity = res.data ?? {};
  const title = entity.name ?? entity.fullName ?? entity.tradingName ?? loc.resource;
  main.insertBefore(el('h1', { text: title }), body);
  main.insertBefore(el('p', { class: 'subtitle' },
    el('span', { class: 'mono', text: entity.id ?? id }), ' ',
    entity['@type'] ? el('span', { class: 'pill', text: entity['@type'] }) : null, ' ',
    el('button', { class: 'link', text: 'copy curl', onclick: () => copy(`curl -s '${res.url}' | jq`) }), ' ',
    el('button', { class: 'link', text: 'copy URN', onclick: () => copy(entity.id ?? id, 'URN copied') }),
  ), body);

  const showJson = parseHash().query.get('view') === 'json';
  const tabs = el('div', { class: 'tabs' });
  const viewBtn = el('button', { 'aria-selected': String(!showJson), text: 'View' });
  const jsonBtn = el('button', { 'aria-selected': String(showJson), text: 'JSON' });
  tabs.append(viewBtn, jsonBtn);
  body.append(tabs);

  const pretty = el('div', { class: 'card', hidden: showJson }, kvList(entity, { skipHref: true }));
  const rawText = JSON.stringify(entity, null, 2);
  const rawPre = el('pre', { class: 'json', hidden: !showJson }, highlightJson(rawText));
  const copyJson = el('p', { hidden: !showJson }, el('button', { text: 'Copy JSON', onclick: () => copy(rawText, 'JSON copied') }));
  body.append(pretty, copyJson, rawPre);
  const refby = referencedBy(entity.id ?? id, loc);
  if (refby) body.append(refby);

  viewBtn.addEventListener('click', () => setQuery({ view: null }));
  jsonBtn.addEventListener('click', () => setQuery({ view: 'json' }));
}

// ------------------------------------------------------------------ startup

function rememberBase(base) {
  const list = JSON.parse(localStorage.getItem(RECENT_KEY) ?? '[]').filter((x) => x !== base);
  list.unshift(base);
  localStorage.setItem(RECENT_KEY, JSON.stringify(list.slice(0, 8)));
  localStorage.setItem(BASE_KEY, base);
  fillRecent();
}

function fillRecent() {
  const dl = $('#recent');
  clear(dl);
  for (const b of JSON.parse(localStorage.getItem(RECENT_KEY) ?? '[]')) dl.append(el('option', { value: b }));
}

// remember=false for a server-fixed endpoint: the address of somebody else's cluster has no
// business surviving in this browser's localStorage, waiting to reappear as "the last one".
async function connect(raw, { remember = true } = {}) {
  const base = normalizeBase(raw);
  if (!base) return;
  state.base = base;
  $('#endpoint').value = base;
  if (remember) rememberBase(base);
  await detect();
}

// Served from a container: `localhost` in the endpoint box is the container's own loopback,
// not the machine the browser runs on, and a kubectl port-forward lives on the latter. Not
// shown when TMF_ENDPOINT is set: in a pod localhost IS the right answer (the all-in-one is
// the neighbouring container), and the note would be telling the operator to break it.
function renderConnHint() {
  const box = $('#conn-hint');
  if (!state.cfg.container || state.cfg.locked) { box.hidden = true; return; }
  box.hidden = false;
  box.innerHTML = 'Running in a container: your machine is <code>host.docker.internal</code>, '
    + 'not <code>localhost</code> — a <code>port-forward</code> on the host is reached there '
    + '(on Linux, run with <code>--add-host=host.docker.internal:host-gateway</code>).';
}

// With the endpoint fixed there is nothing to connect to: the button re-runs the detection.
function lockEndpoint(why) {
  const input = $('#endpoint');
  input.readOnly = true;
  input.removeAttribute('list');
  input.title = why;
  $('#conn').classList.add('locked');
  $('#connect-btn').textContent = 'Reload';
  $('#connect-btn').title = 'Probe the APIs again';
}

$('#conn').addEventListener('submit', (e) => {
  e.preventDefault();
  if ($('#endpoint').readOnly) detect();
  else connect($('#endpoint').value);
});
window.addEventListener('hashchange', route);

fillRecent();
(async () => {
  let cfg = {};
  try {
    // Relative on purpose: under the all-in-one this page lives at /ui/, not at the root.
    cfg = await (await fetch('config.json')).json();
  } catch { /* never mind */ }
  state.cfg = cfg;
  renderConnHint();

  // Served by the all-in-one: the API is this very origin, and there is nothing to choose.
  if (cfg.sameOrigin) {
    lockEndpoint('Served by the all-in-one itself: the API is this origin');
    await connect(location.origin, { remember: false });
    return;
  }

  // The dev server was started with TMF_ENDPOINT: connect on load and let nobody change
  // it — not even through ?endpoint=, which the proxy would reject anyway.
  if (cfg.locked && cfg.endpoint) {
    lockEndpoint(`Fixed by the server (TMF_ENDPOINT=${cfg.endpoint})`);
    await connect(cfg.endpoint, { remember: false });
    return;
  }

  // If the server is in mock mode, offer that endpoint.
  if (cfg.mock) {
    $('#mock-hint').textContent = cfg.mockBase;
    if (!localStorage.getItem(BASE_KEY)) $('#endpoint').value = cfg.mockBase;
  }

  // ?endpoint=... makes it possible to share a link that already points at an environment.
  const fromUrl = new URLSearchParams(location.search).get('endpoint');
  const last = fromUrl ?? localStorage.getItem(BASE_KEY);
  if (last) { $('#endpoint').value = last; await connect(last); }
})();
