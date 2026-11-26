package org.fiware.tmforum.accesspolicy;

import com.nimbusds.jose.util.JSONObjectUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.text.ParseException;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the detector that decides whether a cached decision may be shared across resources; a wrong {@code false}
 * would leak one resource's verdict to another.
 */
class ResourceConstraintDetectorTest {

	private static final String TMF_NAMESPACE = "https://www.tmforum.org/ns/odrl#";
	private static final String VC_NAMESPACE = ResourceConstraintDetector.VC_NAMESPACE;
	private static final String INLINE_CONTEXT = """
			"@context": [ "http://www.w3.org/ns/odrl.jsonld",
			  { "vc": "https://w3id.org/fiware/dsc/vc#", "dspace": "https://w3id.org/dspace/2024/1/" } ]""";

	private static String permission(String constraint) {
		return """
				{ %s, "@type": "Offer", "odrl:uid": "urn:policy:1",
				  "permission": [ { "action": "read", "dspace:scope": "catalog", "constraint": %s } ] }"""
				.formatted(INLINE_CONTEXT, constraint);
	}

	private static String leftOperand(String operand) {
		return permission("""
				{ "leftOperand": %s, "operator": "odrl:eq", "rightOperand": "DE" }""".formatted(operand));
	}

	private static Map<String, Object> parse(String json) {
		try {
			return JSONObjectUtils.parse(json);
		} catch (ParseException e) {
			throw new IllegalArgumentException(json, e);
		}
	}

