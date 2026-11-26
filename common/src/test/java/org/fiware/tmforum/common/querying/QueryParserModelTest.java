package org.fiware.tmforum.common.querying;

import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.QueryException;
import org.fiware.tmforum.storage.query.AttributePath;
import org.fiware.tmforum.storage.query.ComparisonOperator;
import org.fiware.tmforum.storage.query.QueryFilter;
import org.fiware.tmforum.storage.query.QueryFilter.AllOf;
import org.fiware.tmforum.storage.query.QueryFilter.AnyOf;
import org.fiware.tmforum.storage.query.QueryFilter.Comparison;
import org.fiware.tmforum.storage.query.SortKey;
import org.fiware.tmforum.storage.query.ValueType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.fiware.tmforum.storage.query.ComparisonOperator.EQUALS;
import static org.fiware.tmforum.storage.query.ComparisonOperator.GREATER_THAN;
import static org.fiware.tmforum.storage.query.ComparisonOperator.GREATER_THAN_EQUALS;
import static org.fiware.tmforum.storage.query.ComparisonOperator.LESS_THAN;
import static org.fiware.tmforum.storage.query.ComparisonOperator.LESS_THAN_EQUALS;
import static org.fiware.tmforum.storage.query.ComparisonOperator.REGEX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pins the backend-neutral model the front half of {@link QueryParser} produces.
 */
class QueryParserModelTest {

	private static final String RELATIONSHIP_ID = "urn:ngsi-ld:rel:1";

	@ParameterizedTest(name = "{0}")
	@MethodSource("filterQueries")
	void testParse(String tmForumQuery, Class<?> queryClass, ParsedQuery expected) {
		QueryParser qp = new QueryParser(new GeneralProperties());
		assertEquals(expected, qp.parse(queryClass, tmForumQuery));
	}

