# Analysis: what governs access to TMForum resources today

The requirement comes from the [FIWARE Data Space Connector](https://github.com/FIWARE/data-space-connector)
(DSC), which deploys this project together with an identity stack and an ODRL policy engine. This
document records what that deployment enforces today, what it cannot enforce, and which of the gaps
are this project's to close. References point at the DSC repository at `main`; they were checked
against DSC chart version 10.21.x.

- [Three policy planes, not one](#three-policy-planes-not-one)
- [Where each plane is enforced](#where-each-plane-is-enforced)
- [Who calls these APIs](#who-calls-these-apis)
- [The gap, stated precisely](#the-gap-stated-precisely)
- [What this project already has](#what-this-project-already-has)
- [Constraints that shape any solution](#constraints-that-shape-any-solution)

## Three policy planes, not one

"Policy" means three different things in that deployment, written in three places and read by three
components. Conflating them is the main source of confusion around this feature.

### Plane 1 — the data-plane policy (`authorizationPolicy`)

An ODRL policy carried as a characteristic on a `ProductSpecification` or a `ServiceSpecification`:

```json
{ "id": "policyConfig", "valueType": "authorizationPolicy",
  "productSpecCharacteristicValue": [ { "value": { "@type": "odrl:Policy", "odrl:permission": { … } } } ] }
```

A separate component (`contract-management`) reads it **when a `ProductOrder` completes** and
installs it at the policy engine, keyed by `odrl:uid` plus the order id. From then on it governs the
*purchased service* — a request to the provider's data API, enforced by a gateway. See
[CENTRAL_MARKETPLACE.md](https://github.com/FIWARE/data-space-connector/blob/main/doc/CENTRAL_MARKETPLACE.md)
and [COMPOSED_SPECIFICATIONS.md](https://github.com/FIWARE/data-space-connector/blob/main/doc/deployment-integration/roles/provider/COMPOSED_SPECIFICATIONS.md).

**It says nothing about who may see the offering.** It is installed only after the offering has
already been found and bought.

### Plane 2 — the access/contract policy pair (`productOfferingTerm`)

Two ODRL policies on the `ProductOffering` itself — **entities this project stores and serves**:

```json
"productOfferingTerm": [ { "name": "edc:contractDefinition",
  "accessPolicy":   { "@type": "Offer", "permission": [ … ] },
  "contractPolicy": { "@type": "Offer", "permission": [ … ] } } ]
```

The DSC's EDC-based connector turns the pair into a `ContractDefinition` and evaluates them
post-authentication, with the counter-party's verified credentials in hand
([DSP_INTEGRATION.md](https://github.com/FIWARE/data-space-connector/blob/main/doc/DSP_INTEGRATION.md#policy-evaluation-in-dsp-flows)):

| Policy | Evaluation scope | Effect |
|---|---|---|
| access policy | `catalog` | the asset does not appear in the catalog at all |
| contract policy | `contract.negotiation` | the asset is visible, negotiation is rejected |
| either, scoped | `transfer.process` | the transfer is refused |

This is **exactly the semantics the feature needs** — per-offering, credential-aware, visibility
separate from contracting. It exists only on the Dataspace Protocol path. The policies sit in
entities this project serves, and this project does not read them.

### Plane 3 — the API protection policy

ODRL policies installed at the policy engine by the operator, before anyone uses the API, and
enforced by the gateway in front of it. Examples ship in the DSC repository under
[`it/src/test/resources/policies/`](https://github.com/FIWARE/data-space-connector/tree/main/it/src/test/resources/policies):

```jsonc
// allowProductOffering.json — "anybody may read product offerings"
"odrl:target":   { "odrl:refinement": [ { "odrl:leftOperand": "tmf:resource",
                                          "odrl:operator": "odrl:eq",
                                          "odrl:rightOperand": "productOffering" } ] },
"odrl:assignee": { "@id": "vc:any" },
"odrl:action":   { "@id": "odrl:read" }
```

The vocabulary is `tmf:resource` (a resource **type**), `tmf:create`, `http:path`, `vc:type`,
`vc:role`. The granularity is *(resource type × HTTP method × credential type/role)*. There is no
term for *this particular offering*, and no mechanism that could evaluate one: the decision is taken
by a gateway before the entity has been loaded.

## Where each plane is enforced

| Access path | Enforcement point | Decision input | Granularity | Per-offering policy honoured? |
|---|---|---|---|---|
| Dataspace Protocol catalog / negotiation / transfer | the EDC connector's policy engine | participant identity + verified credentials, scope-tagged | per asset | **yes** (plane 2) |
| These APIs, through the DSC gateway | gateway plugin → policy engine | request method, path, body, token claims | resource type | **no** |
| These APIs, from a marketplace UI | *none* — the UI's backend calls the service directly | — | — | **no** |
| These APIs, through their own Ingress | *none* | — | — | **no** |
| The purchased data service | gateway plugin → policy engine | request + token claims | per policy installed at order activation | n/a (plane 1) |

## Who calls these APIs

This decides where enforcement can go. In the DSC deployment all of these resolve to one service —
the `all-in-one` application, or the envoy `apiProxy` in front of the per-API deployments:

| Caller | Identity it carries | Must be filtered? |
|---|---|---|
| External API consumer, through the gateway | the caller's access token, with the full verified credential inside it | **yes** |
| Marketplace UI backend (BAE) | holds the user's session; sends **no** token onward today | **yes** |
| `contract-management` | none | **no** — it activates orders and must read everything |
| The EDC connector | none | **no** — it needs the whole catalog to build per-consumer catalogs, and filters itself |
| `consent-facade` | none | **no** — it projects privacy notices from agreements |
| Anything else in the cluster, or anyone reaching the plain Ingress | none | the Ingress is the hole |

Three consequences follow, and they decide the architecture:

1. **Gateway-only enforcement is not an answer.** It leaves the marketplace path — the one most
   consumers use — completely unguarded.
2. **A proxy in front of the service is not an answer either**, because three of the six callers are
   infrastructure that must *not* be filtered. Enforcement has to distinguish callers, not merely
   requests.
3. **Enforcement therefore belongs where every caller necessarily arrives**: in this project. It is
   also the only place that can tell an infrastructure caller from a consumer without depending on
   how the request was routed.

## The gap, stated precisely

1. **No per-instance decision.** The policy engine is asked "may this subject read
   `productOffering`?", never "may this subject read *offering `urn:…:42`*?".

2. **No collection filtering.** `GET /productOffering` returns a list. A gateway that decides before
   the response exists can only permit or refuse the whole list.

3. **Pagination would be a casualty of filtering anywhere else.** This project computes
   `X-Result-Count`, `X-Total-Count`, the TMF630 `Link` header (`next`, `last`) and
   `206 Partial Content` from `PagedResult`
   ([`PaginationFilter`](../../common/src/main/java/org/fiware/tmforum/common/filter/PaginationFilter.java)).
   An external filter that drops items from the body leaves every one of those describing the
   *unfiltered* collection — wrong counts, links that page through hidden entities, and a status
   code that claims a subset of something the caller cannot see.

4. **No contracting check.** `POST /productOrder` references an offering by id. Nothing evaluates
   that offering's contract policy before the order is created; the first evaluation happens *after*
   the order completes, when plane 1 is installed.

5. **The two paths disagree.** The same offering with the same `accessPolicy` is hidden over the
   Dataspace Protocol and public here. An attacker needs no attack: they use the other door.

A worked instance, using the motivating case: a provider wants an offering restricted to
participants established in DE, AT or CH. The credential issued in the DSC reference deployment
already carries a `country` claim, and the verifier is configured to include the whole credential in
the access token. The attribute is in the token the gateway already validates — there is simply
nothing that compares it against a policy attached to one offering.

## What this project already has

The feature is mostly assembly, and the pieces are in place:

* **A storage port.** [`EntityReadRepository`](../../storage-api/src/main/java/org/fiware/tmforum/storage/EntityReadRepository.java)
  is the single seam every read goes through — `get`, `findEntities`, `findEntitiesPolymorphic`,
  all returning `Mono<PagedResult<T>>` or `Mono<T>`. Decorating it reaches every one of the ~44 list
  endpoints and every read-by-id without touching a controller.
* **One funnel for list endpoints.** [`AbstractApiController#list`](../../common/src/main/java/org/fiware/tmforum/common/rest/AbstractApiController.java)
  resolves pagination and query parameters, calls the port once, and stashes
  offset/limit/returned-count/total-count as request attributes. Correct pagination after filtering
  is a matter of handing it a correct `PagedResult`.
* **The policy arrives with the entity.** `productOfferingTerm.accessPolicy` is deserialised with
  the offering. No second fetch, no second client, no privileged credential in the request path.
* **A filter package and its conventions.** [`common/.../filter`](../../common/src/main/java/org/fiware/tmforum/common/filter)
  already holds `ForwardedForFilter`, which resolves per-request information in an
  `HttpServerFilter` and publishes it as a request attribute for later stages. Subject resolution is
  the same shape.
* **`ServerRequestContext` works inside the reactive chain here.** `AbstractApiController` and
  `FieldCleaningSerializer` already rely on it, so a decorator below the controller can reach the
  request without a new propagation mechanism.
* **Caching.** Micronaut `@Cacheable` with an in-memory or Redis backend is already wired
  (`micronaut.caches.*`, `redis.caches.*`), which is what makes a decision cache cheap.
* **A query parser.** `QueryParser` / `QueryParams` already translate request filters into NGSI-LD
  `q` syntax — the machinery any later predicate push-down would reuse.
* **Micrometer and an OTEL-friendly deployment**, so an access audit log has somewhere to go.

## Constraints that shape any solution

| # | Constraint | Consequence |
|---|---|---|
| C1 | This is a general TMForum implementation, not a Data Space Connector component. | The feature must be optional and inert when off, and must depend on an *endpoint contract* for the decision, never on a specific policy engine, identity stack or credential format. |
| C2 | Infrastructure callers must read unfiltered. | Enforcement has to see *who* is calling. A decorator that filters unconditionally breaks order activation and catalog building. |
| C3 | Nothing in this project validates a token today. | Subject resolution is new capability, and it cannot be delegated to the gateway, because the callers that most need enforcing do not pass through one. |
| C4 | Pagination is TMF630-complete and derived from `PagedResult`. | Whatever filters must produce a `PagedResult` that is correct for the caller, or `Link`, `X-Total-Count` and the `206` status all become lies. |
| C5 | Existing deployments have entities with no policy, and callers that send no token. | Default must reproduce today's behaviour, and the feature must be switchable on without requiring other components to change first. |
| C6 | The decision must mean the same thing here as on the Dataspace Protocol path. | Send the same policy to the same PDP with the same scope semantics; do not re-implement evaluation. |
| C7 | Hiding the existence of an entity is part of the requirement. | A denied read of a known id must be indistinguishable from a read of an id that does not exist. |
| C8 | A provider must keep managing its own restricted entities. | The policy governs *consumers*; the owning party is exempt from its own visibility rules. |
| C9 | Everything here is reactive (`Mono`/`Flux`). | The decision path must be non-blocking: a reactive PDP client, and no `block()` anywhere in a repository decorator. |
