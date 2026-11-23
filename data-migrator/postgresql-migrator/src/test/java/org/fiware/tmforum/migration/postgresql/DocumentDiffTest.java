package org.fiware.tmforum.migration.postgresql;

import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.migration.postgresql.DocumentDiff.Difference;
import org.fiware.tmforum.migration.postgresql.DocumentDiff.Kind;
import org.fiware.tmforum.migration.postgresql.DocumentDiff.Rules;
import org.fiware.tmforum.product.ProductOffering;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentDiffTest {

	private static final JsonMapper MAPPER = JsonMapper.builder().build();
	private static final Rules RULES = new Rules(true, false, Map.of("productSpecification", Set.of("name", "version"),
			"category", Set.of("name")));
	private static final Rules HUB_RULES = new Rules(false, true, Map.of());
	private static final Rules UNVERSIONED_RULES = new Rules(false, false, Map.of());

	static Stream<Arguments> differences() {
		return Stream.of(
				Arguments.of("equal documents", RULES,
						"{\"id\":\"a\",\"name\":\"n\"}", "{\"id\":\"a\",\"name\":\"n\"}", List.of()),
				Arguments.of("backfilled single reference field", RULES,
						"{\"productSpecification\":{\"id\":\"s\"}}",
						"{\"productSpecification\":{\"id\":\"s\",\"name\":\"spec\"}}",
						List.of(new Expected(Kind.BACKFILLED_REFERENCE_FIELD, "productSpecification.name"))),
				Arguments.of("backfilled list element field", RULES,
						"{\"category\":[{\"id\":\"c1\",\"name\":\"kept\"},{\"id\":\"c2\"}]}",
						"{\"category\":[{\"id\":\"c1\",\"name\":\"kept\"},{\"id\":\"c2\",\"name\":\"filled\"}]}",
						List.of(new Expected(Kind.BACKFILLED_REFERENCE_FIELD, "category[1].name"))),
				Arguments.of("seeded version", RULES, "{\"name\":\"n\"}", "{\"name\":\"n\",\"version\":\"0.0.1\"}",
						List.of(new Expected(Kind.SEEDED_VERSION, "version"))),
				Arguments.of("seeded version over a blank one", RULES, "{\"version\":\"\"}", "{\"version\":\"0.0.1\"}",
						List.of(new Expected(Kind.SEEDED_VERSION, "version"))),
				Arguments.of("dropped hub subscription", HUB_RULES,
						"{\"query\":\"q\",\"subscription\":{\"id\":\"urn:s\"}}", "{\"query\":\"q\"}",
						List.of(new Expected(Kind.DROPPED_SUBSCRIPTION, "subscription"))),
				Arguments.of("changed field", RULES, "{\"name\":\"n\"}", "{\"name\":\"changed\"}",
						List.of(new Expected(Kind.MISMATCH, "name"))),
				Arguments.of("removed field", RULES, "{\"name\":\"n\",\"description\":\"d\"}", "{\"name\":\"n\"}",
						List.of(new Expected(Kind.MISMATCH, "description"))),
				Arguments.of("target-only field", RULES, "{\"name\":\"n\"}", "{\"name\":\"n\",\"extra\":1}",
						List.of(new Expected(Kind.MISMATCH, "extra"))),
				Arguments.of("replaced reference field", RULES,
						"{\"productSpecification\":{\"id\":\"s\",\"name\":\"client\"}}",
						"{\"productSpecification\":{\"id\":\"s\",\"name\":\"target\"}}",
						List.of(new Expected(Kind.MISMATCH, "productSpecification.name"))),
				Arguments.of("reference field not fillable", RULES,
						"{\"productSpecification\":{\"id\":\"s\"}}",
						"{\"productSpecification\":{\"id\":\"s\",\"href\":\"h\"}}",
						List.of(new Expected(Kind.MISMATCH, "productSpecification.href"))),
				Arguments.of("reference pointing elsewhere", RULES,
						"{\"productSpecification\":{\"id\":\"s\"}}",
						"{\"productSpecification\":{\"id\":\"other\"}}",
						List.of(new Expected(Kind.MISMATCH, "productSpecification.id"))),
				Arguments.of("list of a different length", RULES,
						"{\"category\":[{\"id\":\"c1\"},{\"id\":\"c2\"}]}", "{\"category\":[{\"id\":\"c1\"}]}",
						List.of(new Expected(Kind.MISMATCH, "category"))),
				Arguments.of("reordered list", RULES,
						"{\"category\":[{\"id\":\"c1\"},{\"id\":\"c2\"}]}",
						"{\"category\":[{\"id\":\"c2\"},{\"id\":\"c1\"}]}",
						List.of(new Expected(Kind.MISMATCH, "category[0].id"), new Expected(Kind.MISMATCH, "category[1].id"))),
				Arguments.of("dropped reference", RULES, "{\"productSpecification\":{\"id\":\"s\"}}", "{}",
						List.of(new Expected(Kind.MISMATCH, "productSpecification"))),
				Arguments.of("version changed", RULES, "{\"version\":\"1.0\"}", "{\"version\":\"0.0.1\"}",
						List.of(new Expected(Kind.MISMATCH, "version"))),
				Arguments.of("version seeded on an unversioned type", UNVERSIONED_RULES, "{}", "{\"version\":\"0.0.1\"}",
						List.of(new Expected(Kind.MISMATCH, "version"))),
				Arguments.of("subscription dropped on a non-hub", RULES, "{\"subscription\":{\"id\":\"urn:s\"}}", "{}",
						List.of(new Expected(Kind.MISMATCH, "subscription"))),
				Arguments.of("hub subscription changed", HUB_RULES, "{\"subscription\":{\"id\":\"urn:s\"}}",
						"{\"subscription\":{\"id\":\"urn:t\"}}", List.of(new Expected(Kind.MISMATCH, "subscription"))));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("differences")
	void classifiesDifferences(String description, Rules rules, String source, String target, List<Expected> expected) {
		List<Expected> actual = DocumentDiff.diff(object(source), object(target), rules).stream()
				.map(difference -> new Expected(difference.kind(), difference.path()))
				.toList();

		assertEquals(expected, actual, description);
	}

	@Test
	void truncatesLongValues() {
		String longValue = "x".repeat(DocumentDiff.MAX_VALUE_LENGTH * 2);

		Difference difference = DocumentDiff.diff(object("{\"name\":\"" + longValue + "\"}"), object("{}"), RULES)
				.getFirst();

		assertTrue(difference.detail().length() < longValue.length(), difference.detail());
	}

	@Test
	void rulesComeFromTheCatalogAndTheDomainClass() {
		ReferenceBackfillCatalog catalog = ReferenceBackfillCatalog.load();

		Rules offering = Rules.of(ProductOffering.TYPE_PRODUCT_OFFERING, ProductOffering.class, catalog);
		Rules hub = Rules.of(TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION, TMForumSubscription.class, catalog);

		assertTrue(offering.versioned());
		assertTrue(offering.fillableKeys().get("category").contains("name"), offering.toString());
		assertTrue(hub.hub());
		assertEquals(false, hub.versioned());
	}

	private static ObjectNode object(String json) {
		return (ObjectNode) MAPPER.readTree(json);
	}

	private record Expected(Kind kind, String path) {
	}
}
