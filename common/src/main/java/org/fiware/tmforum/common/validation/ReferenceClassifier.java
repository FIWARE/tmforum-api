package org.fiware.tmforum.common.validation;

import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.configuration.ReferenceProperties;

import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Classifies references as local or external. A reference is external only if its id or href starts with an
 * operator-configured prefix; it is never inferred, so a client cannot switch validation off on its own.
 */
@Slf4j
@Singleton
public class ReferenceClassifier {

	/** Common start of all server-generated ids ({@code urn:ngsi-ld:<type>:<uuid>}). */
	private static final String LOCAL_ID_ROOT = "urn:ngsi-ld:";

	/** Id prefixes that only name a type, optionally continued by UUID characters, overlap local ids. */
	private static final Pattern LOCAL_ID_OVERLAP = Pattern.compile("^urn:ngsi-ld:[^:]*(:[0-9a-fA-F-]*)?$");

	private final List<String> externalIdPrefixes;
	private final List<String> externalHrefPrefixes;

	public ReferenceClassifier(ReferenceProperties referenceProperties) {
		this.externalIdPrefixes = sanitize(referenceProperties.getExternalIdPrefixes(), "external-id-prefixes");
		this.externalHrefPrefixes = sanitize(referenceProperties.getExternalHrefPrefixes(),
				"external-href-prefixes");
		externalIdPrefixes.stream()
				.filter(ReferenceClassifier::overlapsLocalIds)
				.forEach(prefix -> log.warn("External id prefix '{}' overlaps server-generated local ids; "
						+ "local references matching it are not validated. Use a deployment-specific prefix.",
						prefix));
	}

	/**
	 * An id-prefix match wins over an href-prefix match, since only the latter needs a local type guard.
	 */
	public ReferenceScope classify(ReferencedEntity ref) {
		if (matches(ref.getEntityId(), externalIdPrefixes)) {
			return ReferenceScope.EXTERNAL;
		}
		if (matches(ref.getReferenceHref(), externalHrefPrefixes)) {
			return ReferenceScope.EXTERNAL_BY_HREF;
		}
		return ReferenceScope.LOCAL;
	}

	/**
	 * Whether the id alone marks a reference as external; the read path only sees ids.
	 */
	public boolean isExternal(URI id) {
		return matches(id, externalIdPrefixes);
	}

	private static boolean matches(URI uri, List<String> prefixes) {
		if (uri == null) {
			return false;
		}
		String value = uri.toString();
		return prefixes.stream().anyMatch(value::startsWith);
	}

	private static boolean overlapsLocalIds(String prefix) {
		return LOCAL_ID_ROOT.startsWith(prefix) || LOCAL_ID_OVERLAP.matcher(prefix).matches();
	}

	// a blank prefix would match everything and silently switch validation off
	private static List<String> sanitize(List<String> prefixes, String property) {
		if (prefixes == null) {
			return List.of();
		}
		return prefixes.stream()
				.map(prefix -> prefix == null ? "" : prefix.trim())
				.filter(prefix -> {
					if (prefix.isEmpty()) {
						log.warn("Ignoring blank entry in references.{}.", property);
					}
					return !prefix.isEmpty();
				})
				.toList();
	}
}
