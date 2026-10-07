#!/usr/bin/env node
// Regenerates src/main/resources/tmf-ui/catalog.js from this repository.
//
//   node scripts/gen-catalog.mjs [path-to-tmforum-api]   (defaults to the root of this repo)
//
// Sources:
//   - all-in-one/src/main/resources/application.yaml  -> each API's basePath (canonical)
//   - api/tm-forum/<module>/api.json                  -> resources and entity type
//   - the domain classes (*.java)                     -> URN type, and which fields hold a
//                                                        reference to which entity type
//
// The output is committed, so neither the Maven build nor the UI needs Node to run. CI
// regenerates it and fails on a diff, so it cannot drift from the APIs it describes.

import { readFileSync, writeFileSync, readdirSync, existsSync, statSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
// ui/scripts/ -> the repository root.
const REPO = process.argv[2] ?? join(HERE, '..', '..');

// Metadata the repo does not hold reliably: product-catalog's pom says TMF632 by mistake,
// and document-management has no ctk script-folder at all.
const META = {
  account: { tmf: 'TMF666', label: 'Account Management', group: 'Billing' },
  agreement: { tmf: 'TMF651', label: 'Agreement Management', group: 'Agreements' },
  'customer-bill-management': { tmf: 'TMF678', label: 'Customer Bill', group: 'Billing' },
  'customer-management': { tmf: 'TMF629', label: 'Customer Management', group: 'Parties' },
  'document-management': { tmf: 'TMF667', label: 'Document Management', group: 'Other' },
  'party-catalog': { tmf: 'TMF632', label: 'Party', group: 'Parties' },
  'party-role': { tmf: 'TMF669', label: 'Party Role', group: 'Parties' },
  'product-catalog': { tmf: 'TMF620', label: 'Product Catalog', group: 'Catalogue' },
  'product-inventory': { tmf: 'TMF637', label: 'Product Inventory', group: 'Inventory' },
  'product-ordering-management': { tmf: 'TMF622', label: 'Product Ordering', group: 'Ordering' },
  quote: { tmf: 'TMF648', label: 'Quote', group: 'Agreements' },
  'resource-catalog': { tmf: 'TMF634', label: 'Resource Catalog', group: 'Catalogue' },
  'resource-function-activation': { tmf: 'TMF664', label: 'Resource Function Activation', group: 'Inventory' },
  'resource-inventory': { tmf: 'TMF639', label: 'Resource Inventory', group: 'Inventory' },
  'resource-order-management': { tmf: 'TMF652', label: 'Resource Ordering', group: 'Ordering' },
  'service-catalog': { tmf: 'TMF633', label: 'Service Catalog', group: 'Catalogue' },
  'service-inventory': { tmf: 'TMF638', label: 'Service Inventory', group: 'Inventory' },
  'service-order-management': { tmf: 'TMF641', label: 'Service Ordering', group: 'Ordering' },
  'software-management': { tmf: 'TMF730', label: 'Software Compute', group: 'Other' },
  'usage-management': { tmf: 'TMF635', label: 'Usage Management', group: 'Other' },
};

// Resources that are not navigable entities (TMF630 pub/sub and task operations).
const SKIP = new Set([
  'hub', 'listener',
  'cancelProductOrder', 'cancelResourceOrder', 'cancelServiceOrder',
  'customerBillOnDemand', 'heal', 'migrate', 'monitor', 'scale',
  'importJob', 'exportJob',
]);


// One pass over every domain class. It yields two things:
//
//   urnTypes    class -> the type that shows up in the URN (`urn:ngsi-ld:<type>:<uuid>`), from
//               the class's TYPE_* constant. Not derivable from the name: kebab-case
//               (product-specification) and camelCase (billingCycleSpecification) are mixed.
//   classes     the raw material for the reference metadata: the relationship fields a class
//               declares, and, for a `*Ref` class, the entity types it points at.
function scanJava(repo) {
  const urnTypes = {};
  const consts = {};   // TYPE_FOO -> "foo", across the whole repo
  const classes = {};  // simple class name -> { attrs, referencedTypes }

  const parseArgs = (s) => ({
    type: /AttributeType\.([A-Z_]+)/.exec(s)?.[1] ?? null,
    targetName: /targetName\s*=\s*"([^"]+)"/.exec(s)?.[1] ?? null,
    targetClass: /targetClass\s*=\s*([A-Za-z0-9_]+)\.class/.exec(s)?.[1] ?? null,
  });

  const parse = (src) => {
    const cls = /\b(?:class|abstract class)\s+([A-Z][A-Za-z0-9]*)/.exec(src);
    if (!cls) return;
    const name = cls[1];

    for (const m of src.matchAll(/public static final String (TYPE_[A-Z_0-9]+)\s*=\s*"([^"]+)"/g)) {
      consts[m[1]] = m[2];
      if (urnTypes[name] === undefined) urnTypes[name] = m[2];
    }

    // Getters carry the attribute type and the queryable name; setters carry the target class.
    // Keyed by targetName because that is what the backend matches a filter against - it is
    // not always the Java field name.
    const attrs = {};
    for (const m of src.matchAll(/@AttributeGetter\(([^)]*)\)/g)) {
      const a = parseArgs(m[1]);
      if (a.targetName) attrs[a.targetName] = { ...(attrs[a.targetName] ?? {}), type: a.type };
    }
    for (const m of src.matchAll(/@AttributeSetter\(([^)]*)\)/g)) {
      const a = parseArgs(m[1]);
      if (a.targetName && a.targetClass) {
        attrs[a.targetName] = { ...(attrs[a.targetName] ?? {}), targetClass: a.targetClass };
      }
    }

    // `public List<String> getReferencedTypes() { return new ArrayList<>(List.of(...)); }`
    const body = /getReferencedTypes\(\)\s*\{([\s\S]{0,400}?)\n\s*\}/.exec(src);
    const listed = body ? /List\.of\(([^)]*)\)/.exec(body[1])?.[1] ?? '' : null;
    const referencedTypes = listed === null ? null : listed
      .split(',').map((t) => t.trim()).filter(Boolean)
      // A quoted literal, or a TYPE_* constant (possibly qualified). Anything else is
      // `getAtReferredType()`: the type is only known at runtime, so there is nothing to emit.
      .map((t) => /^"([^"]*)"$/.exec(t)?.[1] ?? consts[t.split('.').pop()] ?? null);

    // Same simple name in two packages happens (ResourceSpecificationRef). Merge rather than
    // let one win: an extra candidate is a button that finds nothing, a missing one is a dead end.
    const prev = classes[name];
    classes[name] = {
      attrs: { ...(prev?.attrs ?? {}), ...attrs },
      referencedTypes: [...new Set([...(prev?.referencedTypes ?? []), ...(referencedTypes ?? [])])],
    };
  };

  const walk = (dir) => {
    for (const e of readdirSync(dir, { withFileTypes: true })) {
      const p = join(dir, e.name);
      if (e.isDirectory()) {
        if (e.name === 'target' || e.name === '.git') continue;
        walk(p);
      } else if (e.name.endsWith('.java')) {
        parse(readFileSync(p, 'utf8'));
      }
    }
  };
  for (const mod of readdirSync(repo)) {
    const src = join(repo, mod, 'src', 'main', 'java');
    if (existsSync(src) && statSync(src).isDirectory()) walk(src);
  }
  return { urnTypes, classes };
}

