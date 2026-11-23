package org.fiware.tmforum.migration.postgresql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseDiffTest {

	private static final JsonMapper MAPPER = JsonMapper.builder().build();
	private static final ResponseDiff.Rules ORDERED = new ResponseDiff.Rules(false, List.of(), Set.of());
	private static final ResponseDiff.Rules UNORDERED = new ResponseDiff.Rules(true, List.of(), Set.of());
	private static final String EXTERNAL_HOST = "partner.example.org";

	static Stream<Arguments> bodies() {
		return Stream.of(
				Arguments.of("equal", ORDERED, "{\"id\":\"a\",\"n\":1}", "{\"id\":\"a\",\"n\":1}", List.of()),
				Arguments.of("reordered keys", ORDERED, "{\"id\":\"a\",\"n\":1}", "{\"n\":1,\"id\":\"a\"}", List.of()),
				Arguments.of("equal numbers in another notation", ORDERED, "{\"n\":1.0}", "{\"n\":1}", List.of()),
				Arguments.of("reordered list on a set-like endpoint", UNORDERED,
						"[{\"id\":\"a\",\"c\":[1,2]},{\"id\":\"b\"}]", "[{\"id\":\"b\"},{\"id\":\"a\",\"c\":[2,1]}]",
						List.of()),
				Arguments.of("reordered list on an ordered endpoint", ORDERED, "[\"a\",\"b\"]", "[\"b\",\"a\"]",
						List.of("/0", "/1")),
				Arguments.of("element missing on a set-like endpoint", UNORDERED, "[\"a\",\"b\"]", "[\"b\"]",
						List.of("/")),
				Arguments.of("ignored pointer", new ResponseDiff.Rules(false, List.of("/lastUpdate"), Set.of()),
						"{\"id\":\"a\",\"lastUpdate\":\"1\"}", "{\"id\":\"a\",\"lastUpdate\":\"2\"}", List.of()),
				Arguments.of("ignored pointer with wildcards",
						new ResponseDiff.Rules(false, List.of("/*/relatedParty/**/name"), Set.of()),
						"[{\"relatedParty\":[{\"id\":\"p\",\"name\":\"x\"}]}]",
						"[{\"relatedParty\":[{\"id\":\"p\",\"name\":\"y\"}]}]", List.of()),
				Arguments.of("server host of href", ORDERED,
						"{\"href\":\"http://broker-api:8080/tmf-api/x/urn:1\"}",
						"{\"href\":\"http://postgres-api:8080/tmf-api/x/urn:1\"}", List.of()),
				Arguments.of("path of href", ORDERED,
						"{\"href\":\"http://broker-api:8080/tmf-api/x/urn:1\"}",
						"{\"href\":\"http://postgres-api:8080/tmf-api/x/urn:2\"}", List.of("/href")),
				Arguments.of("reference fields of an external target",
						new ResponseDiff.Rules(false, List.of(), Set.of(EXTERNAL_HOST)),
						"{\"category\":[{\"id\":\"c\",\"href\":\"https://partner.example.org/c\",\"name\":\"old\"}]}",
						"{\"category\":[{\"id\":\"c\",\"href\":\"https://partner.example.org/c\"}]}", List.of()),
				Arguments.of("reference fields of a local target", ORDERED,
						"{\"category\":[{\"id\":\"c\",\"href\":\"https://partner.example.org/c\",\"name\":\"old\"}]}",
						"{\"category\":[{\"id\":\"c\",\"href\":\"https://partner.example.org/c\"}]}",
						List.of("/category/0/name")),
				Arguments.of("changed value", ORDERED, "{\"name\":\"a\"}", "{\"name\":\"b\"}", List.of("/name")),
				Arguments.of("missing key", ORDERED, "{\"name\":\"a\",\"d\":\"x\"}", "{\"name\":\"a\"}",
						List.of("/d")),
				Arguments.of("key with a slash", ORDERED, "{\"a/b\":1}", "{\"a/b\":2}", List.of("/a~1b")));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("bodies")
	void diffsTheBodiesUnderTheRules(String description, ResponseDiff.Rules rules, String broker, String postgres,
			List<String> expectedPaths) {
		JsonNode brokerBody = MAPPER.readTree(broker);
		JsonNode postgresBody = MAPPER.readTree(postgres);

		List<String> differences = ResponseDiff.diff(brokerBody, postgresBody, rules);

		assertEquals(expectedPaths, differences.stream().map(difference -> difference.substring(0,
				difference.indexOf(':'))).toList(), description + ": " + differences);
		assertEquals(MAPPER.readTree(broker), brokerBody, "The diff must not modify its input.");
	}

	@Test
	void rejectsAPointerWithoutALeadingSlash() {
		assertThrows(IllegalArgumentException.class, () -> ResponseDiff.parsePointer("name"));
	}

	@Test
	void namesTheElementsOnlyOneSideHolds() {
		String difference = ResponseDiff.diff(MAPPER.readTree("[\"a\",\"b\"]"), MAPPER.readTree("[\"b\",\"c\"]"),
				UNORDERED).getFirst();

		assertTrue(difference.contains("broker only in the list [\"a\"]"), difference);
		assertTrue(difference.contains("postgresql only in the list [\"c\"]"), difference);
	}
}
