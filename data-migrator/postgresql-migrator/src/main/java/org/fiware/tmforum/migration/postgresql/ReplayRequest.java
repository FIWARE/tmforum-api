package org.fiware.tmforum.migration.postgresql;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * One captured API request: a line {@code {"method":"GET","path":"/tmf-api/...?limit=10","headers":{..},"body":..}}.
 * {@code body} is null without one; a JSON string body is sent as its text, any other JSON value as JSON.
 */
record ReplayRequest(String method, String path, Map<String, String> headers, JsonNode body) {

	static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
	private static final String QUERY_SEPARATOR = "?";

	/**
	 * Parses one line. Throws {@link IllegalArgumentException} when the method or the path is missing.
	 */
	static ReplayRequest parse(String line, JsonMapper mapper) {
		JsonNode node = mapper.readTree(line);
		String method = node.path("method").asString(null);
		String path = node.path("path").asString(null);
		if (method == null || method.isBlank() || path == null || !path.startsWith("/")) {
			throw new IllegalArgumentException("a request needs a method and a path starting with /.");
		}
		Map<String, String> headers = new LinkedHashMap<>();
		node.path("headers").properties().forEach(header -> headers.put(header.getKey(), header.getValue().asString()));
		JsonNode body = node.get("body");
		return new ReplayRequest(method.toUpperCase(Locale.ROOT), path, Map.copyOf(headers),
				body == null || body.isNull() ? null : body);
	}

	boolean isWrite() {
		return WRITE_METHODS.contains(method);
	}

	/**
	 * The path without its query string.
	 */
	String pathOnly() {
		int query = path.indexOf(QUERY_SEPARATOR);
		return query < 0 ? path : path.substring(0, query);
	}

	/**
	 * The body as sent, or null.
	 */
	String bodyText() {
		if (body == null) {
			return null;
		}
		return body.isString() ? body.asString() : body.toString();
	}
}