	private static Stream<Arguments> filterQueries() {
		return Stream.of(
				Arguments.of("status=Active", MyPojo.class,
						filter(declared(EQUALS, ValueType.STRING, List.of("Active"), "status"))),
				Arguments.of("active=true", MyPojo.class,
						filter(declared(EQUALS, ValueType.BOOLEAN, List.of("true"), "active"))),
				// numbers are trimmed, as the brokers ignore the whitespace anyway
				Arguments.of("temperature.gte= 20 ", MyPojo.class,
						filter(declared(GREATER_THAN_EQUALS, ValueType.NUMBER, List.of("20"), "temperature"))),
				Arguments.of("status=Active&temperature>10", MyPojo.class,
						filter(new AllOf(List.of(
								declared(EQUALS, ValueType.STRING, List.of("Active"), "status"),
								declared(GREATER_THAN, ValueType.NUMBER, List.of("10"), "temperature"))))),
				// OR-grouping per attribute keeps the HashMap order the NGSI-LD output always had
				Arguments.of("status=Active,Started;color=Red", MyPojo.class,
						filter(new AnyOf(List.of(
								declared(EQUALS, ValueType.STRING, List.of("Red"), "color"),
								declared(EQUALS, ValueType.STRING, List.of("Active", "Started"), "status"))))),
				Arguments.of("status=Active;status=Started", MyPojo.class,
						filter(declared(EQUALS, ValueType.STRING, List.of("Active", "Started"), "status"))),
				// renamed attributes keep the TMForum name, not the NGSI-LD one (otherSub)
				Arguments.of("otherNamedSub.status=Started", MyPojo.class,
						filter(declared(EQUALS, ValueType.STRING, List.of("Started"), "otherNamedSub", "status"))),
				// JSON-LD reserved tokens
				Arguments.of("@type=A", MyEntityPojo.class,
						filter(declared(EQUALS, ValueType.STRING, List.of("A"), "atType"))),
				Arguments.of("@baseType=B", MyEntityPojo.class,
						filter(declared(EQUALS, ValueType.STRING, List.of("B"), "atBaseType"))),
				Arguments.of("@id=urn:a", MyEntityPojo.class, new ParsedQuery("urn:a", null, null)),
				Arguments.of("id=urn:a,urn:b&status=Active", MyPojo.class,
						new ParsedQuery("urn:a,urn:b", null,
								declared(EQUALS, ValueType.STRING, List.of("Active"), "status"))),
				// extension attributes are typed once, from the combined value
				Arguments.of("ext=1", MyPojo.class,
						filter(undeclared(EQUALS, ValueType.NUMBER, List.of("1"), "ext"))),
				Arguments.of("ext=true", MyPojo.class,
						filter(undeclared(EQUALS, ValueType.BOOLEAN, List.of("true"), "ext"))),
				Arguments.of("ext=1,2", MyPojo.class,
						filter(undeclared(EQUALS, ValueType.STRING, List.of("1", "2"), "ext"))),
				Arguments.of("ext=1;ext=2", MyPojo.class,
						filter(undeclared(EQUALS, ValueType.STRING, List.of("1", "2"), "ext"))),
				Arguments.of("ext=1,abc", MyPojo.class,
						filter(undeclared(EQUALS, ValueType.STRING, List.of("1", "abc"), "ext"))),
				Arguments.of("relatedParty.role=Owner", MyPojo.class,
						filter(undeclared(EQUALS, ValueType.STRING, List.of("Owner"), "relatedParty", "role"))),
				// a regex is one opaque pattern, OR-ed patterns included
				Arguments.of("color.regex=a,b", MyPojo.class,
						filter(declared(REGEX, ValueType.STRING, List.of("a,b"), "color"))),
				// two patterns on one attribute stay two comparisons: joined, the comma would be part of the pattern
				Arguments.of("color.regex=x;color.regex=y", MyPojo.class,
						filter(new AnyOf(List.of(
								declared(REGEX, ValueType.STRING, List.of("x"), "color"),
								declared(REGEX, ValueType.STRING, List.of("y"), "color"))))),
				// date-time fields
				Arguments.of("createdAt.gt=2024-01-01T00:00:00Z", MyPojo.class,
						filter(declared(GREATER_THAN, ValueType.DATE_TIME, List.of("2024-01-01T00:00:00Z"), "createdAt"))),
				Arguments.of("createdAt.lt=2024-01-01T00:00:00+01:00", MyPojo.class,
						filter(declared(LESS_THAN, ValueType.DATE_TIME, List.of("2024-01-01T00:00:00+01:00"),
								"createdAt"))),
				Arguments.of("createdAt.lte=2024-01-01T00:00:00", MyPojo.class,
						filter(declared(LESS_THAN_EQUALS, ValueType.DATE_TIME, List.of("2024-01-01T00:00:00"),
								"createdAt"))),
				Arguments.of("createdAt=2024-01-01,2024-01-02", MyPojo.class,
						filter(declared(EQUALS, ValueType.DATE_TIME, List.of("2024-01-01", "2024-01-02"), "createdAt"))),
				// one literal that is no date-time keeps the whole comparison a string, no 400
				Arguments.of("sub.since.gte=2024-01-01T00:00:00.500Z", MyPojo.class,
						filter(declared(GREATER_THAN_EQUALS, ValueType.DATE_TIME, List.of("2024-01-01T00:00:00.500Z"),
								"sub", "since"))),
				Arguments.of("otherNamedSub.since=2024-01-01", MyPojo.class,
						filter(declared(EQUALS, ValueType.DATE_TIME, List.of("2024-01-01"), "otherNamedSub", "since"))),
				Arguments.of("sub.history.lt=2024-01-01T00:00:00Z", MyPojo.class,
						filter(declared(LESS_THAN, ValueType.DATE_TIME, List.of("2024-01-01T00:00:00Z"),
								"sub", "history"))),
				Arguments.of("sub.milestones=2024-01-01", MyPojo.class,
						filter(declared(EQUALS, ValueType.DATE_TIME, List.of("2024-01-01"), "sub", "milestones"))),
				Arguments.of("sub.since=yesterday", MyPojo.class,
						filter(declared(EQUALS, ValueType.STRING, List.of("yesterday"), "sub", "since"))),
				Arguments.of("createdAt=2024-01-01,yesterday", MyPojo.class,
						filter(declared(EQUALS, ValueType.STRING, List.of("2024-01-01", "yesterday"), "createdAt"))),
				Arguments.of("createdAt.regex=2024.*", MyPojo.class,
						filter(declared(REGEX, ValueType.STRING, List.of("2024.*"), "createdAt"))),
				// a relationship without a sub-path points at the referenced id
				Arguments.of("rel=" + RELATIONSHIP_ID, MyPojo.class,
						filter(declared(EQUALS, ValueType.STRING, List.of(RELATIONSHIP_ID), "rel", "id"))),
				Arguments.of("relList=" + RELATIONSHIP_ID, MyPojo.class,
						filter(declared(EQUALS, ValueType.STRING, List.of(RELATIONSHIP_ID), "relList", "id"))),
				Arguments.of("rel.id=" + RELATIONSHIP_ID, MyPojo.class,
						filter(declared(EQUALS, ValueType.STRING, List.of(RELATIONSHIP_ID), "rel", "id"))),
				Arguments.of("rel.name=therel", MyPojo.class,
						filter(declared(EQUALS, ValueType.STRING, List.of("therel"), "rel", "name"))));
	}