	private static Stream<Arguments> policies() {
		return Stream.of(
				Arguments.of("UC-1 compact", leftOperand("\"vc:country\""), false),
				Arguments.of("UC-1 expanded", """
						{ "@id": "urn:policy:1",
						  "http://www.w3.org/ns/odrl/2/permission": [ {
						    "http://www.w3.org/ns/odrl/2/action": [ { "@id": "http://www.w3.org/ns/odrl/2/read" } ],
						    "https://w3id.org/dspace/2024/1/scope": [ { "@value": "catalog" } ],
						    "http://www.w3.org/ns/odrl/2/constraint": [ {
						      "http://www.w3.org/ns/odrl/2/leftOperand": [ { "@id": "%scountry" } ],
						      "http://www.w3.org/ns/odrl/2/operator": [ { "@id": "http://www.w3.org/ns/odrl/2/isAnyOf" } ],
						      "http://www.w3.org/ns/odrl/2/rightOperand": [ { "@value": "DE" } ] } ] } ] }"""
						.formatted(VC_NAMESPACE), false),
				Arguments.of("odrl:spatial", leftOperand("\"odrl:spatial\""), false),
				Arguments.of("UC-2 dspace:membershipType under the dspace context", """
						{ "@context": [ "http://www.w3.org/ns/odrl.jsonld", "https://w3id.org/dspace/2024/1/context.json" ],
						  "@type": "Offer", "odrl:uid": "urn:policy:2",
						  "permission": [ { "action": "use", "dspace:scope": "contract.negotiation",
						    "constraint": { "leftOperand": "dspace:membershipType", "operator": "eq",
						                    "rightOperand": "FullMember" } } ] }""", false),
				Arguments.of("UC-6 nested odrl:leftOperand by @id", """
						{ "@context": { "odrl": "http://www.w3.org/ns/odrl/2/", "vc": "https://w3id.org/fiware/dsc/vc#" },
						  "odrl:permission": { "odrl:constraint": {
						    "odrl:leftOperand": { "@id": "vc:country" }, "odrl:rightOperand": "DE" } } }""", false),
				Arguments.of("cred prefix bound to the vc namespace", """
						{ "@context": { "cred": "https://w3id.org/fiware/dsc/vc#" },
						  "permission": [ { "constraint": { "leftOperand": "cred:country", "rightOperand": "DE" } } ] }""",
						false),
				Arguments.of("no constraint at all", """
						{ %s, "permission": [ { "action": "read", "assignee": "did:web:partner" } ] }"""
						.formatted(INLINE_CONTEXT), false),
				Arguments.of("inline typed compact IRI terms", """
						{ "@context": { "odrl": "http://www.w3.org/ns/odrl/2/", "vc": "https://w3id.org/fiware/dsc/vc#",
						                "odrl:leftOperand": { "@type": "@id" }, "odrl:action": { "@type": "@id" } },
						  "odrl:permission": { "odrl:action": "odrl:read", "odrl:constraint": {
						    "odrl:leftOperand": "vc:country", "odrl:rightOperand": "DE" } } }""", false),
				Arguments.of("UC-3 refinement on the target", """
						{ "@context": { "odrl": "http://www.w3.org/ns/odrl/2/", "dspace": "https://w3id.org/dspace/2024/1/" },
						  "odrl:permission": {
						    "odrl:assignee": { "@id": "did:web:fancy-marketplace.biz" },
						    "odrl:target": { "@type": "odrl:AssetCollection", "odrl:refinement": { "odrl:and": [
						      { "odrl:leftOperand": "tmf:resource", "odrl:rightOperand": "catalog" },
						      { "odrl:leftOperand": "tmf:resourceId", "odrl:rightOperand": "urn:ngsi-ld:Catalog:p" } ] } },
						    "dspace:scope": "catalog" } }""", true),
				Arguments.of("plain odrl:target id", """
						{ "odrl:permission": { "odrl:target": "urn:ngsi-ld:Catalog:p", "odrl:action": "read" } }""", true),
				Arguments.of("expanded target key", """
						{ "http://www.w3.org/ns/odrl/2/permission": [ {
						    "http://www.w3.org/ns/odrl/2/target": [ { "@id": "urn:ngsi-ld:Catalog:p" } ] } ] }""", true),
				Arguments.of("tmf:relatedParty in a permission constraint", leftOperand("\"tmf:relatedParty\""), true),
				Arguments.of("expanded unknown tmf operand",
						leftOperand("\"%sproductSpecification\"".formatted(TMF_NAMESPACE)), true),
				Arguments.of("aliased tmf prefix", """
						{ "@context": { "t": "%s" },
						  "permission": [ { "constraint": { "leftOperand": "t:catalog", "rightOperand": "c" } } ] }"""
						.formatted(TMF_NAMESPACE), true),
				Arguments.of("vc rebound to the tmf namespace", """
						{ "@context": { "vc": "%s" },
						  "permission": [ { "constraint": { "leftOperand": "vc:country", "rightOperand": "DE" } } ] }"""
						.formatted(TMF_NAMESPACE), true),
				Arguments.of("prefix bound twice", """
						{ "@context": { "cred": "https://w3id.org/fiware/dsc/vc#" },
						  "permission": [ { "@context": { "cred": "%s" },
						    "constraint": { "leftOperand": "cred:country", "rightOperand": "DE" } } ] }"""
						.formatted(TMF_NAMESPACE), true),
				Arguments.of("compact IRI redefined as a term", """
						{ "@context": { "vc:country": "%scatalog" },
						  "permission": [ { "constraint": { "leftOperand": "vc:country", "rightOperand": "DE" } } ] }"""
						.formatted(TMF_NAMESPACE), true),
				Arguments.of("term aliasing odrl:target", """
						{ "@context": { "odrl": "http://www.w3.org/ns/odrl/2/", "asset": "odrl:target" },
						  "permission": [ { "asset": "urn:ngsi-ld:Catalog:p" } ] }""", true),
				Arguments.of("inheritFrom a parent policy", """
						{ "inheritFrom": "urn:policy:parent", "permission": [ { "action": "read" } ] }""", true),
				Arguments.of("prefix splitting the odrl:target IRI", """
						{ "@context": { "ot": "http://www.w3.org/ns/odrl/2/tar" },
						  "permission": [ { "ot:get": "urn:ngsi-ld:Catalog:p" } ] }""", true),
				Arguments.of("term aliasing a split-prefix target", """
						{ "@context": { "ot": "http://www.w3.org/ns/odrl/2/tar", "asset": "ot:get" },
						  "permission": [ { "asset": "urn:ngsi-ld:Catalog:p" } ] }""", true),
				Arguments.of("prefix chained through a default prefix", """
						{ "@context": { "o": "odrl:", "asset": "o:target" },
						  "permission": [ { "asset": "urn:ngsi-ld:Catalog:p" } ] }""", true),
				Arguments.of("@vocab splitting the odrl:target IRI", """
						{ "@context": { "@vocab": "http://www.w3.org/ns/odrl/2/tar" },
						  "permission": [ { "get": "urn:ngsi-ld:Catalog:p" } ] }""", true),
				Arguments.of("any inline @vocab", """
						{ "@context": [ { "@vocab": "http://www.w3.org/ns/odrl/2/" },
						                { "vc": "https://w3id.org/fiware/dsc/vc#" } ],
						  "permission": [ { "constraint": { "leftOperand": "vc:country", "rightOperand": "DE" } } ] }""",
						true),
				Arguments.of("key term with a @reverse definition", """
						{ "@context": { "asset": { "@reverse": "http://www.w3.org/ns/odrl/2/target" } },
						  "permission": [ { "asset": "urn:ngsi-ld:Catalog:p" } ] }""", true),
				Arguments.of("cyclic term definitions", """
						{ "@context": { "a": "b:x", "b": "a:y" }, "permission": [ { "a": "urn:ngsi-ld:Catalog:p" } ] }""",
						true),
				Arguments.of("unknown remote context without constraints", """
						{ "@context": [ "http://www.w3.org/ns/odrl.jsonld", "https://example.org/context.jsonld" ],
						  "permission": [ { "action": "read", "asset": "urn:ngsi-ld:Catalog:p" } ] }""", true),
				Arguments.of("unbound prefix", leftOperand("\"foo:country\""), true),
				Arguments.of("bare term", leftOperand("\"country\""), true),
				Arguments.of("vc:country under an unknown remote context", """
						{ "@context": [ "http://www.w3.org/ns/odrl.jsonld", "https://example.org/context.jsonld" ],
						  "permission": [ { "constraint": { "leftOperand": "vc:country", "rightOperand": "DE" } } ] }""",
						true),
				Arguments.of("vc:country under an unknown @import", """
						{ "@context": { "@import": "https://example.org/context.jsonld" },
						  "permission": [ { "constraint": { "leftOperand": "vc:country", "rightOperand": "DE" } } ] }""",
						true),
				Arguments.of("subject-side next to a tmf operand inside an and", permission("""
						{ "and": [ { "leftOperand": "vc:country", "operator": "odrl:eq", "rightOperand": "DE" },
						           { "leftOperand": "tmf:catalog", "operator": "odrl:eq", "rightOperand": "c" } ] }"""),
						true),
				Arguments.of("tmf operand in a prohibition duty", """
						{ "prohibition": [ { "duty": [ { "constraint": [
						  { "leftOperand": "tmf:category", "rightOperand": "x" } ] } ] } ] }""", true),
				Arguments.of("rightOperandReference", permission("""
						{ "leftOperand": "vc:country", "operator": "odrl:eq", "rightOperandReference": "urn:ref" }"""),
						true),
				Arguments.of("leftOperand as a number", leftOperand("42"), true),
				Arguments.of("leftOperand as a @value object", leftOperand("{ \"@value\": \"vc:country\" }"), true),
				Arguments.of("leftOperand as a two-element list", leftOperand("[ \"vc:country\", \"tmf:catalog\" ]"),
						true),
				Arguments.of("vc namespace itself", leftOperand("\"%s\"".formatted(VC_NAMESPACE)), true));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("policies")
	void detectsResourceConstraints(String description, String policy, boolean expected) {
		assertEquals(expected, ResourceConstraintDetector.constrainsResource(parse(policy)));
	}

	@Test
	void treatsANullPolicyAsResourceConstrained() {
		assertTrue(ResourceConstraintDetector.constrainsResource(null));
	}
}
