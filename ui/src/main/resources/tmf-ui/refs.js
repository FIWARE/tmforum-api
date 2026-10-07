// Resolving references between TMForum entities.
//
// The detail that matters: in tmforum-api `href` is NOT a navigable URL — the mappers do
// `@Mapping(target="href", source="id")`, so href == id == urn:ngsi-ld:<type>:<uuid>.
// We have to work out which resource that URN belongs to and build the URL ourselves.

import { APIS } from './catalog.js';

export const LOCATIONS = APIS.flatMap((api) =>
  api.resources.map((r) => ({
    apiKey: api.key,
    apiLabel: api.label,
    tmf: api.tmf,
    basePath: api.basePath,
    resource: r.name,
    entityType: r.entityType,
    urnType: r.urnType,
    refs: r.refs ?? [],
  })),
);

function indexBy(fn) {
  const m = new Map();
  for (const loc of LOCATIONS) {
    const k = fn(loc);
    if (!k) continue;
    if (!m.has(k)) m.set(k, []);
    m.get(k).push(loc);
  }
  return m;
}

const BY_RESOURCE = indexBy((l) => l.resource.toLowerCase());
const BY_ENTITY = indexBy((l) => l.entityType?.toLowerCase());
const BY_URN_TYPE = indexBy((l) => l.urnType?.toLowerCase());

export function locate(apiKey, resource) {
  return LOCATIONS.find((l) => l.apiKey === apiKey && l.resource === resource) ?? null;
}

export const URN_RE = /^urn:ngsi-ld:([A-Za-z0-9-]{1,63}):.+$/;

/** The type declared inside the URN itself: urn:ngsi-ld:product-specification:xxx -> product-specification */
export function urnTypeOf(id) {
  const m = URN_RE.exec(String(id ?? ''));
  return m ? m[1] : null;
}

/** Does this value look like a reference to another entity? */
export function isRefLike(value) {
  return (
    value && typeof value === 'object' && !Array.isArray(value) &&
    typeof value.id === 'string' && URN_RE.test(value.id)
  );
}

const lowerFirst = (s) => (s ? s[0].toLowerCase() + s.slice(1) : s);

/**
 * Candidate target resources for a reference, most trustworthy first.
 *   1. `@referredType` (matches the entity type from the spec)
 *   2. the type carried by the URN itself
 *   3. the name of the field holding it
 * Returns [] when there is no way to tell (e.g. `relatedParty` with no `@referredType`),
 * in which case it has to be probed — see `probeCandidates`.
 */
export function candidatesFor(ref, fieldName) {
  const out = [];
  const add = (locs) => {
    for (const l of locs ?? []) if (!out.includes(l)) out.push(l);
  };

  const referred = ref?.['@referredType'];
  if (referred) {
    add(BY_ENTITY.get(String(referred).toLowerCase()));
    add(BY_RESOURCE.get(lowerFirst(String(referred)).toLowerCase()));
  }

  const urnType = urnTypeOf(ref?.id);
  if (urnType) add(BY_URN_TYPE.get(urnType.toLowerCase()));

  if (fieldName) {
    const f = String(fieldName).toLowerCase();
    add(BY_RESOURCE.get(f));
    if (f.endsWith('s')) add(BY_RESOURCE.get(f.slice(0, -1)));
  }

  return out;
}

/**
 * When the target cannot be deduced: the resources worth asking. Ordered by affinity with
 * the field name and the role (`relatedParty` is usually an Individual or an Organization).
 */
export function probeCandidates(ref, fieldName) {
  const f = String(fieldName ?? '').toLowerCase();
  const score = (l) => {
    let s = 0;
    if (f.includes('party')) s += l.apiKey === 'party-catalog' ? 3 : l.apiKey === 'party-role' ? 2 : 0;
    if (f.includes(l.resource.toLowerCase())) s += 3;
    if (l.resource.toLowerCase().includes(f)) s += 1;
    return s;
  };
  return [...LOCATIONS].sort((a, b) => score(b) - score(a));
}

// ------------------------------------------------------------------ the other direction

/**
 * Who can point *at* an entity of this URN type: `urnType -> [{ loc, field, many }]`, built
 * from the `refs` the catalogue generator reads off the domain classes.
 *
 * There is no such thing as a stored backlink in TMForum — the answer is a query,
 * `?<field>.id=<urn>`, which the backend turns into a match against the NGSI-LD relationship.
 * So this index says where it is worth *asking*, not where something was found.
 */
const REVERSE = new Map();
for (const loc of LOCATIONS) {
  for (const ref of loc.refs) {
    for (const target of ref.targets ?? []) {
      const k = String(target).toLowerCase();
      if (!REVERSE.has(k)) REVERSE.set(k, []);
      REVERSE.get(k).push({ loc, field: ref.field, many: ref.many });
    }
  }
}

/** The (resource, field) pairs that can reference an entity of this URN type. */
export function referrersOf(urnType) {
  return urnType ? (REVERSE.get(String(urnType).toLowerCase()) ?? []) : [];
}

/**
 * The resources that do NOT declare a reference to this type but might still carry one in the
 * data: the API accepts attributes the model does not know and stores them, so a deployment
 * fed by the BAE has `relatedParty` on a `productOffering`, which TMF620 does not declare.
 * Offered separately, and only for the field that actually happens in practice.
 */
export function undeclaredReferrersOf(urnType, field = 'relatedParty') {
  const declared = new Set(referrersOf(urnType).map((r) => `${r.loc.apiKey}/${r.loc.resource}`));
  return LOCATIONS
    .filter((l) => !declared.has(`${l.apiKey}/${l.resource}`))
    .map((loc) => ({ loc, field, many: true, undeclared: true }));
}

/** Human-readable label for rendering a reference. */
export function refLabel(ref) {
  return ref.name ?? ref.fullName ?? ref.tradingName ?? ref.role ?? ref.id;
}
