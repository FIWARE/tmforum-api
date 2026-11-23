package org.fiware.tmforum.migration.postgresql;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ContainerNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Compares two JSON response bodies of one replayed request. Key order never matters; list order only where the rules
 * say so. Before comparing, both sides drop the ignored pointers, reduce references to external targets to their
 * {@code id} and {@code href}, and cut every absolute {@code href} down to its path, since each deployment renders
 * its own server host.
 */
final class ResponseDiff {

	static final String HREF = "href";
	static final String ID = "id";
	static final String ANY_KEY = "*";
	static final String ANY_DEPTH = "**";
	// longer values are cut in the report, a whole body would drown the cause
	static final int MAX_VALUE_LENGTH = 200;
	private static final String POINTER_SEPARATOR = "/";
	private static final String TRUNCATED = "...";
	private static final String ABSENT = "<absent>";

	/**
	 * What to compare for one request; {@code ignored} holds JSON pointer patterns.
	 */
	record Rules(boolean unordered, List<String> ignored, Set<String> externalHosts) {
	}

	private ResponseDiff() {
	}

	/**
	 * The segments of a JSON pointer pattern, unescaped. Throws {@link IllegalArgumentException} unless it starts
	 * with {@code /}.
	 */
	static List<String> parsePointer(String pointer) {
		if (pointer == null || !pointer.startsWith(POINTER_SEPARATOR)) {
			throw new IllegalArgumentException("A JSON pointer must start with /, was '" + pointer + "'.");
		}
		return Arrays.stream(pointer.substring(1).split(POINTER_SEPARATOR, -1))
				.map(segment -> segment.replace("~1", "/").replace("~0", "~"))
				.toList();
	}

	/**
	 * Every difference as {@code <pointer>: broker <value>, postgresql <value>}; empty when the bodies are equal under
	 * the rules. Neither argument is modified.
	 */
	static List<String> diff(JsonNode broker, JsonNode postgres, Rules rules) {
		JsonNode left = normalize(broker, rules);
		JsonNode right = normalize(postgres, rules);
		List<String> differences = new ArrayList<>();
		compare(left, right, "", rules.unordered(), differences);
		return differences;
	}

	private static JsonNode normalize(JsonNode body, Rules rules) {
		if (body == null) {
			return null;
		}
		JsonNode copy = body.deepCopy();
		for (String pointer : rules.ignored()) {
			removeBelow(copy, parsePointer(pointer));
		}
		normalizeReferences(copy, rules.externalHosts());
		return copy;
	}

	private static void removeBelow(JsonNode node, List<String> segments) {
		if (node == null || segments.isEmpty() || !node.isContainer()) {
			return;
		}
		String head = segments.getFirst();
		List<String> rest = segments.subList(1, segments.size());
		if (ANY_DEPTH.equals(head)) {
			if (rest.isEmpty()) {
				// a trailing ** matches everything below, the container itself stays
				((ContainerNode<?>) node).removeAll();
				return;
			}
			// ** matching nothing, then one level deeper with ** still pending
			removeBelow(node, rest);
			children(node).forEach(child -> removeBelow(child, segments));
			return;
		}
		if (node instanceof ObjectNode object) {
			List<String> keys = ANY_KEY.equals(head) ? List.copyOf(object.propertyNames()) : List.of(head);
			for (String key : keys) {
				if (rest.isEmpty()) {
					object.remove(key);
				} else {
					removeBelow(object.get(key), rest);
				}
			}
		} else if (node instanceof ArrayNode array) {
			for (int index = array.size() - 1; index >= 0; index--) {
				if (!ANY_KEY.equals(head) && !String.valueOf(index).equals(head)) {
					continue;
				}
				if (rest.isEmpty()) {
					array.remove(index);
				} else {
					removeBelow(array.get(index), rest);
				}
			}
		}
	}

	private static List<JsonNode> children(JsonNode node) {
		List<JsonNode> children = new ArrayList<>();
		node.values().forEach(children::add);
		return children;
	}