// The reference fields of one entity class, as the UI needs them: the name to filter by, and
// the entity types it can point at.
//
// Only RELATIONSHIP and RELATIONSHIP_LIST. Those are stored as NGSI-LD relationships, and for
// them the backend's QueryParser strips the `.id` and matches against the relationship object
// - which is what makes `?<field>.id=<urn>` work, arrays included (each entry is a separate
// attribute instance with its own datasetId, not a JSON array). A PROPERTY_LIST of
// `*RefOrValue` looks the same in the JSON but queries as a structured value, so offering it
// would produce buttons that quietly find nothing.
function refsOf(entityType, classes) {
  const cls = classes[entityType];
  if (!cls) return [];
  const out = [];
  for (const [field, a] of Object.entries(cls.attrs)) {
    if (a.type !== 'RELATIONSHIP' && a.type !== 'RELATIONSHIP_LIST') continue;
    const targets = (classes[a.targetClass]?.referencedTypes ?? []).filter(Boolean);
    if (!targets.length) continue;
    out.push({ field, many: a.type === 'RELATIONSHIP_LIST', targets: [...new Set(targets)].sort() });
  }
  return out.sort((x, y) => x.field.localeCompare(y.field));
}

function parseBasePaths(yamlPath) {
  const text = readFileSync(yamlPath, 'utf8');
  // The `api:` block, with entries `  <key>:\n    basepath: <path>`
  const out = {};
  const re = /^ {2}([a-z0-9-]+):\s*\n\s+basepath:\s*(\S+)\s*$/gm;
  let m;
  while ((m = re.exec(text)) !== null) out[m[1]] = m[2].replace(/\/+$/, '');
  return out;
}

