# tmf-ui

A **read-only** browser for the TMForum APIs of the **all-in-one** deployment: list the
entities of all 20 APIs, open one, and **jump from entity to entity through hyperlinks**
(`productOffering` → `productSpecification` → `organization`, …).

It only ever issues `GET`. It creates nothing, changes nothing and deletes nothing.

The module holds static files only — no Java, and no Node in the Maven build. The all-in-one
depends on its jar and serves it when asked to.

## Serving it from the all-in-one

Off by default. Turn it on with one environment variable:

```bash
TMF_UI_ENABLED=true
```

and open `http://<all-in-one>:8632/ui/` (the trailing slash matters: the page loads its
scripts relative to it).

It is served on the API's own port and origin, so there is no CORS to work around, no proxy,
and nothing to configure: the page connects to the origin it was loaded from and the endpoint
box is read-only. The mapping lives in `all-in-one/src/main/resources/application.yaml`
under `micronaut.router.static-resources.ui`.

It inherits whatever stands in front of the API. Behind a gateway that demands a token (the
FIWARE Data Space Connector's `mp-tmf-api.*` hosts want a JWT from the VCVerifier) the page is
not reachable either, and behind an ingress that publishes the API under a sub-path the
request URLs will not match. Both are out of scope: this is for direct access, a
port-forward or an ingress with no PEP.

```bash
kubectl -n <namespace> port-forward svc/tm-forum-api-svc 8632:8080
# http://localhost:8632/ui/
```

## Running it locally against any endpoint

For a browser on a laptop pointed at *any* TMForum endpoint — a port-forward, a remote
ingress, sample data — there is a small development server, Node ≥ 18 and no dependencies:

```
cd ui
npm start            # http://localhost:8080
npm run mock         # the same, with sample data (no cluster needed)
npm run gen-catalog  # regenerate the catalogue from this repository
```

Enter the base endpoint of the all-in-one — **the root, with no `/tmf-api/...`** — and press
*Connect*. A link can also carry the environment with it:
`http://localhost:8080/?endpoint=http://localhost:8632#/product-catalog/productOffering`

It serves the very same files the jar ships (`src/main/resources/tmf-ui/`) and answers
`config.json` itself, which is how the page knows to go through the server's proxy rather than
straight to its own origin. The proxy exists because neither `tmforum-api` nor the
`tm-forum-api` chart nor the usual ingresses enable **CORS**: a page served from any other
origin could send a request and never read the answer. It accepts nothing but `GET` over
`http`/`https`, and forwards `X-Total-Count` and `Link` along with the body.

Against a local demo with `*.nip.io` hosts, a self-signed certificate and a forward proxy:

```bash
TMF_INSECURE=1 NODE_USE_ENV_PROXY=1 HTTPS_PROXY=http://localhost:8888 npm start
# endpoint:  https://tm-forum-api.127.0.0.1.nip.io
```

| Variable | Effect |
|---|---|
| `PORT` | Local port (8080 by default) |
| `TMF_ENDPOINT` | Fix the endpoint: the page connects to it on load, the box goes read-only and the proxy accepts that origin only |
| `TMF_INSECURE=1` | Accept self-signed certificates |
| `TMF_MOCK=1` | Expose a fake TMForum at `/mock` (`dev/fixtures/data.json`) |
| `TMF_TIMEOUT_MS` | Per-request timeout (20000 by default) |
| `NODE_USE_ENV_PROXY=1` | Honour `HTTPS_PROXY` / `HTTP_PROXY` |

The proxy accepts any URL unless `TMF_ENDPOINT` is set. That is a convenience on a laptop and
an SSRF anywhere somebody else can reach the port, which is one more reason the deployed path
is the all-in-one and not this server.

## What it can do

- **Detection**: on connect it probes all 42 resources of the 20 APIs with `limit=1` and
  marks which ones answer, with their entity count whenever the backend returns
  `X-Total-Count`.
- **Lists**: `offset`/`limit` pagination, sorting (`sort=name,-lastUpdate`), a column picker
  and filters. The filter **applies as you type** (and clearing the box brings everything
  back — no Enter anywhere), and takes the backend's own syntax
  (`lifecycleStatus=Launched&relatedParty.id=urn:...`, the `.regex` `.gt` `.lt` operators,
  `,` as OR); bare text is translated to `name.regex=<text>`.
- **Detail**: a collapsible tree of the entity and a **syntax-highlighted JSON** tab (the
  active tab lives in the hash: `…?view=json`), plus *copy curl* with the real URL.
- **Hyperlinks between entities**, in both directions (see below).
- All the state is in the hash (`#/<api>/<resource>?offset=…`), so back/forward and
  bookmarks work.

## How the links between entities are resolved

In `tmforum-api` the `href` field **is not a URL**: the mappers do
`@Mapping(target = "href", source = "id")`, so `href == id == urn:ngsi-ld:<type>:<uuid>`.
Turning a reference into a link means working out which resource that URN belongs to.
`src/main/resources/tmf-ui/refs.js` tries, in this order:

1. The reference's **`@referredType`** (`ProductSpecification` → `productSpecification`).
2. **The type carried by the URN itself** (`urn:ngsi-ld:product-specification:…`). Careful:
   that type comes from the `TYPE_*` constants of the domain classes and is not uniform —
   kebab-case (`product-specification`) and camelCase (`billingCycleSpecification`) are
   mixed, which is why the catalogue is generated from the repo rather than guessed.
3. **The name of the field** holding the reference.
4. If it is still ambiguous (the usual case: `relatedParty` with no `@referredType`, which
   can be an `Individual` or an `Organization`), the *find…* button probes the candidate
   resources until one answers 200.

When one URN legitimately fits several APIs (`resource` and `resourceSpecification` exist in
TMF639 / TMF634 and in TMF730), the link goes to the likeliest one and the rest are offered
in a dropdown.

## Referenced by: the other direction

The links above go one way — from an entity to what it names. The question you usually have is
the reverse one ("which offerings does this organization have?"), and TMForum has no answer to
it: there are no backlinks, only a query, `?<field>.id=<urn>`.

So the detail view ends in a **Referenced by** panel: one chip per resource whose model can
point at this entity, each a link to that resource's list *already filtered*. Nothing is
requested until you click one — an organization offers around thirty.

Which resource can point where is generated, not written by hand. `gen-catalog.mjs` reads the
`@AttributeGetter`/`@AttributeSetter` annotations of the domain classes and keeps only
`RELATIONSHIP` and `RELATIONSHIP_LIST` fields, because for those the backend strips the `.id`
and matches against the NGSI-LD relationship. Two reasons that matters:

- The field is not always `relatedParty`. An `agreement` calls it **`engagedParty`**, and so
  does a `customer`. A hand-written list would have missed them without saying so.
- `attachment` and friends look identical in the JSON but are a `PROPERTY_LIST`, where the
  same query matches a structured value and quietly returns nothing. They are not offered.

A second, collapsed group — *try also, on resources that do not declare it* — exists because
the API stores attributes its model does not know: a deployment fed by the BAE has
`relatedParty` on a `productOffering` even though TMF620 declares none, and that query does
work. Those chips are guesses and look like it.

**The matching is the broker's, not ours.** Verified against a real deployment:
`relatedParty.id=<org>` returns 4 of 25 `productSpecification` and 1 of 21 `productOffering`,
and a URN that names nobody returns 0 — but `name.regex=` is matched *exactly* there, so a
fragment of a name finds nothing while the whole name works. The empty-state message says so
when it happens.

## The catalogue

`src/main/resources/tmf-ui/catalog.js` is **generated** from this repository by
`scripts/gen-catalog.mjs`, and committed so that neither the build nor the page needs Node:

- `all-in-one/src/main/resources/application.yaml` → each API's `basePath` (the canonical
  source: the `basePath` in the specs disagrees for `document-management`, `party-role` and
  `quote`).
- `api/tm-forum/<module>/api.json` → the resources and each one's entity type.
- The `TYPE_*` constants of the domain classes → the type that appears inside the URNs.
- The `@AttributeGetter`/`@AttributeSetter` annotations → which field of a resource
  references which entity type, which is what the *Referenced by* chips are built from.

**Regenerate it whenever one of those changes** — a new API, a new resource, a new `TYPE_*`:

```bash
npm run gen-catalog
```

CI (`.github/workflows/ui.yml`) regenerates it on every pull request and fails if the result
differs from what is committed, so a stale catalogue cannot be merged.

## Known limits

- **All-in-one only.** A deployment with one pod per module serves each API at `/`, which
  this UI does not handle.
- No authentication: see *Serving it from the all-in-one*.
- `X-Total-Count` only arrives when the broker returns `NGSILD-Results-Count`; otherwise the
  counter says "total unknown" and pagination is driven by the `Link` header.
- Task operations (`heal`, `migrate`, `cancelProductOrder`, …) and the `hub`/`listener`
  notification endpoints do not show up: they are not navigable entities.
