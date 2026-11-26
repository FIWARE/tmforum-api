package org.fiware.tmforum.product;

import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.querying.ParsedQuery;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.storage.query.AttributePath;
import org.fiware.tmforum.storage.query.ComparisonOperator;
import org.fiware.tmforum.storage.query.QueryFilter.Comparison;
import org.fiware.tmforum.storage.query.SortKey;
import org.fiware.tmforum.storage.query.ValueType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Resolves {@code DATE_TIME} on a real domain class, whose getters carry Lombok {@code onMethod} annotations,
 * both at the top level and below the plain {@code TimePeriod}.
 */
class ProductOfferingQueryParsingTest {

	private static final String INSTANT_LITERAL = "2024-01-01T10:00:00Z";

	private final QueryParser queryParser = new QueryParser(new GeneralProperties());

	@ParameterizedTest(name = "{0}")
	@MethodSource("dateTimePaths")
	void testFilterResolvesDateTime(String tmForumPath, List<String> segments) {
		ParsedQuery parsed = queryParser.parse(ProductOffering.class, tmForumPath + ".gt=" + INSTANT_LITERAL);

		assertEquals(new Comparison(new AttributePath(segments, true), ComparisonOperator.GREATER_THAN,
				ValueType.DATE_TIME, List.of(INSTANT_LITERAL)), parsed.filter());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("dateTimePaths")
	void testSortResolvesDateTime(String tmForumPath, List<String> segments) {
		List<SortKey> sort = queryParser.parseSort(ProductOffering.class,
				Map.of(QueryParser.SORT_KEY, List.of(tmForumPath)));

		assertEquals(List.of(new SortKey(new AttributePath(segments, true), ValueType.DATE_TIME, false)), sort);
	}

	private static Stream<Arguments> dateTimePaths() {
		return Stream.of(
				Arguments.of("lastUpdate", List.of("lastUpdate")),
				Arguments.of("validFor.startDateTime", List.of("validFor", "startDateTime")));
	}
}
