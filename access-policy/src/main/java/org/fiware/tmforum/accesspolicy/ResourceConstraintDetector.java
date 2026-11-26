package org.fiware.tmforum.accesspolicy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Decides whether a policy's verdict may depend on the resource, so a cached decision is only shared across resources
 * for policies proven subject-only. Fail-safe: whatever this cannot prove subject-side counts as resource-side.
 */
final class ResourceConstraintDetector {

	static final String VC_NAMESPACE = "https://w3id.org/fiware/dsc/vc#";
	static final String DSPACE_NAMESPACE = "https://w3id.org/dspace/2024/1/";
	static final String ODRL_NAMESPACE = "http://www.w3.org/ns/odrl/2/";

	// POLICY_EXAMPLES.md § Vocabulary; besides these, every IRI in VC_NAMESPACE is subject-side.
	private static final Set<String> SUBJECT_SIDE_IRIS = Set.of(
			DSPACE_NAMESPACE + "membershipType",
			DSPACE_NAMESPACE + "scope",
			ODRL_NAMESPACE + "spatial");
	private static final Map<String, String> DEFAULT_PREFIXES = Map.of(
			"vc", VC_NAMESPACE,
			"dspace", DSPACE_NAMESPACE,
			"odrl", ODRL_NAMESPACE);
	// Checked by hand: both bind only odrl/dspace (to the namespaces above) and alias none of the watched keys.
	private static final Set<String> TRUSTED_REMOTE_CONTEXTS = Set.of(
			"http://www.w3.org/ns/odrl.jsonld",
			"https://w3id.org/dspace/2024/1/context.json");
	private static final List<String> ABSOLUTE_IRI_SCHEMES = List.of("http://", "https://");

	private static final String TARGET = "target";
	private static final String RIGHT_OPERAND_REFERENCE = "rightOperandReference";
	private static final String LEFT_OPERAND = "leftOperand";
	private static final Set<String> WATCHED_KEYS = Set.of(TARGET, RIGHT_OPERAND_REFERENCE, LEFT_OPERAND);
	private static final String CONTEXT = "@context";
	private static final String ID = "@id";
	private static final String IMPORT = "@import";
	private static final String KEYWORD_MARKER = "@";
	private static final char COMPACT_IRI_SEPARATOR = ':';
	private static final String LOCAL_NAME_SEPARATORS = "#/:";

	private ResourceConstraintDetector() {
	}

	/** True unless {@code policy} provably constrains only the subject; never throws. */
	static boolean constrainsResource(Map<String, Object> policy) {
		if (policy == null) {
			return true;
		}
		try {
			Bindings bindings = new Bindings();
			collectContexts(policy, bindings);
			return bindings.aliasesWatchedKey || constrainsResource(policy, bindings);
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
			String localName = localName(key);
			if (TARGET.equals(localName) || RIGHT_OPERAND_REFERENCE.equals(localName)) {
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

	private static void collectContexts(Object node, Bindings bindings) {
		if (node instanceof List<?> list) {
			list.forEach(element -> collectContexts(element, bindings));
		} else if (node instanceof Map<?, ?> map) {
			map.forEach((key, value) -> {
				if (CONTEXT.equals(key)) {
					readContext(value, bindings);
				} else {
					collectContexts(value, bindings);
				}
			});
		}
	}

	private static void readContext(Object context, Bindings bindings) {
		if (context instanceof String remote) {
			bindings.untrustedRemoteContext |= !TRUSTED_REMOTE_CONTEXTS.contains(remote);
		} else if (context instanceof List<?> list) {
			list.forEach(element -> readContext(element, bindings));
		} else if (context instanceof Map<?, ?> definitions) {
			definitions.forEach((key, definition) -> {
				String term = String.valueOf(key);
				if (IMPORT.equals(term)) {
					readContext(definition, bindings);
				} else if (!term.startsWith(KEYWORD_MARKER)) {
					bindings.bind(term, definitionIri(definition));
					if (definition instanceof Map<?, ?> expanded) {
						readContext(expanded.get(CONTEXT), bindings);
					}
				}
			});
		}
	}

	private static String definitionIri(Object definition) {
		if (definition instanceof String iri) {
			return iri;
		}
		if (definition instanceof Map<?, ?> expanded && expanded.get(ID) instanceof String iri) {
			return iri;
		}
		return null;
	}

	// Expanded JSON-LD wraps every value in a list, so a single-element list is read like its element.
	private static String operandIri(Object operand) {
		if (operand instanceof List<?> list && list.size() == 1) {
			return operandIri(list.getFirst());
		}
		if (operand instanceof String iri) {
			return iri;
		}
		if (operand instanceof Map<?, ?> reference && reference.get(ID) instanceof String iri) {
			return iri;
		}
		return null;
	}

	private static String expand(String iri, Bindings bindings) {
		if (iri == null || ABSOLUTE_IRI_SCHEMES.stream().anyMatch(iri::startsWith)) {
			return iri;
		}
		int separator = iri.indexOf(COMPACT_IRI_SEPARATOR);
		// A context term named like the compact IRI itself would override the prefix expansion.
		if (separator < 0 || bindings.untrustedRemoteContext || bindings.isBound(iri)) {
			return null;
		}
		String namespace = bindings.resolvePrefix(iri.substring(0, separator));
		return namespace == null ? null : namespace + iri.substring(separator + 1);
	}

	private static boolean isSubjectSide(String iri) {
		return iri != null && ((iri.startsWith(VC_NAMESPACE) && iri.length() > VC_NAMESPACE.length())
				|| SUBJECT_SIDE_IRIS.contains(iri));
	}

	private static String localName(String key) {
		int lastSeparator = -1;
		for (char separator : LOCAL_NAME_SEPARATORS.toCharArray()) {
			lastSeparator = Math.max(lastSeparator, key.lastIndexOf(separator));
		}
		return key.substring(lastSeparator + 1);
	}

	/** The term bindings of every inline context in one policy, merged regardless of scope. */
	private static final class Bindings {
		private final Map<String, String> terms = new HashMap<>();
		private final Set<String> unresolvable = new HashSet<>();
		private boolean untrustedRemoteContext;
		private boolean aliasesWatchedKey;

		void bind(String term, String iri) {
			if (iri == null) {
				unresolvable.add(term);
				return;
			}
			if (WATCHED_KEYS.contains(localName(iri)) && !localName(iri).equals(localName(term))) {
				aliasesWatchedKey = true;
			}
			String previous = terms.putIfAbsent(term, iri);
			if (previous != null && !previous.equals(iri)) {
				unresolvable.add(term);
			}
		}

		boolean isBound(String term) {
			return terms.containsKey(term) || unresolvable.contains(term);
		}

		String resolvePrefix(String prefix) {
			if (unresolvable.contains(prefix)) {
				return null;
			}
			return terms.getOrDefault(prefix, DEFAULT_PREFIXES.get(prefix));
		}
	}
}
