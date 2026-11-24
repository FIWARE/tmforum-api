# Decision records

Short records of the decisions behind the design: context, decision, consequences, alternatives.

| # | Decision | Status |
|---|---|---|
| [AP-1](#ap-1--delegate-evaluation-never-implement-a-policy-engine-here) | Delegate evaluation; never implement a policy engine here | Proposed |
| [AP-2](#ap-2--read-the-policies-the-entities-already-carry) | Read the policies the entities already carry | Proposed |
| [AP-3](#ap-3--enforce-at-the-storage-port) | Enforce at the storage port | Proposed |
| [AP-3a](#ap-3a--rejected-enforce-in-a-gateway-plugin) | *Enforce in a gateway plugin* | **Rejected** |
| [AP-4](#ap-4--validate-the-token-here-and-require-callers-to-forward-it) | Validate the token here; require callers to forward it | Proposed |
| [AP-5](#ap-5--a-denied-read-answers-404-a-denied-order-answers-403) | A denied read answers 404, a denied order 403 | Proposed |
| [AP-6](#ap-6--optional-default-allow-and-bypass-unauthenticated-callers-at-first) | Optional, default allow, bypass unauthenticated callers at first | Proposed |

---

## AP-1 — Delegate evaluation; never implement a policy engine here

**Context.** The feature needs ODRL policies evaluated against a subject's credentials. ODRL is a
large specification, credential formats vary, and the surrounding ecosystem already runs a policy
engine for other purposes.

**Decision.** This project resolves *which* policies govern an entity and *who* is asking, and asks
an external Policy Decision Point over a small HTTP contract
([ARCHITECTURE.md § The PDP contract](./ARCHITECTURE.md#the-pdp-contract)). It never parses a
constraint, never interprets an operator, and never learns a credential format.

**Consequences.**
* The project stays a TMForum implementation. The dependency is an endpoint contract, not a product:
  the reference PDP is the [ODRL-PAP](https://github.com/SEAMWARE/odrl-pap), and anything answering
  the same contract works.
* A policy constrains on whatever the PDP understands. Extending the vocabulary is the PDP's work,
  not a release here.
* One evaluation engine sees this path and the Dataspace Protocol path, so the same policy cannot
  mean two things depending on which door a consumer used.
* Cost: a network call on the read path. Mitigated by deduplicating per `(policy uid, subject)` and
  caching — see [ARCHITECTURE.md § Pagination](./ARCHITECTURE.md#pagination).
* Cost: the feature is unavailable without a PDP. Correct: without one there is nothing to enforce.

**Alternatives.**
* *Evaluate ODRL in-process.* Rejected: it makes a general TMForum implementation own a policy
  engine, and guarantees it drifts from the one the rest of the ecosystem uses.
* *A pluggable `AccessDecision` SPI with an in-process default.* Still available later; starting
  with one remote implementation avoids designing an abstraction before there is a second case.

---

## AP-2 — Read the policies the entities already carry

**Context.** `ProductOffering.productOfferingTerm` already holds an access policy and a contract
policy, written by providers and already honoured by the Dataspace Protocol side of the surrounding
deployment. Specifications already carry `authorizationPolicy` characteristics. The alternative is a
new, TMForum-specific policy field.

**Decision.** Evaluate the existing fields, using the ecosystem's existing scope names (`catalog`,
`contract.negotiation`) and `dspace:scope` as the discriminator for which permission this project
reads. Add no new schema.

**Consequences.**
* A provider authors one policy and gets both paths. There is no way to write one that is honoured
  over the Dataspace Protocol and forgotten here.
* A permission carrying **no** `dspace:scope` keeps the meaning it has elsewhere — a data-plane
  policy applied after an order completes — and is ignored here. The feature is additive, not a
  migration.
* Catalog and category policies have no carrier in TMF620. Rather than extend the model, the
  resolver also matches policies registered at the PDP whose target names the entity by id.
  [UC-3](./USE_CASES.md#uc-3--a-private-catalog-for-a-named-partner) tests whether that suffices.
* Cost: this project inherits vocabulary from a specific ecosystem (`dspace:scope`). It is a
  namespace in data the project already stores, not a code dependency.

**Alternatives.**
* *A TMForum-specific policy field.* Rejected: it guarantees the two paths drift and asks providers
  to say the same thing twice.
* *Reuse `authorizationPolicy` with no scope discriminator.* Rejected: an existing data-plane policy
  would silently become a visibility rule on upgrade.

---

## AP-3 — Enforce at the storage port

**Context.** Three seams exist: an `HttpServerFilter` over the serialised response, the controllers,
or the [`EntityReadRepository`](../../storage-api/src/main/java/org/fiware/tmforum/storage/EntityReadRepository.java)
storage port.

**Decision.** A `@Primary` decorator of `EntityRepository`, gated by `@Requires`.

**Consequences.**
* **Pagination stays correct.**
  [`PaginationFilter`](../../common/src/main/java/org/fiware/tmforum/common/filter/PaginationFilter.java)
  derives `X-Total-Count`, the `next` and `last` link relations and the `200`/`206` status from the
  total that `AbstractApiController#list` stashed, which is the broker's count of the *unfiltered*
  set. A decorator returns a `PagedResult` that is already the caller's view, so every one of those
  follows without modification. A response filter would leave them describing a collection the
  caller cannot see, and would have to reach back and rewrite state an earlier stage computed.
* **One seam covers everything.** `get`, `findEntities`, `findEntitiesPolymorphic`, and reads from
  `ReferenceValidationService` — all of them. No controller changes for reads.
* **Page refill is possible at all.** The decorator can ask the delegate for more entities to fill a
  page; a filter over a finished response cannot.
* Cost: the decorator needs the request to find the subject. `ServerRequestContext.currentRequest()`
  is how, and the codebase already relies on it inside the reactive chain in `AbstractApiController`
  and `FieldCleaningSerializer`, so this introduces no new mechanism.
* Cost: the delegate must be injected by concrete type, and the composition walk must go through the
  delegate rather than the decorator — otherwise the filter censors its own inputs and a restricted
  specification makes its own products unresolvable.
* Cost: the write path is not covered. Ordering and quoting need a check in those two modules'
  create paths. That asymmetry is real and is called out in the architecture.

**Alternatives.**
* *The controllers.* Rejected: 44 list endpoints funnel through `AbstractApiController#list`, but
  `get`, `listPolymorphic` and reference validation do not, so enforcement would cover some reads.
* *A response filter.* See [AP-3a](#ap-3a--rejected-enforce-in-a-gateway-plugin); the same
  pagination objection applies to an in-process response filter, only without the routing problems.

---

## AP-3a — *(Rejected)* Enforce in a gateway plugin

**Status: rejected.** Recorded because it is the obvious first answer, and the surrounding
deployment already runs a gateway plugin that does something similar for consent.

**What it proposed.** An external plugin on the API gateway, running in the request and response
phases, filtering the serialised response, with a companion service to resolve the policies and a
NetworkPolicy to stop anything reaching the API directly.

**Why it was attractive.** No change to this project at all; an existing, proven pattern in that
deployment; infrastructure callers exempted simply by not routing them through the plugin.

**Why it was rejected.**
* **It cannot page.** `limit` degrades to "up to limit", `offset` stops indexing anything stable,
  and `X-Total-Count`, `Link` and the `206` status all describe the unfiltered collection. The
  mitigation was an HTTP over-fetch loop — cost paid per request to approximate a property the API
  has for free.
* **It needs a second component** whose only job is to re-fetch, over HTTP and with privileged
  credentials, the policies the API had already deserialised.
* **Correctness depends on routing.** Callers not routed through the plugin are silently unenforced,
  and the guarantee is a deployment convention rather than a property of the code. The marketplace
  backend, which is the path most consumers use, calls the service directly.
* It still required callers to forward the user's token, so it avoided none of the hard dependency —
  it only added a second gateway route on top of it.

The consent plugin in that deployment remains correctly placed: the fact it enforces on — who owns
the returned data — genuinely does not exist until the response does. An access policy is a field of
the entity. Different problem, different seam.

---

## AP-4 — Validate the token here, and require callers to forward it

**Context.** This project validates nothing today; the gateway does. But the callers that most need
enforcing — a marketplace UI backend above all — reach the service directly, without a gateway.

**Decision.** The filter validates the bearer token itself against a configured JWKS endpoint, rather
than trusting a header set by an upstream proxy. Callers acting on a user's behalf must forward that
user's token.

**Consequences.**
* Enforcement is a property of this code, not of how a request was routed to it.
* Validation happens twice for requests that did pass through a gateway. Deliberate: the redundancy
  is what removes the routing dependency.
* Where the deployment's verifier embeds the whole verified credential in the token, every claim a
  policy might constrain on is present with no further lookup.
* Cost: a new configuration surface and a new failure mode, both standard.
* Cost: **the feature cannot enforce on a caller that forwards no identity.** The honest response is
  `observe` mode and an audit record, not a pretence of enforcement.
* Cost: a dependency for JWT validation. Which one is an
  [open question](./ARCHITECTURE.md#open-questions).

**Alternatives.**
* *Trust a pre-validated header from the gateway.* Rejected: it enforces on exactly the path that is
  already enforced and skips the one that is not.
* *Require every caller through the gateway.* Rejected: it makes infrastructure callers take a
  network detour to be told they are exempt, and reintroduces the routing dependency.

---

## AP-5 — A denied read answers 404, a denied order answers 403

**Context.** Hiding the *existence* of an entity is part of the requirement — a competitor should
not confirm a restricted product exists by probing ids. But a caller refused an order needs to know
why.

**Decision.** A read denied at scope `catalog` answers `404` with the standard error body. An order
denied at `contract.negotiation` answers `403`, naming the offering and the failing policy uid.

**Consequences.**
* Probing ids yields nothing, and the answer matches the Dataspace Protocol side, where a filtered
  asset is simply absent from the catalog.
* A caller refused an order has already seen the offering, so there is nothing left to conceal and a
  `404` would be misleading. They get an actionable error.
* Cost: an operator debugging a `404` needs the audit log, because the response deliberately does not
  say. That is what [UC-11](./USE_CASES.md#uc-11--a-denied-access-is-auditable) is for.

**Alternatives.**
* *`403` everywhere.* Simpler and leaks the existence of every restricted entity.
* *`404` everywhere.* Makes a contract-policy denial indistinguishable from a typo.

---

## AP-6 — Optional, default allow, and bypass unauthenticated callers at first

**Context.** This is a general TMForum implementation; most deployments will never use the feature.
Those that do have existing entities with no policies and infrastructure callers that send no token.

**Decision.** Three independently switchable layers:

* `access-policy.enabled: false` by default, with every bean `@Requires`-gated — off means the beans
  do not exist, not that they are configured to do nothing.
* `default-decision: allow` — an entity no policy governs behaves as it does today, and a deployment
  with no policies makes zero PDP calls.
* `bypass-unauthenticated: true` initially — a request with no token is not filtered, so the
  infrastructure callers keep working with no change in any of them. The target state is
  `bypass-roles` / `bypass-subjects`, exempting by identity rather than by its absence.

**Consequences.**
* The feature can be switched on without first changing four other components, which is how features
  ship.
* Every rollout step is independently verifiable.
* The weakest moment is explicit: while `bypass-unauthenticated` is `true`, dropping the
  `Authorization` header defeats enforcement. The deployment must therefore not publish the API
  unauthenticated. This project can document that obligation and name the setting for what it does;
  it cannot enforce it.
* A deployment wanting a closed catalog sets `default-decision: deny` deliberately, knowing it hides
  everything until policies exist.

**Alternatives.**
* *Deny by default.* Safer in the abstract, wrong for every existing deployment.
* *Require infrastructure tokens from day one.* Rejected as a starting point, retained as the
  destination.