	/**
	 * Values split only on TMForum's {@code ,}; the broker's {@code ngsildOrQueryValue} plays no part in parsing.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("orValueSeparatorQueries")
	void testParseSplitsOnlyOnTmForumOrValue(String tmForumQuery, ParsedQuery expected) {
		assertEquals(expected, new QueryParser(new GeneralProperties()).parse(MyPojo.class, tmForumQuery));
	}

	private static Stream<Arguments> orValueSeparatorQueries() {
		return Stream.of(
				Arguments.of("color=a|b", filter(declared(EQUALS, ValueType.STRING, List.of("a|b"), "color"))),
				Arguments.of("color=a|b,c", filter(declared(EQUALS, ValueType.STRING, List.of("a|b", "c"), "color"))),
				Arguments.of("color=a,b", filter(declared(EQUALS, ValueType.STRING, List.of("a", "b"), "color"))),
				// a regex is never split
				Arguments.of("color.regex=a|b", filter(declared(REGEX, ValueType.STRING, List.of("a|b"), "color"))));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("sortQueries")
	void testParseSort(String sort, Class<?> queryClass, List<SortKey> expected) {
		QueryParser qp = new QueryParser(new GeneralProperties());
		assertEquals(expected, qp.parseSort(queryClass, Map.of(QueryParser.SORT_KEY, List.of(sort))));
	}

	private static Stream<Arguments> sortQueries() {
		return Stream.of(
				Arguments.of("color", MyPojo.class,
						List.of(new SortKey(path(true, "color"), ValueType.STRING, false))),
				Arguments.of("-temperature,color", MyPojo.class, List.of(
						new SortKey(path(true, "temperature"), ValueType.NUMBER, true),
						new SortKey(path(true, "color"), ValueType.STRING, false))),
				Arguments.of("-createdAt", MyPojo.class,
						List.of(new SortKey(path(true, "createdAt"), ValueType.DATE_TIME, true))),
				Arguments.of("-sub.since", MyPojo.class,
						List.of(new SortKey(path(true, "sub", "since"), ValueType.DATE_TIME, true))),
				Arguments.of("otherNamedSub.status", MyPojo.class,
						List.of(new SortKey(path(true, "otherNamedSub", "status"), ValueType.STRING, false))),
				// sort paths are kept as given, a relationship gets no "id"
				Arguments.of("rel", MyPojo.class,
						List.of(new SortKey(path(true, "rel"), ValueType.STRING, false))),
				Arguments.of("-@type", MyEntityPojo.class,
						List.of(new SortKey(path(true, "atType"), ValueType.STRING, true))),
				Arguments.of("sub.milestones", MyPojo.class,
						List.of(new SortKey(path(true, "sub", "milestones"), ValueType.DATE_TIME, false))),
				// an undeclared sort key is untyped and keeps the names the client sent, as in a filter
				Arguments.of("unknown", MyPojo.class,
						List.of(new SortKey(path(false, "unknown"), null, false))),
				Arguments.of("-@schemaLocation", MyPojo.class,
						List.of(new SortKey(path(false, "@schemaLocation"), null, true))),
				Arguments.of("", MyPojo.class, List.of()));
	}

	@Test
	void testParseSortWithoutSort() {
		assertEquals(List.of(), new QueryParser(new GeneralProperties()).parseSort(MyPojo.class, Map.of()));
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"-", "sub..status"})
	void testParseSortRejectsBlankSegments(String sort) {
		QueryParser qp = new QueryParser(new GeneralProperties());
		assertThrows(QueryException.class,
				() -> qp.parseSort(MyPojo.class, Map.of(QueryParser.SORT_KEY, List.of(sort))));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidQueries")
	void testParseRejectsInvalidQueries(String tmForumQuery) {
		QueryParser qp = new QueryParser(new GeneralProperties());
		assertThrows(QueryException.class, () -> qp.parse(MyPojo.class, tmForumQuery));
	}

	private static Stream<Arguments> invalidQueries() {
		return Stream.of(
				Arguments.of("status=a;color=b&temperature=1"),
				Arguments.of("active.gt=true"),
				Arguments.of("active=yes"),
				Arguments.of("temperature=warm"),
				Arguments.of("temperature=1,warm"),
				Arguments.of("temperature.regex=5"),
				Arguments.of("active.regex=true"),
				// an extension in a Java-only number form is typed NUMBER and then fails the number literal check
				Arguments.of("ext=+5"),
				Arguments.of("ext=.5"),
				Arguments.of("ext=1."),
				Arguments.of("ext=NaN"),
				Arguments.of("ext=1d"),
				Arguments.of("status=,"),
				Arguments.of("sub..status=a"));
	}

	private static ParsedQuery filter(QueryFilter filter) {
		return new ParsedQuery(null, null, filter);
	}

	private static Comparison declared(ComparisonOperator operator, ValueType valueType, List<String> values,
									   String... segments) {
		return new Comparison(path(true, segments), operator, valueType, values);
	}

	private static Comparison undeclared(ComparisonOperator operator, ValueType valueType, List<String> values,
										 String... segments) {
		return new Comparison(path(false, segments), operator, valueType, values);
	}

	private static AttributePath path(boolean declared, String... segments) {
		return new AttributePath(List.of(segments), declared);
	}
}
