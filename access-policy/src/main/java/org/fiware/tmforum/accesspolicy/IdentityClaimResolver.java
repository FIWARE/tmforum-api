package org.fiware.tmforum.accesspolicy;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Finds the caller's identity in a token's claims by the configured {@code jwt.identity-claim} path.
 */
final class IdentityClaimResolver {

	private static final Pattern PATH_SEPARATOR = Pattern.compile("\\.");
	/** Key of the identifier when the terminal value is an object, as a VC {@code issuer} may be. */
	private static final String ID_KEY = "id";

	private IdentityClaimResolver() {
	}

	/**
	 * Resolves {@code path}, preferring a top-level claim of exactly that name over a walk of its {@code .}-separated
	 * segments. Empty unless the value is a non-blank string or an object with a non-blank string {@code id}.
	 */
	static Optional<String> resolve(Map<String, Object> claims, String path) {
		if (claims.containsKey(path)) {
			return identifier(claims.get(path));
		}
		Object current = claims;
		for (String segment : PATH_SEPARATOR.split(path, -1)) {
			current = child(current, segment);
			if (current == null) {
				return Optional.empty();
			}
		}
		return identifier(current);
	}

	private static Object child(Object node, String segment) {
		Object unwrapped = node;
		if (unwrapped instanceof List<?> list) {
			if (list.size() != 1) {
				return null;
			}
			unwrapped = list.getFirst();
		}
		return unwrapped instanceof Map<?, ?> map ? map.get(segment) : null;
	}

	private static Optional<String> identifier(Object value) {
		Object candidate = value instanceof Map<?, ?> map ? map.get(ID_KEY) : value;
		return candidate instanceof String identity && !identity.isBlank() ? Optional.of(identity) : Optional.empty();
	}
}
