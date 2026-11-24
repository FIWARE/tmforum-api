# Use cases

What the feature has to do, written so that each case can be read by someone deciding whether it
solves their problem, and by someone deciding whether the implementation is finished.

Each case names the integration test that proves it. Tests follow the project's conventions: an
`*IT.java` extending `AbstractApiIT`, with a stub Policy Decision Point, parameterized where the
cases differ only in data.

**Actors.** *Provider* publishes the catalog. *Consumer A* qualifies for the policies below
(`country: DE`); *Consumer B* does not (`country: SG`). "Qualifies" always means: the subject
resolved from the caller's token satisfies the policy's constraints.

| # | Use case | Scope | Phase |
|---|---|---|---|
| [UC-1](#uc-1--an-entity-restricted-by-a-subject-attribute) | Entity restricted by a subject attribute | `catalog` + `contract.negotiation` | 2–4 |
| [UC-2](#uc-2--visible-but-not-orderable) | Visible but not orderable | `contract.negotiation` | 4 |
| [UC-3](#uc-3--a-private-catalog-for-a-named-partner) | Private catalog for a named partner | `catalog`, derived | 3 |
| [UC-4](#uc-4--one-policy-one-answer-on-every-path) | One policy, one answer on every path | both | 3 |
| [UC-5](#uc-5--a-caller-acting-for-a-user) | A caller acting for a user | `catalog` | 3 |
| [UC-6](#uc-6--a-restriction-inherited-from-a-shared-service-specification) | Restriction inherited from a composed specification | `catalog` | 3 |
| [UC-7](#uc-7--a-provider-manages-its-own-restricted-entity) | The owner is not hidden from itself | all | 2 |
| [UC-8](#uc-8--an-entity-without-a-policy-stays-public) | No policy, no change | all | 2 |
| [UC-9](#uc-9--a-policy-change-takes-effect) | Policy change takes effect | `catalog` | 3 |
| [UC-10](#uc-10--callers-without-a-token-are-not-filtered) | Callers without a token are not filtered | all | 2 |
| [UC-11](#uc-11--a-denied-access-is-auditable) | Denied access is auditable | all | 5 |
| [UC-12](#uc-12--paging-a-filtered-collection) | Paging a filtered collection | `catalog` | 3 |

Phases refer to [ARCHITECTURE.md § Rollout](./ARCHITECTURE.md#rollout).

---

## UC-1 — An entity restricted by a subject attribute

*The case that motivates the feature.*

**Goal.** The provider publishes an offering that should be seen and ordered only by participants
established in DE, AT or CH — a legal constraint, because the service processes data that may not
leave the region.

**Preconditions.** `access-policy.enabled: true`. The offering's `productOfferingTerm` carries an
access policy and a contract policy constraining the subject's `country` to `{DE, AT, CH}`
([POLICY_EXAMPLES.md](./POLICY_EXAMPLES.md#an-entity-restricted-by-a-subject-attribute)). Both
consumers present a token the filter can validate, carrying their `country`.

**Main flow.**
1. Consumer A lists `GET /productOffering`. The decorator resolves the policy from the entity and
   asks the PDP at scope `catalog`. `DE` satisfies the constraint; the offering is in the response.
2. Consumer A creates a `ProductOrder` for it. The contract policy is evaluated at
   `contract.negotiation` and permits. The order is created.

**Alternative flow — a consumer who does not qualify.**
1. Consumer B lists the same collection. The offering is absent, and the page is *full*: asking for
   `limit=20` returns 20 visible offerings while any remain.
2. Consumer B, having learned the id elsewhere, calls `GET /productOffering/{id}`. The answer is
   **`404`**, byte-identical to the response for an id that does not exist.
3. Consumer B posts a `ProductOrder` referencing the id anyway. The answer is **`403`** naming the
   offering; nothing is persisted.

**Acceptance criteria.**
* The qualifying consumer's result is identical to the feature being off.
* The non-qualifying consumer can neither enumerate, read, nor order the offering.
* No credential other than the one actually presented influences the decision.
* The `404` body and headers are indistinguishable from a genuine miss.

**Integration test** — `AccessPolicyProductOfferingIT`:
`list` as A contains the id; `list` as B does not; `get` as B is `404` with the standard error body;
`POST /productOrder` as B is `403` and a subsequent `list` of product orders is empty.

---

## UC-2 — Visible but not orderable

**Goal.** The offering is discoverable — it is part of the public portfolio — but only subjects with
a particular credential may actually order it.

**Preconditions.** Access policy permits everyone; contract policy constrains on a membership claim.

**Main flow.**
1. The consumer lists offerings; the offering is present with its full description.
2. `GET /{id}` answers `200`.
3. `POST /productOrder` is refused with `403`, and the problem detail names the offering and the
   failing policy uid, so the consumer can work out which credential to obtain.

**Acceptance criteria.**
* Visibility and orderability are independently decidable; denying one does not deny the other.
* The decision happens **before** anything is written — no order in an intermediate state remains.

**Integration test** — `AccessPolicyOrderingIT`: `get` is `200`; `POST /productOrder` is `403`; the
product-order collection is unchanged.

---

## UC-3 — A private catalog for a named partner

**Goal.** A "Partner Preview" `Catalog` holding several offerings, visible to one named subject. The
provider wants to write the restriction **once**, on the catalog, not on every offering in it.

**Preconditions.** A policy targeting the catalog by id, registered at the PDP
([POLICY_EXAMPLES.md](./POLICY_EXAMPLES.md#a-catalog-restricted-to-a-named-partner)). The offerings
in it carry no policy of their own.

**Main flow.**
1. The partner lists catalogs; the preview catalog is present, and its offerings are listed.
2. Any other consumer lists catalogs; the preview catalog is absent — and **its offerings are absent
   too**, by downward derivation.
3. That consumer reads a preview offering by id: `404`.

**Acceptance criteria.**
* One policy on the catalog governs every offering in it.
* An offering's own policy, where it has one, is combined with the catalog's, never overridden.
* With `derive-catalog-visibility: true`, a catalog that becomes empty for a subject is itself
  hidden from them; with it `false`, it stays visible and empty.

**Open point.** TMF620 gives `Catalog` no characteristic array, so the policy has no carrier on the
entity. Matching a PDP-registered policy by target id avoids a schema change — this use case is the
test of whether that is sufficient in practice.

**Integration test** — `AccessPolicyCatalogIT`, parameterized over
`derive-catalog-visibility` true/false.

---

## UC-4 — One policy, one answer on every path

*The regression guard for the gap the feature exists to close.*

**Goal.** For one offering with one policy, this API and the Dataspace Protocol catalog served by
the surrounding deployment answer the same question the same way.

**Preconditions.** An offering with a restrictive `accessPolicy` in `productOfferingTerm`, and a
consumer who does not qualify.

**Main flow.** The asset is absent from the Dataspace Protocol catalog (already true today), and the
offering is absent from `GET /productOffering` and answers `404` by id (what this feature adds).

**Acceptance criteria.**
* The provider authored **one** policy, not two.
* Changing it changes both answers.
* A policy that genuinely needs to differ between paths says so with the `protocol` field, so the
  difference is visible in the policy text rather than hidden in deployment configuration.

**Integration test.** Only this project's half is testable here —
`AccessPolicyProductOfferingIT` asserts that a policy the Dataspace Protocol side would evaluate at
scope `catalog` is sent to the PDP with the same scope and the same policy body. The cross-component
half belongs in the
[Data Space Connector's integration tests](https://github.com/FIWARE/data-space-connector/tree/main/it).

---

## UC-5 — A caller acting for a user

**Goal.** A UI backend — a marketplace, a portal — calls these APIs on behalf of a logged-in user,
and the user's policies apply. This is the path most humans actually use.

**Preconditions.** The caller forwards the user's access token on its outbound calls. It reaches
this service directly, without any gateway; the filter enforces regardless of how the request
arrived.

**Main flow.**
1. A user of a qualifying consumer browses the catalogue; the offering is listed and orderable.
2. A user of a non-qualifying consumer browses the same catalogue; the offering is not listed, and a
   direct link to its detail page shows a not-found.
3. A stale cart entry reaching checkout is refused with `403`.

**Acceptance criteria.**
* The UI needs no change: it shows what the API returns, and needs no new route or proxy.
* A caller that forwards **no** token is anonymous. While `bypass-unauthenticated` is `true` it is
  unfiltered, and `observe` mode plus the audit log records what *would* have been hidden. This is
  the feature's one hard external dependency, and it is measured rather than assumed.

**Integration test** — `AccessPolicySubjectResolutionIT`: the same request with A's token, with B's
token, and with none, against the three settings of `bypass-unauthenticated` and `mode`.

---

## UC-6 — A restriction inherited from a shared service specification

**Goal.** The restriction lives where the restricted thing lives — on the `ServiceSpecification` —
and every product composed of it inherits it, with no policy authored on the products.

**Preconditions.** A `ServiceSpecification` carries an `authorizationPolicy` characteristic whose
permission declares `dspace:scope: "catalog"`. Two `ProductSpecification`s reference it; two
offerings reference those; a third offering is built from something else.

**Main flow.** A non-qualifying consumer lists offerings and sees only the third. Adding a fourth
product composed of the restricted service hides it immediately, with nothing authored on it.

**Acceptance criteria.**
* Resolution unions across levels, keys by `odrl:uid`, and is bounded by a depth limit.
* Two different policies claiming one `odrl:uid` is an error, reported as such, not silently
  resolved to one of them.
* A permission **without** a `dspace:scope` is ignored here and keeps whatever meaning it has
  elsewhere in the ecosystem.
* The composition walk reads through the **delegate**: a restricted specification does not make
  itself unresolvable to the resolver that is trying to read it.

**Integration test** — `AccessPolicyCompositionIT`, including the uid-collision and depth-limit
cases.

---

## UC-7 — A provider manages its own restricted entity

**Goal.** The seller who published a restricted offering keeps seeing and editing it. A naive
implementation hides the offering from its owner, who then cannot fix the policy that hides it.

**Preconditions.** `owner-exempt: true` (the default). The subject's identity matches a
`relatedParty` with role `provider` on the entity.

**Main flow.** The provider lists, reads, updates and retires its own offerings regardless of
policy — and does **not** see another provider's restricted offerings.

**Acceptance criteria.**
* The exemption is decided from `relatedParty` before any PDP call is made.
* `owner-exempt: false` is possible and then locks the owner out — documented as the behaviour, not
  filed as a bug.

**Integration test** — `AccessPolicyOwnerExemptionIT`, parameterized over the setting.

---

## UC-8 — An entity without a policy stays public

**Goal.** Enabling the feature changes nothing until a policy exists.

**Main flow.** With `default-decision: allow`, every consumer sees what they saw before, and **no
PDP call is made** for an entity no policy governs.

**Alternative flow.** With `default-decision: deny`, an entity is invisible until a policy permits
it: a consumer with no qualifying credential gets an empty collection, while the provider still sees
everything through the owner exemption.

**Acceptance criteria.**
* With `access-policy.enabled` unset, the decorator bean does not exist at all — assert on the
  application context, not only on behaviour.
* With it enabled and no policies present, the stub PDP receives zero requests.

**Integration test** — `AccessPolicyDefaultsIT`, parameterized over `default-decision`, plus a
context assertion that no `access-policy` bean is present when the feature is off.

---

## UC-9 — A policy change takes effect

**Goal.** Widening or narrowing a policy on a published entity takes effect without a restart.

**Main flow.** An offering restricted to `DE` is invisible to an `AT` consumer; the provider widens
the policy to `{DE, AT, CH}`; within the decision TTL the `AT` consumer sees it; narrowing it back
hides it again on the same terms.

**Acceptance criteria.**
* The staleness window is bounded by `decision-ttl` and documented.
* Narrowing is as prompt as widening — no cache path makes a revocation slower than a grant.
* Entities already ordered are unaffected: hiding an offering does not revoke what was bought.

**Integration test** — `AccessPolicyCacheIT` with a short `decision-ttl`.

---

## UC-10 — Callers without a token are not filtered

**Goal.** The components that make the surrounding deployment work — order activation, catalog
building, consent projection — read unfiltered. None of them sends a token, so the first iteration
exempts callers that present no identity, and that exemption has to be understood for what it is.

**Preconditions.** `bypass-unauthenticated: true`.

**Main flow.**
1. A request with no `Authorization` header is not filtered and sees restricted entities.
2. A request with a token is filtered normally.
3. With `bypass-unauthenticated: false` and no bypass role, the same unauthenticated request is
   decided by `anonymous-decision`.

**Acceptance criteria.**
* The bypass is explicit in configuration and in the audit record; a bypassed request is logged as
  bypassed, not as permitted by policy.
* The documentation states the deployment obligation plainly: with the bypass on, the API must not
  be reachable unauthenticated, or dropping a header defeats the feature.

**Integration test** — `AccessPolicyBypassIT`, parameterized over
`bypass-unauthenticated` × `anonymous-decision`.

---

## UC-11 — A denied access is auditable

**Goal.** Every decision is explainable afterwards, and `observe` mode makes a policy's impact
measurable before it is enforced.

**Main flow.** A consumer reports a missing offering; the operator finds one audit record per denied
entity carrying the decision, the policy uid, the resource id, the subject and a reason. Before
narrowing a policy, the operator runs in `observe` mode and counts what *would* be denied.

**Acceptance criteria.**
* One record per decision, emitted asynchronously, never changing the decision or the response.
* `observe` produces identical records to `enforce` and filters nothing.
* The reason distinguishes "denied by policy X" from "denied because the PDP was unreachable".

**Integration test** — `AccessPolicyAuditIT`, asserting record content and that `observe` leaves the
body untouched.

---

## UC-12 — Paging a filtered collection

*The property the whole design exists to preserve.*

**Goal.** Filtering is invisible in the paging contract. A client pages a filtered collection exactly
as it pages an unfiltered one.

**Preconditions.** 100 offerings, 40 of them restricted from this consumer. `total-count: exact`,
and a broker profile where `general.countHeader` is configured so a total exists at all.

**Main flow.**
1. `GET …/productOffering?limit=20&offset=0` returns **20** offerings, all permitted.
2. `X-Result-Count: 20`. `X-Total-Count: 60` — the collection *as this consumer sees it*.
3. The `Link` header's `next` and `last` point into the 60, not the 100. The status is
   `206 Partial Content`.
4. `offset=20`, `offset=40` return the next twenty and the last twenty. Pages neither overlap nor
   skip. `offset=60` returns an empty array and `200`.
5. The provider, through the owner exemption, pages the same collection and sees 100 in five pages.

**Alternative flows.**
* **Past the scan bound.** The match set exceeds `max-scan`: `X-Total-Count` and `last` are omitted
  rather than reported unverified, the status stays `200`, and `next` falls back to the
  `returnedCount == limit` heuristic — which is correct precisely because pages are refilled. Paging
  itself is unaffected.
* **`total-count: upstream`.** The unfiltered total is passed through. This leaks the size of the
  hidden set and produces links into it; the test asserts that it does, so nobody enables it by
  accident.
* **No broker total.** On a profile where the broker reports none, the result stays `null` as it does
  today, and only the refill behaviour is observable.

**Acceptance criteria.**
* Every full page holds exactly `limit` items while items remain.
* `offset` indexes the *filtered* sequence, so consecutive pages are disjoint and complete.
* `X-Total-Count`, when sent, counts only what the caller may see.
* `Link` relations and the `200`/`206` status are consistent with the filtered total.
* Sorting is preserved: a filtered page of a sorted collection is still correctly sorted.

**Integration test** — `AccessPolicyPaginationIT`, parameterized over `total-count` and over both
broker profiles. It asserts headers and status, not only the body — a test that checks the array and
not the `Link` header would pass while the feature is broken.
