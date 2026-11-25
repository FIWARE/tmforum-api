# Policy examples and vocabulary

Concrete ODRL for the use cases in [USE_CASES.md](./USE_CASES.md), plus the vocabulary the Policy
Decision Point has to understand. None of this is implemented in this project — it is what a
provider authors and what the PDP evaluates; this project resolves the policy from the entity and
asks. Everything here follows the conventions already used in
[`it/src/test/resources/policies/`](https://github.com/FIWARE/data-space-connector/tree/main/it/src/test/resources/policies) and
[CENTRAL_MARKETPLACE.md](https://github.com/FIWARE/data-space-connector/blob/main/doc/CENTRAL_MARKETPLACE.md).

- [Vocabulary](#vocabulary)
- [An entity restricted by a subject attribute](#an-entity-restricted-by-a-subject-attribute)
- [Visible but not orderable](#visible-but-not-orderable)
- [A catalog restricted to a named partner](#a-catalog-restricted-to-a-named-partner)
- [A visibility policy on a service specification](#a-visibility-policy-on-a-service-specification)
- [Scoping: which permission is evaluated where](#scoping-which-permission-is-evaluated-where)
- [What the PDP receives](#what-the-pdp-receives)

## Vocabulary

| Operand | Meaning | Status |
|---|---|---|
| `tmf:resource` | the resource **type** (`productOffering`, `catalog`, …) | exists in the reference PDP |
| `tmf:resourceId` | the instance id | **new** |
| `tmf:relatedParty` | a DID appearing in the entity's `relatedParty`, optionally by role | **new** |
| `tmf:catalog`, `tmf:category` | the containing catalog / category id | **new** |
| `vc:type`, `vc:role` | credential type, role claim | exist in the reference PDP |
| `vc:<claim>` | any claim of a presented, verified credential — `vc:country`, `vc:legalName`, … | **new** (generic form) |
| `odrl:spatial` | the standard ODRL spatial operand, mapped onto the subject's established country | **new**, optional sugar over `vc:country` |
| `dspace:membershipType` | membership asserted by a membership credential | exists on the DSP path |
| `dspace:scope` | the evaluation point a permission applies to; **the discriminator this project reads** | exists on the DSP path |

Operators in use: `odrl:eq`, `odrl:isAnyOf`, `odrl:hasPart`, `odrl:lt`, `http:isInPath`.

## An entity restricted by a subject attribute

[UC-1](./USE_CASES.md#uc-1--an-entity-restricted-by-a-subject-attribute). Written
on the `ProductOffering` as the DSP access/contract pair, so one authoring step covers both paths.
The block is one entry of the offering's `productOfferingTerm` array.

```json
{
  "name": "edc:contractDefinition",
  "@schemaLocation": "https://raw.githubusercontent.com/wistefan/edc-dsc/refs/heads/init/schemas/contract-definition.json",
  "accessPolicy": {
    "@context": [
      "http://www.w3.org/ns/odrl.jsonld",
      { "vc": "https://w3id.org/fiware/dsc/vc#", "dspace": "https://w3id.org/dspace/2024/1/" }
    ],
    "@type": "Offer",
    "odrl:uid": "https://mp-operation.org/policy/offering/k8s-eu/access",
    "assigner": "did:web:mp-operations.org",
    "permission": [
      {
        "action": "read",
        "dspace:scope": "catalog",
        "constraint": {
          "leftOperand": "vc:country",
          "operator": "odrl:isAnyOf",
          "rightOperand": [
            { "@value": "DE", "@type": "xsd:string" },
            { "@value": "AT", "@type": "xsd:string" },
            { "@value": "CH", "@type": "xsd:string" }
          ]
        }
      }
    ]
  },
  "contractPolicy": {
    "@context": [
      "http://www.w3.org/ns/odrl.jsonld",
      { "vc": "https://w3id.org/fiware/dsc/vc#", "dspace": "https://w3id.org/dspace/2024/1/" }
    ],
    "@type": "Offer",
    "odrl:uid": "https://mp-operation.org/policy/offering/k8s-eu/contract",
    "assigner": "did:web:mp-operations.org",
    "permission": [
      {
        "action": "use",
        "dspace:scope": "contract.negotiation",
        "constraint": {
          "leftOperand": "vc:country",
          "operator": "odrl:isAnyOf",
          "rightOperand": [
            { "@value": "DE", "@type": "xsd:string" },
            { "@value": "AT", "@type": "xsd:string" },
            { "@value": "CH", "@type": "xsd:string" }
          ]
        }
      }
    ]
  }
}
```

**The `@schemaLocation` is mandatory.** `ProductOfferingTerm` has no policy fields, so
`accessPolicy` and `contractPolicy` are extensions. Without a `@schemaLocation` whose schema admits
them, the API rejects the term with `400` ("No additional properties are allowed without a
@schemaLocation"), and the body never names the offending key. The schema above is the one the DSC's
[DSP integration guide](https://github.com/FIWARE/data-space-connector/blob/main/doc/DSP_INTEGRATION.md#prepare-the-offering)
uses. A deployment may host its own schema instead, as long as it admits both keys as objects. With
a schema that declares both keys as `{"type":"object"}`, the pair round-trips unchanged on every
storage ([AP-01 finding](./AP-01_FINDINGS.md)). No IT runs with the DSC schema itself.

The DSC `contract-definition.json` lists `accessPolicy` and `contractPolicy` as `required`. On
write the API validates only the term's extension keys against the schema, and only when there is at
least one. A term that points at it with exactly one of the two keys gets `400`; a term with neither
is accepted. The same applies to the term carrying the UC-2 contract policy below.

Two things worth stating plainly:

* **Restate the constraint on the contract policy.** Access and contract are separate decisions;
  a consumer who learned the offering id elsewhere is only stopped from ordering by the contract
  policy. Omitting it makes the restriction a visibility nicety, not a control.
* **`vc:country` reads a claim of the presented credential**, which an issuer asserted and the
  deployment's verifier checked. Reading the same value from a TMForum party record instead would be
  a weaker statement — the participant wrote it themselves — and is not part of this design.

The same constraint using the standard spatial operand, if the PDP implements the sugar:

```json
{ "action": "read", "dspace:scope": "catalog",
  "constraint": { "leftOperand": "odrl:spatial", "operator": "odrl:isAnyOf",
                  "rightOperand": [ "https://publications.europa.eu/resource/authority/country/DEU",
                                    "https://publications.europa.eu/resource/authority/country/AUT",
                                    "https://publications.europa.eu/resource/authority/country/CHE" ] } }
```

## Visible but not orderable

[UC-2](./USE_CASES.md#uc-2--visible-but-not-orderable). No access policy at all (or one that
permits everyone), and a contract policy that constrains membership. The term needs a
`@schemaLocation` like the UC-1 term. Under the DSC `contract-definition.json`, which requires both
keys, "no access policy" is not accepted: write a permit-all `accessPolicy`, or point the term at a
schema that does not require it.

```json
"contractPolicy": {
  "@context": [ "http://www.w3.org/ns/odrl.jsonld", "https://w3id.org/dspace/2024/1/context.json" ],
  "@type": "Offer",
  "odrl:uid": "https://mp-operation.org/policy/offering/browse-only/contract",
  "assigner": "did:web:mp-operations.org",
  "permission": [
    { "action": "use",
      "dspace:scope": "contract.negotiation",
      "constraint": { "leftOperand": "dspace:membershipType",
                      "operator": "eq",
                      "rightOperand": "FullMember" } }
  ]
}
```

This is byte-for-byte the shape the DSP path already uses
([the DSC's DSP integration guide](https://github.com/FIWARE/data-space-connector/blob/main/doc/DSP_INTEGRATION.md#prepare-the-offering)) — which is the point.

## A catalog restricted to a named partner

[UC-3](./USE_CASES.md#uc-3--a-private-catalog-for-a-named-partner). Assignee-based rather than
constraint-based, because the audience is enumerated rather than described:

```json
{
  "@context": { "odrl": "http://www.w3.org/ns/odrl/2/", "dspace": "https://w3id.org/dspace/2024/1/" },
  "@id": "https://mp-operation.org/policy/catalog/partner-preview/access",
  "odrl:uid": "https://mp-operation.org/policy/catalog/partner-preview/access",
  "@type": "odrl:Policy",
  "odrl:permission": {
    "odrl:assigner": { "@id": "did:web:mp-operations.org" },
    "odrl:assignee": { "@id": "did:web:fancy-marketplace.biz" },
    "odrl:target": {
      "@type": "odrl:AssetCollection",
      "odrl:source": "urn:tmf",
      "odrl:refinement": [
        { "@type": "odrl:Constraint",
          "odrl:leftOperand": "tmf:resource",
          "odrl:operator": { "@id": "odrl:eq" },
          "odrl:rightOperand": "catalog" },
        { "@type": "odrl:Constraint",
          "odrl:leftOperand": "tmf:resourceId",
          "odrl:operator": { "@id": "odrl:eq" },
          "odrl:rightOperand": "urn:ngsi-ld:Catalog:partner-preview" }
      ]
    },
    "dspace:scope": "catalog",
    "odrl:action": { "@id": "odrl:read" }
  }
}
```

A policy written this way can equally be installed **directly at the PAP** rather than attached to
the entity — it names its target by id. That is the fallback if no carrier for catalog-level
characteristics is agreed
([ARCHITECTURE.md § Open questions](./ARCHITECTURE.md#open-questions)): the provider writes it once
at the PAP, and the resolver picks it up by target match instead of by attachment.

## A visibility policy on a service specification

[UC-6](./USE_CASES.md#uc-6--a-restriction-inherited-from-a-shared-service-specification). Note the
container names — `specCharacteristic` / `characteristicValueSpecification` on a
`ServiceSpecification`, not the product's `productSpecCharacteristic` /
`productSpecCharacteristicValue`. Getting this wrong is the most common authoring mistake
([COMPOSED_SPECIFICATIONS.md](https://github.com/FIWARE/data-space-connector/blob/main/doc/deployment-integration/roles/provider/COMPOSED_SPECIFICATIONS.md#the-container-names-differ)).

```json
{
  "name": "M&P K8S Cluster API (EU only)",
  "version": "1.0.0",
  "lifecycleStatus": "ACTIVE",
  "specCharacteristic": [
    {
      "id": "visibility",
      "name": "Who may see products built from this API",
      "valueType": "authorizationPolicy",
      "characteristicValueSpecification": [
        {
          "isDefault": true,
          "value": {
            "@context": { "odrl": "http://www.w3.org/ns/odrl/2/",
                          "vc": "https://w3id.org/fiware/dsc/vc#",
                          "dspace": "https://w3id.org/dspace/2024/1/" },
            "@id": "https://mp-operation.org/policy/service/k8s-eu/visibility",
            "odrl:uid": "https://mp-operation.org/policy/service/k8s-eu/visibility",
            "@type": "odrl:Policy",
            "odrl:permission": {
              "odrl:assigner": { "@id": "did:web:mp-operations.org" },
              "dspace:scope": "catalog",
              "odrl:action": { "@id": "odrl:read" },
              "odrl:constraint": {
                "@type": "odrl:Constraint",
                "odrl:leftOperand": "vc:country",
                "odrl:operator": { "@id": "odrl:isAnyOf" },
                "odrl:rightOperand": [ { "@value": "DE", "@type": "xsd:string" },
                                       { "@value": "AT", "@type": "xsd:string" },
                                       { "@value": "CH", "@type": "xsd:string" } ]
              }
            }
          }
        }
      ]
    }
  ]
}
```

Give every policy its own `odrl:uid`. Two different policies claiming one uid fail the resolution
([UC-6](./USE_CASES.md#uc-6--a-restriction-inherited-from-a-shared-service-specification)), exactly
as they fail order activation in the surrounding deployment today.

## Scoping: which permission is evaluated where

`dspace:scope` is the discriminator that lets one `authorizationPolicy` characteristic list carry
both kinds of rule without ambiguity:

| `dspace:scope` | Read by | Governs |
|---|---|---|
| `catalog` | **this project**, and the EDC connector for the DSP catalog | whether the resource is visible |
| `contract.negotiation` | **this project**, and the EDC connector for DSP negotiation | whether it can be ordered / negotiated |
| `transfer.process` | the EDC connector only | whether a DSP transfer proceeds |
| *(absent)* | the surrounding deployment, at order activation | the purchased data service — ignored by this project |

A permission with no scope therefore keeps exactly the meaning it has today, and this project
ignores it. That is what makes the feature additive rather than a migration.

## What the PDP receives

For reference when writing or debugging policy rules — this project sends the same request shape the
Dataspace Protocol side sends
([the DSC's DSP integration guide](https://github.com/FIWARE/data-space-connector/blob/main/doc/DSP_INTEGRATION.md#policy-context-what-the-pap-receives)),
with two additions. See [ARCHITECTURE.md § The PDP contract](./ARCHITECTURE.md#the-pdp-contract):

```json
{
  "policy": { "…the resolved policy, expanded JSON-LD…" },
  "jsonInput": {
    "payload": {
      "scope": "catalog",
      "protocol": "tmf",
      "action": "read",
      "resource": {
        "type": "ProductOffering",
        "id": "urn:ngsi-ld:ProductOffering:42",
        "catalog": "urn:ngsi-ld:Catalog:public",
        "category": [ "urn:ngsi-ld:Category:compute" ],
        "relatedParty": [ { "role": "provider", "did": "did:web:mp-operations.org" } ]
      }
    },
    "subject": {
      "identity": "did:web:fancy-marketplace.biz",
      "claims": { "vc": [ { "…the verified credential, as presented…" } ] }
    }
  }
}
```

`protocol` is `"tmf"` from this project and `"dsp"` from the EDC connector. A policy that ignores it — which is almost
every policy — is enforced identically on both paths. A policy that reads it can deliberately
differ, and the difference is then visible in the policy text rather than hidden in deployment
configuration.
