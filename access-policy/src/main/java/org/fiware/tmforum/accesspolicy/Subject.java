package org.fiware.tmforum.accesspolicy;

import com.nimbusds.jose.util.JSONObjectUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * The caller a request is evaluated for. {@code claims} are the token's non-registered claims, i.e. the presented
 * credentials a policy constrains on, deeply immutable. A {@code null} identity and expiry occur only on
 * {@link #ANONYMOUS}; an unresolved subject is never a {@code Subject} but an absent request attribute.
 */
public record Subject(String identity, Map<String, Object> claims, Instant expiresAt, String credentialFingerprint) {

	private static final String FINGERPRINT_DIGEST_ALGORITHM = "SHA-256";

	public static final Subject ANONYMOUS = new Subject(null, Map.of(), null, null);

	/**
	 * Admits only the {@link #ANONYMOUS} shape and identified subjects with an expiry and the fingerprint of
	 * {@code claims}; anything else is an {@link IllegalArgumentException}.
	 */
	public Subject {
		claims = immutableMap(Objects.requireNonNull(claims, "claims"));
		if (identity == null) {
			if (!claims.isEmpty() || expiresAt != null || credentialFingerprint != null) {
				throw new IllegalArgumentException("A subject without identity must carry nothing else.");
			}
		} else {
			if (expiresAt == null) {
				throw new IllegalArgumentException("An identified subject requires an expiry.");
			}
			if (!fingerprint(claims).equals(credentialFingerprint)) {
				throw new IllegalArgumentException("The credential fingerprint does not match the claims.");
			}
		}
	}

	/**
	 * Creates an authenticated subject whose fingerprint covers exactly {@code claims}, so a refreshed token with the
	 * same credentials keeps it.
	 */
	public static Subject of(String identity, Map<String, Object> claims, Instant expiresAt) {
		Objects.requireNonNull(identity, "identity");
		Objects.requireNonNull(expiresAt, "expiresAt");
		return new Subject(identity, claims, expiresAt, fingerprint(claims));
	}

	/** True only for {@link #ANONYMOUS}. */
	public boolean isAnonymous() {
		return identity == null;
	}

	private static String fingerprint(Map<String, Object> claims) {
		String canonicalJson = JSONObjectUtils.toJSONString(canonicalMap(claims));
		try {
			byte[] digest = MessageDigest.getInstance(FINGERPRINT_DIGEST_ALGORITHM)
					.digest(canonicalJson.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Every Java platform must provide " + FINGERPRINT_DIGEST_ALGORITHM, e);
		}
	}

	private static Map<String, Object> canonicalMap(Map<?, ?> map) {
		Map<String, Object> sorted = new TreeMap<>();
		map.forEach((key, value) -> sorted.put(String.valueOf(key), canonicalValue(value)));
		return sorted;
	}

	private static Object canonicalValue(Object value) {
		if (value instanceof Map<?, ?> map) {
			return canonicalMap(map);
		}
		if (value instanceof List<?> list) {
			return list.stream().map(Subject::canonicalValue).toList();
		}
		return value;
	}

	private static Map<String, Object> immutableMap(Map<?, ?> map) {
		Map<String, Object> copy = new LinkedHashMap<>();
		map.forEach((key, value) -> copy.put(String.valueOf(key), immutableValue(value)));
		return Collections.unmodifiableMap(copy);
	}

	private static Object immutableValue(Object value) {
		if (value instanceof Map<?, ?> map) {
			return immutableMap(map);
		}
		if (value instanceof List<?> list) {
			List<Object> copy = new ArrayList<>(list.size());
			list.forEach(element -> copy.add(immutableValue(element)));
			return Collections.unmodifiableList(copy);
		}
		return value;
	}
}
