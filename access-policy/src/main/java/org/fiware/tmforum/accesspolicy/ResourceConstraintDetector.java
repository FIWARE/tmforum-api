package org.fiware.tmforum.accesspolicy;

import org.fiware.tmforum.accesspolicy.JsonLdKeys.Bindings;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.fiware.tmforum.accesspolicy.JsonLdKeys.CONTEXT;
import static org.fiware.tmforum.accesspolicy.JsonLdKeys.DSPACE_NAMESPACE;
import static org.fiware.tmforum.accesspolicy.JsonLdKeys.ODRL_NAMESPACE;
import static org.fiware.tmforum.accesspolicy.JsonLdKeys.collectContexts;
import static org.fiware.tmforum.accesspolicy.JsonLdKeys.expand;
import static org.fiware.tmforum.accesspolicy.JsonLdKeys.expandKey;
import static org.fiware.tmforum.accesspolicy.JsonLdKeys.localName;
import static org.fiware.tmforum.accesspolicy.JsonLdKeys.operandIri;

/**
 * Decides whether a policy's verdict may depend on the resource, so a cached decision is only shared across resources
 * for policies proven subject-only. Fail-safe: whatever this cannot prove subject-side counts as resource-side.
 */
final class ResourceConstraintDetector {

	static final String VC_NAMESPACE = JsonLdKeys.VC_NAMESPACE;

	// POLICY_EXAMPLES.md § Vocabulary; besides these, every IRI in VC_NAMESPACE is subject-side.
	private static final Set<String> SUBJECT_SIDE_IRIS = Set.of(
			DSPACE_NAMESPACE + "membershipType",
			DSPACE_NAMESPACE + "scope",
			ODRL_NAMESPACE + "spatial");

	private static final String TARGET = "target";
	private static final String RIGHT_OPERAND_REFERENCE = "rightOperandReference";
	// The parent policy named by inheritFrom may carry a target this detector never sees.
	private static final String INHERIT_FROM = "inheritFrom";
	private static final Set<String> RESOURCE_KEYS = Set.of(TARGET, RIGHT_OPERAND_REFERENCE, INHERIT_FROM);
	private static final String LEFT_OPERAND = "leftOperand";

	private ResourceConstraintDetector() {
	}

	/** True unless {@code policy} provably constrains only the subject; never throws. */
	static boolean constrainsResource(Map<String, Object> policy) {
		if (policy == null) {
			return true;
		}
		try {
			Bindings bindings = collectContexts(policy);
			// A context this code cannot read could alias any key, even a bare one, to a resource key.
			return bindings.untrustedRemoteContext() || bindings.vocab() || constrainsResource(policy, bindings);
		} catch (RuntimeException e) {
			return true;
		}
	}

	private static boolean constrainsResource(Object node, Bindings bindings) {
		if (node instanceof List<?> list) {
			return list.stream().anyMatch(element -> constrainsResource(element, bindings));
		}
		if (!(node instanceof Map<?, ?> map)) {
			return false;
		}
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			String key = String.valueOf(entry.getKey());
			if (CONTEXT.equals(key)) {
				continue;
			}
			String iri = expandKey(key, bindings);
			if (iri == null) {
				return true;
			}
			String localName = localName(iri);
			if (RESOURCE_KEYS.contains(localName)) {
				return true;
			}
			if (LEFT_OPERAND.equals(localName) && !isSubjectSide(expand(operandIri(entry.getValue()), bindings))) {
				return true;
			}
			if (constrainsResource(entry.getValue(), bindings)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isSubjectSide(String iri) {
		return iri != null && ((iri.startsWith(VC_NAMESPACE) && iri.length() > VC_NAMESPACE.length())
				|| SUBJECT_SIDE_IRIS.contains(iri));
	}
}