	private static void normalizeReferences(JsonNode node, Set<String> externalHosts) {
		if (node instanceof ObjectNode object) {
			JsonNode href = object.get(HREF);
			if (href != null && href.isString()) {
				URI uri = absoluteUri(href.asString());
				if (uri != null) {
					if (externalHosts.contains(uri.getHost())) {
						object.retain(ID, HREF);
					}
					object.set(HREF, StringNode.valueOf(pathOf(uri)));
				}
			}
		}
		if (node != null && node.isContainer()) {
			children(node).forEach(child -> normalizeReferences(child, externalHosts));
		}
	}

	private static URI absoluteUri(String value) {
		try {
			URI uri = URI.create(value);
			return uri.isAbsolute() && uri.getHost() != null ? uri : null;
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static String pathOf(URI uri) {
		return uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
	}

	private static void compare(JsonNode broker, JsonNode postgres, String path, boolean unordered,
			List<String> differences) {
		if (equal(broker, postgres, unordered)) {
			return;
		}
		if (broker instanceof ObjectNode left && postgres instanceof ObjectNode right) {
			Set<String> keys = new TreeSet<>(left.propertyNames());
			keys.addAll(right.propertyNames());
			keys.forEach(key -> compare(left.get(key), right.get(key), path + POINTER_SEPARATOR + escape(key), unordered,
					differences));
		} else if (broker instanceof ArrayNode left && postgres instanceof ArrayNode right) {
			if (unordered) {
				compareAsSets(left, right, path, differences);
			} else {
				for (int i = 0; i < Math.max(left.size(), right.size()); i++) {
					compare(left.get(i), right.get(i), path + POINTER_SEPARATOR + i, false, differences);
				}
			}
		} else {
			differences.add(difference(path, render(broker), render(postgres)));
		}
	}

	private static void compareAsSets(ArrayNode broker, ArrayNode postgres, String path, List<String> differences) {
		Map<String, Long> counts = new HashMap<>();
		broker.values().forEach(element -> counts.merge(canonical(element, true), 1L, Long::sum));
		postgres.values().forEach(element -> counts.merge(canonical(element, true), -1L, Long::sum));
		String onlyBroker = elements(counts, 1);
		String onlyPostgres = elements(counts, -1);
		differences.add(difference(path, "only in the list " + onlyBroker, "only in the list " + onlyPostgres));
	}

	private static String elements(Map<String, Long> counts, int sign) {
		return truncate(counts.entrySet().stream()
				.filter(entry -> Long.signum(entry.getValue()) == sign)
				.map(Map.Entry::getKey)
				.sorted()
				.collect(Collectors.joining(",", "[", "]")));
	}

	private static boolean equal(JsonNode broker, JsonNode postgres, boolean unordered) {
		if (broker == null || postgres == null) {
			return broker == postgres;
		}
		return canonical(broker, unordered).equals(canonical(postgres, unordered));
	}

	/**
	 * A text that is equal for equal values: object keys sorted, numbers by value, and with {@code unordered} lists
	 * sorted too.
	 */
	static String canonical(JsonNode node, boolean unordered) {
		if (node.isObject()) {
			return new TreeSet<>(node.propertyNames()).stream()
					.map(key -> StringNode.valueOf(key) + ":" + canonical(node.get(key), unordered))
					.collect(Collectors.joining(",", "{", "}"));
		}
		if (node.isArray()) {
			List<String> elements = new ArrayList<>();
			node.values().forEach(element -> elements.add(canonical(element, unordered)));
			if (unordered) {
				elements.sort(null);
			}
			return String.join(",", elements).transform(joined -> "[" + joined + "]");
		}
		if (node.isNumber()) {
			return node.decimalValue().stripTrailingZeros().toPlainString();
		}
		return node.toString();
	}

	private static String difference(String path, String broker, String postgres) {
		return (path.isEmpty() ? POINTER_SEPARATOR : path) + ": broker " + broker + ", postgresql " + postgres;
	}

	private static String escape(String key) {
		return key.replace("~", "~0").replace("/", "~1");
	}

	private static String render(JsonNode value) {
		return value == null ? ABSENT : truncate(value.toString());
	}

	private static String truncate(String text) {
		return text.length() > MAX_VALUE_LENGTH ? text.substring(0, MAX_VALUE_LENGTH) + TRUNCATED : text;
	}
}