function resourcesOf(spec) {
  const byName = new Map();
  for (const [p, ops] of Object.entries(spec.paths ?? {})) {
    const m = /^\/([A-Za-z0-9]+)$/.exec(p);
    if (!m || !ops.get) continue;
    const name = m[1];
    if (SKIP.has(name)) continue;
    byName.set(name, { name, entityType: null });
  }
  // Entity type from GET /<resource>/{id}: this is what `@referredType` is matched against.
  for (const [p, ops] of Object.entries(spec.paths ?? {})) {
    const m = /^\/([A-Za-z0-9]+)\/\{id\}$/.exec(p);
    if (!m || !ops.get || !byName.has(m[1])) continue;
    const ref = ops.get.responses?.['200']?.schema?.$ref ?? '';
    const t = ref.split('/').pop();
    if (t) byName.get(m[1]).entityType = t;
  }
  return [...byName.values()].sort((a, b) => a.name.localeCompare(b.name));
}

const yamlPath = join(REPO, 'all-in-one', 'src', 'main', 'resources', 'application.yaml');
const specsDir = join(REPO, 'api', 'tm-forum');
if (!existsSync(yamlPath) || !existsSync(specsDir)) {
  console.error(`Cannot find tmforum-api at ${REPO}.\nUsage: node scripts/gen-catalog.mjs [path-to-tmforum-api]`);
  process.exit(1);
}

const basePaths = parseBasePaths(yamlPath);
const { urnTypes, classes } = scanJava(REPO);
const apis = [];
for (const key of readdirSync(specsDir).sort()) {
  const specFile = join(specsDir, key, 'api.json');
  if (!existsSync(specFile)) continue;
  const basePath = basePaths[key];
  if (!basePath) {
    console.warn(`! ${key}: no basepath in all-in-one/application.yaml, skipped`);
    continue;
  }
  const spec = JSON.parse(readFileSync(specFile, 'utf8'));
  const meta = META[key] ?? { tmf: '', label: key, group: 'Other' };
  const resources = resourcesOf(spec).map((r) => ({
    ...r,
    urnType: r.entityType ? (urnTypes[r.entityType] ?? null) : null,
    refs: r.entityType ? refsOf(r.entityType, classes) : [],
  }));
  apis.push({ key, ...meta, basePath, resources });
}

const banner = `// GENERATED by scripts/gen-catalog.mjs from the tmforum-api repo. Do not edit by hand.
// basePaths from: all-in-one/src/main/resources/application.yaml
// resources from: api/tm-forum/<module>/api.json
// refs      from: the @AttributeGetter/@AttributeSetter annotations of the domain classes
`;
const body = `export const GROUPS = ${JSON.stringify(['Catalogue', 'Inventory', 'Ordering', 'Parties', 'Billing', 'Agreements', 'Other'])};\n\nexport const APIS = ${JSON.stringify(apis, null, 2)};\n`;
const outFile = join(HERE, '..', 'src', 'main', 'resources', 'tmf-ui', 'catalog.js');
writeFileSync(outFile, banner + '\n' + body);

const total = apis.reduce((n, a) => n + a.resources.length, 0);
const refs = apis.reduce((n, a) => n + a.resources.reduce((m, r) => m + r.refs.length, 0), 0);
const noUrnType = apis.flatMap((a) => a.resources.filter((r) => !r.urnType).map((r) => `${a.key}/${r.name}`));
const noRefs = apis.flatMap((a) => a.resources.filter((r) => !r.refs.length).map((r) => `${a.key}/${r.name}`));
console.log(`${outFile}: ${apis.length} APIs, ${total} resources, ${refs} reference fields`);
if (noUrnType.length) console.log(`  no known URN type: ${noUrnType.join(', ')}`);
if (noRefs.length) console.log(`  no reference fields: ${noRefs.join(', ')}`);
