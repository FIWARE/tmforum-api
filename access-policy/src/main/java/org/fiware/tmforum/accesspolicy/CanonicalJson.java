package org.fiware.tmforum.accesspolicy;

import com.nimbusds.jose.util.JSONObjectUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * SHA-256 digests of JSON-shaped maps that ignore key order at every depth while keeping list order.
 * Values must be what JSON parsing yields (maps, lists, strings, numbers, booleans, null), never records or beans.
 */
final class CanonicalJson {

	private static final String DIGEST_ALGORITHM = "SHA-256";

	private CanonicalJson() {
	}

	/** Lower-case hex SHA-256 of {@code value}'s canonical JSON. */
	static String digest(Map<String, ?> value) {
		String canonicalJson = JSONObjectUtils.toJSONString(canonicalMap(value));
		try {
			byte[] digest = MessageDigest.getInstance(DIGEST_ALGORITHM)
					.digest(canonicalJson.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Every Java platform must provide " + DIGEST_ALGORITHM, e);
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
			return list.stream().map(CanonicalJson::canonicalValue).toList();
		}
		return value;
	}
}
