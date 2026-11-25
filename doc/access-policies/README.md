# Access policies on the TMForum APIs

Design documentation for an **optional** filter in `tmforum-api` that decides, per entity, whether
the calling subject may see or order it — from ODRL policies the entities already carry, with the
decision delegated to an external Policy Decision Point.

> :construction: **Status: proposal.** Nothing described here is implemented. This folder is the
> analysis, the proposed architecture and the use cases the implementation has to satisfy.

## Where this comes from

The requirement originates in the [FIWARE Data Space Connector](https://github.com/FIWARE/data-space-connector),
which bundles this project with an identity stack (VCVerifier, Trusted Issuers List) and an ODRL
Policy Administration Point. In that setting a provider publishes a `ProductOffering` and attaches
two ODRL policies to it — an **access policy** (who may see it) and a **contract policy** (who may
contract it). Today those policies are read by exactly one component, the EDC-based connector that
serves the **Dataspace Protocol** catalog. A consumer that instead calls these TMForum APIs, or uses
a marketplace UI built on them, sees and can order everything.

The policies are already in the entities this project serves: `ProductOffering.productOfferingTerm`
carries them. Nothing reads them here. That is the gap.

A worked example of what a provider wants and cannot currently express: *"this offering is visible
and orderable only to participants established in DE, AT or CH."*

## Why it lands in this project

The short version — the long one is [DECISIONS.md AP-2](./DECISIONS.md#ap-3--enforce-at-the-storage-port):

* **The policy is a field of the entity.** The component that just loaded the offering already holds
  its policy. Anything outside has to fetch it again, over HTTP, with privileged credentials.
* **Paging.** This project implements TMF630 pagination properly — `X-Result-Count`,
  `X-Total-Count`, `Link` with `self`/`first`/`prev`/`next`/`last`, and `206 Partial Content`, all
  derived from `PagedResult` in [`AbstractApiController#list`](../../common/src/main/java/org/fiware/tmforum/common/rest/AbstractApiController.java)
  and [`PaginationFilter`](../../common/src/main/java/org/fiware/tmforum/common/filter/PaginationFilter.java).
  A filter that removes items from a serialised response leaves every one of those describing a
  collection the caller cannot see. Filtering at the storage port keeps them honest.
* **Every caller arrives here.** Enforcement in a gateway covers the callers that happen to be
  routed through it; enforcement in the API covers all of them.

## Contents

| Document | What it covers |
|---|---|
| [ANALYSIS.md](./ANALYSIS.md) | What governs access to TMForum resources today, who calls these APIs, and the precise gap |
| [ARCHITECTURE.md](./ARCHITECTURE.md) | The design against this codebase: the storage-port decorator, subject resolution, the PDP client, pagination, configuration, rollout |
| [USE_CASES.md](./USE_CASES.md) | UC-1 … UC-12, with actors, preconditions, flow and acceptance criteria |
| [POLICY_EXAMPLES.md](./POLICY_EXAMPLES.md) | The ODRL a provider writes, and the vocabulary the PDP has to understand |
| [DECISIONS.md](./DECISIONS.md) | The decisions behind the design, including the gateway-plugin alternative and why it was dropped |
| [AP-01_FINDINGS.md](./AP-01_FINDINGS.md) | Whether a policy survives the entity round-trip on each carrier and storage, and the accessor paths to read it |
| [TICKETS.md](./TICKETS.md) | The implementation broken into tickets, their order, and the four points where the design does not yet hold against the code |

## Reading order

[ANALYSIS.md](./ANALYSIS.md) for why the feature is needed. [USE_CASES.md](./USE_CASES.md) for what
it does. [ARCHITECTURE.md](./ARCHITECTURE.md) if you are going to build it.
[TICKETS.md](./TICKETS.md) if you are going to plan it.

## The shape of it, in one paragraph

A new optional module provides three things: an `HttpServerFilter` that resolves the calling subject
from a bearer token, a decorator around the
[`EntityReadRepository`](../../storage-api/src/main/java/org/fiware/tmforum/storage/EntityReadRepository.java)
storage port that filters what the port returns, and a client for the Policy Decision Point. With
`access-policy.enabled` unset, `@Requires` keeps every one of those beans out of the context and the
APIs behave exactly as they do today. With it on, a `GET` of a restricted entity answers `404`, a
list omits it and still returns a full page with correct counts, and a `ProductOrder` referencing it
is refused with `403` before anything is written.

## Prerequisites outside this project

* A **Policy Decision Point** that evaluates ODRL. The reference implementation is the
  [ODRL-PAP](https://github.com/SEAMWARE/odrl-pap) and its `/validate` endpoint; the project depends
  on the endpoint contract, not on that implementation.
* **Callers must forward the end user's token.** A caller acting on a user's behalf that sends no
  token is, to this filter, anonymous. See
  [ARCHITECTURE.md § Identity](./ARCHITECTURE.md#identity-who-is-calling).
