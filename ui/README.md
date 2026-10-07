# tmf-ui

A **read-only** browser for the TMForum APIs of the **all-in-one** deployment: list the
entities of all 20 APIs, open one, and **jump from entity to entity through hyperlinks**
(`productOffering` → `productSpecification` → `organization`, …).

It only ever issues `GET`. It creates nothing, changes nothing and deletes nothing.

It is an image of its own, `quay.io/fiware/tmforum-ui`, not part of the all-in-one: deploy it
next to the all-in-one when you want it (a sidecar), run it on its own to debug an environment,
or leave it out. The module holds the page (`src/main/resources/tmf-ui/`) and the small Node
server that serves it (`server/server.mjs`, no dependencies). There is no Java in it and no Node
in the Maven build: in the `oci` profile, jib copies both onto a Node.js base image.

The server does three things: it serves the page, answers `config.json` (how it was started),
and proxies the page's `GET`s to the TMForum endpoint. The proxy exists because neither
`tmforum-api` nor the `tm-forum-api` chart nor the usual ingresses enable **CORS**: a page served
from any other origin could send a request and never read the answer. It accepts nothing but
`GET` over `http`/`https`, and forwards `X-Total-Count` and `Link` along with the body.

## Running the image

**As a sidecar** of the all-in-one, the API is on the pod's loopback. Fix the endpoint so
the page connects on load and the proxy goes nowhere else:

```yaml
- name: tmf-ui
  image: quay.io/fiware/tmforum-ui:<version>
  env:
    - name: TMF_ENDPOINT
      value: http://localhost:8632
  ports:
    - containerPort: 8080
  readinessProbe:
    httpGet: { path: /healthz, port: 8080 }
```

and expose port 8080 through the service, or reach it with a port-forward:

```bash
kubectl -n <namespace> port-forward <all-in-one-pod> 8080:8080
# http://localhost:8080/
```

**On its own, to debug**, against a port-forward of the all-in-one on your machine:

```bash
kubectl -n <namespace> port-forward svc/tm-forum-api-svc 8632:8080
docker run --rm -p 8080:8080 -e TMF_ENDPOINT=http://host.docker.internal:8632 quay.io/fiware/tmforum-ui
# on Linux, add --add-host=host.docker.internal:host-gateway
```

or with sample data and no cluster: `docker run --rm -p 8080:8080 -e TMF_MOCK=1 quay.io/fiware/tmforum-ui`.

It talks to the API directly, so it does not get through a gateway that demands a token (the
FIWARE Data Space Connector's `mp-tmf-api.*` hosts want a JWT from the VCVerifier): point it at
the all-in-one itself, a port-forward or an ingress with no PEP. It has no authentication of its
own either — whoever reaches its port reads the API.

## Running it locally against any endpoint

The same server without the image, Node ≥ 18 and no dependencies:

```
cd ui
npm start            # http://localhost:8080
npm run mock         # the same, with sample data (no cluster needed)
npm run gen-catalog  # regenerate the catalogue from this repository
```

Without `TMF_ENDPOINT`, enter the base endpoint of the all-in-one — **the root, with no
`/tmf-api/...`** — and press *Connect*. A link can also carry the environment with it:
`http://localhost:8080/?endpoint=http://localhost:8632#/product-catalog/productOffering`

Against a local demo with `*.nip.io` hosts, a self-signed certificate and a forward proxy:

```bash
TMF_INSECURE=1 NODE_USE_ENV_PROXY=1 HTTPS_PROXY=http://localhost:8888 npm start
# endpoint:  https://tm-forum-api.127.0.0.1.nip.io
```

## Configuration

| Variable | Effect |
|---|---|
| `PORT` | Port to listen on (8080 by default) |
| `TMF_ENDPOINT` | Fix the endpoint: the page connects to it on load, the box goes read-only and the proxy accepts that origin only |
| `TMF_INSECURE=1` | Accept self-signed certificates |
| `TMF_MOCK=1` | Expose a fake TMForum at `/mock` (`server/fixtures/data.json`) |
| `TMF_TIMEOUT_MS` | Per-request timeout (20000 by default) |
| `TMF_UI_STATIC_DIR` | Where the page's files are (set by the image; `src/main/resources/tmf-ui` otherwise) |
| `NODE_USE_ENV_PROXY=1` | Honour `HTTPS_PROXY` / `HTTP_PROXY` |

`GET /healthz` answers 200 for probes.

**Set `TMF_ENDPOINT` wherever somebody else can reach the port.** Without it the proxy accepts
any URL: a convenience on a laptop and an SSRF anywhere else.

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
- No authentication: see *Running the image*.
- `X-Total-Count` only arrives when the broker returns `NGSILD-Results-Count`; otherwise the
  counter says "total unknown" and pagination is driven by the `Link` header.
- Task operations (`heal`, `migrate`, `cancelProductOrder`, …) and the `hub`/`listener`
  notification endpoints do not show up: they are not navigable entities.
