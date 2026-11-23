package org.fiware.tmforum.migration.postgresql;

import org.fiware.tmforum.migration.postgresql.ReferenceFillCases.Case;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReferenceFillerTest {

	private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

	static Stream<Case> cases() {
		return ReferenceFillCases.cases();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("cases")
	void fillsAbsentFieldsOnly(Case testCase) {
		JsonNode reference = JSON_MAPPER.readTree(testCase.reference());
		JsonNode original = reference.deepCopy();
		JsonNode target = testCase.target() == null ? null : JSON_MAPPER.readTree(testCase.target());

		JsonNode filled = ReferenceFiller.fill(reference, target, ReferenceFillCases.FIELDS);

		assertEquals(JSON_MAPPER.readTree(testCase.expected()), filled);
		assertEquals(original, reference, "The reference itself must stay unchanged.");
		assertEquals(filled, ReferenceFiller.fill(filled, target, ReferenceFillCases.FIELDS), "A re-fill is a no-op.");
	}
}
