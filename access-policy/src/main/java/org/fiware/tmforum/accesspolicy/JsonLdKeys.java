package org.fiware.tmforum.accesspolicy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The JSON-LD key expansion the policy readers share: inline contexts merged per policy, compact IRIs and the
 * default prefixes. Never fetches a remote context; it only records whether one is trusted.
 */
final class JsonLdKeys {

	static final String VC_NAMESPACE = "https://w3id.org/fiware/dsc/vc#";
	static final String DSPACE_NAMESPACE = "https://w3id.org/dspace/2024/1/";
	static final String ODRL_NAMESPACE = "http://www.w3.org/ns/odrl/2/";
	static final String ODRL_CONTEXT = "http://www.w3.org/ns/odrl.jsonld";
	static final String DSPACE_CONTEXT = "https://w3id.org/dspace/2024/1/context.json";

	static final Map<String, String> DEFAULT_PREFIXES = Map.of(
			"vc", VC_NAMESPACE,
			"dspace", DSPACE_NAMESPACE,
			"odrl", ODRL_NAMESPACE);
	// Checked by hand: both bind odrl/dspace to the namespaces above, set no @vocab and alias none of the detector's
	// resource keys. odrl.jsonld also binds bare ODRL terms and aliases uid/type to @id/@type: the resolver reads them
	// through ScopedPolicyResolution.ODRL_CONTEXT_TERMS, and the DSP context's terms are not interpreted. A new entry
	// must be checked against both readers.
	static final Set<String> TRUSTED_REMOTE_CONTEXTS = Set.of(ODRL_CONTEXT, DSPACE_CONTEXT);

	static final String CONTEXT = "@context";
	static final String ID = "@id";
	static final String TYPE = "@type";
	static final String VALUE = "@value";
	static final String KEYWORD_MARKER = "@";

	private static final List<String> ABSOLUTE_IRI_SCHEMES = List.of("http://", "https://");
	private static final String IMPORT = "@import";
	private static final String VOCAB = "@vocab";
	private static final String REVERSE = "@reverse";
	// Bounds term-to-term chains, which also stops a cyclic context.
	private static final int MAX_EXPANSION_DEPTH = 8;
	private static final char COMPACT_IRI_SEPARATOR = ':';
	private static final String LOCAL_NAME_SEPARATORS = "#/:";

	private JsonLdKeys() {
	}

	/** The bindings of every inline context anywhere in {@code node}, merged regardless of scope. */
	static Bindings collectContexts(Object node) {
		Bindings bindings = new Bindings();
		collectContexts(node, bindings);
		return bindings;
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
			if (TRUSTED_REMOTE_CONTEXTS.contains(remote)) {
				bindings.trustedRemoteContexts.add(remote);
			} else {
				bindings.untrustedRemoteContext = true;
			}
		} else if (context instanceof List<?> list) {
			list.forEach(element -> readContext(element, bindings));
		} else if (context instanceof Map<?, ?> definitions) {
			definitions.forEach((key, definition) -> {
				String term = String.valueOf(key);
				if (IMPORT.equals(term)) {
					readContext(definition, bindings);
				} else if (VOCAB.equals(term)) {
					bindings.vocab |= definition != null;
				} else if (!term.startsWith(KEYWORD_MARKER)) {
					if (!onlyTypesCompactIri(term, definition)) {
						bindings.bind(term, definitionIri(definition));
					}
					if (definition instanceof Map<?, ?> expanded) {
						readContext(expanded.get(CONTEXT), bindings);
					}
				}
			});
		}
	}

	// {"odrl:target": {"@type": "@id"}} keeps the compact IRI's own expansion, so it binds nothing.
	private static boolean onlyTypesCompactIri(String term, Object definition) {
		return term.indexOf(COMPACT_IRI_SEPARATOR) > 0 && definition instanceof Map<?, ?> expanded
				&& !expanded.containsKey(ID) && !expanded.containsKey(REVERSE);
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

	/**
	 * An IRI-valued operand: a string, {@code {"@id": …}} or a single-element list of either, since expanded JSON-LD
	 * wraps every value in a list. Null for any other shape.
	 */
	static String operandIri(Object operand) {
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

	/** Expands an IRI-valued operand; null for anything without a colon or under an untrusted context. */
	static String expand(String iri, Bindings bindings) {
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

	/**
	 * Expands a key the way a JSON-LD processor would, so a prefix that splits a key's IRI cannot hide it. An unbound
	 * bare term comes back unchanged; null when the key cannot be expanded (an unresolvable term, or a long chain).
	 */
	static String expandKey(String key, Bindings bindings) {
		return expandKey(key, bindings, 0);
	}

	private static String expandKey(String key, Bindings bindings, int depth) {
		if (key == null || depth > MAX_EXPANSION_DEPTH) {
			return null;
		}
		if (key.startsWith(KEYWORD_MARKER) || ABSOLUTE_IRI_SCHEMES.stream().anyMatch(key::startsWith)) {
			return key;
		}
		if (bindings.isBound(key)) {
			return expandKey(bindings.definition(key), bindings, depth + 1);
		}
		int separator = key.indexOf(COMPACT_IRI_SEPARATOR);
		if (separator < 0) {
			return key;
		}
		String prefix = key.substring(0, separator);
		String suffix = key.substring(separator + 1);
		if (bindings.isBound(prefix)) {
			String namespace = expandKey(bindings.definition(prefix), bindings, depth + 1);
			return namespace == null ? null : namespace + suffix;
		}
		String namespace = DEFAULT_PREFIXES.get(prefix);
		// An unbound, non-default prefix is an IRI scheme (urn:, did:), so the key is already absolute.
		return namespace == null ? key : namespace + suffix;
	}

	/** True for an absolute IRI (or a compact one with an unknown prefix, which JSON-LD reads as a scheme). */
	static boolean isAbsolute(String iri) {
		return iri.indexOf(COMPACT_IRI_SEPARATOR) > 0;
	}

	static boolean isKeyword(String key) {
		return key.startsWith(KEYWORD_MARKER);
	}

	static String localName(String key) {
		int lastSeparator = -1;
		for (char separator : LOCAL_NAME_SEPARATORS.toCharArray()) {
			lastSeparator = Math.max(lastSeparator, key.lastIndexOf(separator));
		}
		return key.substring(lastSeparator + 1);
	}

	/** The term bindings of every inline context in one policy, merged regardless of scope. */
	static final class Bindings {
		private final Map<String, String> terms = new HashMap<>();
		private final Set<String> unresolvable = new HashSet<>();
		private final Set<String> trustedRemoteContexts = new HashSet<>();
		private boolean untrustedRemoteContext;
		private boolean vocab;

		void bind(String term, String iri) {
			if (iri == null) {
				unresolvable.add(term);
				return;
			}
			String previous = terms.putIfAbsent(term, iri);
			if (previous != null && !previous.equals(iri)) {
				unresolvable.add(term);
			}
		}

		/** The IRI a term is bound to; null if unbound or unresolvable. */
		String definition(String term) {
			return unresolvable.contains(term) ? null : terms.get(term);
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

		boolean untrustedRemoteContext() {
			return untrustedRemoteContext;
		}

		boolean vocab() {
			return vocab;
		}

		/** True when the policy names this trusted remote context; its terms are not in {@link #definition}. */
		boolean namesContext(String remoteContext) {
			return trustedRemoteContexts.contains(remoteContext);
		}
	}
}
