package org.fiware.tmforum.common.querying;

import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.QueryException;
import org.fiware.tmforum.storage.query.AttributePath;
import org.fiware.tmforum.storage.query.ComparisonOperator;
import org.fiware.tmforum.storage.query.EntityQuery;
import org.fiware.tmforum.storage.query.QueryFilter;
import org.fiware.tmforum.storage.query.QueryFilter.AllOf;
import org.fiware.tmforum.storage.query.QueryFilter.AnyOf;
import org.fiware.tmforum.storage.query.QueryFilter.Comparison;
import org.fiware.tmforum.storage.query.SortKey;
import org.fiware.tmforum.storage.query.ValueType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.fiware.tmforum.storage.query.ComparisonOperator.EQUALS;
import static org.fiware.tmforum.storage.query.ComparisonOperator.GREATER_THAN;
import static org.fiware.tmforum.storage.query.ComparisonOperator.REGEX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Renders hand-built models with every broker dialect {@link QueryParserTest} covers.
 */
class NgsiLdQueryRendererTest {

	private static final String PIPE = "|";
	private static final String COMMA = ",";
	private static final String RELATIONSHIP_ID = "urn:ngsi-ld:rel:1";
	private static final String ALL_SEPARATORS = "a,b;c|d&e";

	// Orion-LD style: enclosed lists, "|" as OR, attribute repeated per value
	private static final GeneralProperties ENCLOSED_ATTRIBUTE_IN_LIST = properties(true, PIPE, true, false);
	private static final GeneralProperties ENCLOSED = properties(true, PIPE, false, false);
	private static final GeneralProperties ENCLOSED_DOT = properties(true, PIPE, false, true);
	// Scorpio style: plain "," lists
	private static final GeneralProperties PLAIN = properties(false, COMMA, false, false);
	private static final GeneralProperties PLAIN_DOT = properties(false, COMMA, false, true);
	private static final GeneralProperties DEFAULTS = new GeneralProperties();
	// the shipped orion-ld and scorpio profiles: both brokers take "|" between whole comparisons
	private static final GeneralProperties BROKER_PROFILE = brokerProfile();

	@ParameterizedTest(name = "{0}")
	@MethodSource("filters")
	void testRenderFilter(String expected, GeneralProperties properties, Class<?> queryClass, QueryFilter filter) {
		NgsiLdQueryRenderer renderer = new NgsiLdQueryRenderer(properties);
		assertEquals(expected, renderer.renderFilter(new EntityQuery(queryClass, filter, List.of())));
	}

	private static Stream<Arguments> filters() {
		Comparison red = declared(EQUALS, ValueType.STRING, List.of("Red"), "color");
		Comparison activeOrStarted = declared(EQUALS, ValueType.STRING, List.of("Active", "Started"), "status");
		return Stream.of(
				Arguments.of("color==\"Red\"|(status==\"Active\"|status==\"Started\")", ENCLOSED_ATTRIBUTE_IN_LIST,
						MyPojo.class, new AnyOf(List.of(red, activeOrStarted))),
				Arguments.of("(status==\"Active\"|status==\"Started\");color==\"Red\"", ENCLOSED_ATTRIBUTE_IN_LIST,
						MyPojo.class, new AllOf(List.of(activeOrStarted, red))),
				Arguments.of("color==\"Red\"|(status==\"Active\"|status==\"Started\")", BROKER_PROFILE,
						MyPojo.class, new AnyOf(List.of(red, activeOrStarted))),
				Arguments.of("(temperature==1|temperature==2)", BROKER_PROFILE, MyPojo.class,
						declared(EQUALS, ValueType.NUMBER, List.of("1", "2"), "temperature")),
				Arguments.of("color==\"Red\"|status==(\"Active\"|\"Started\")", ENCLOSED,
						MyPojo.class, new AnyOf(List.of(red, activeOrStarted))),
				Arguments.of("status==\"Active\",\"Started\";color==\"Red\"", PLAIN,
						MyPojo.class, new AllOf(List.of(activeOrStarted, red))),
				Arguments.of("color==\"Red\";(status==\"Active\",status==\"Started\")",
						PLAIN, MyPojo.class, new AllOf(List.of(red, new AnyOf(List.of(
								declared(EQUALS, ValueType.STRING, List.of("Active"), "status"),
								declared(EQUALS, ValueType.STRING, List.of("Started"), "status")))))),
				// paths: renamed attributes, structured properties, reserved names, relationships
				Arguments.of("sub[status]==\"Active\"", ENCLOSED, MyPojo.class,
						declared(EQUALS, ValueType.STRING, List.of("Active"), "sub", "status")),
				Arguments.of("otherSub[status]==\"Started\"", ENCLOSED, MyPojo.class,
						declared(EQUALS, ValueType.STRING, List.of("Started"), "otherNamedSub", "status")),
				Arguments.of("otherSub.status==\"Started\"", ENCLOSED_DOT, MyPojo.class,
						declared(EQUALS, ValueType.STRING, List.of("Started"), "otherNamedSub", "status")),
				Arguments.of("sub[tmfEscaped-valueType]==\"string\"", PLAIN, MyPojo.class,
						declared(EQUALS, ValueType.STRING, List.of("string"), "sub", "valueType")),
				Arguments.of("sub.tmfEscaped-valueType==\"string\"", PLAIN_DOT, MyPojo.class,
						declared(EQUALS, ValueType.STRING, List.of("string"), "sub", "valueType")),
				Arguments.of("sub[tmfEscaped-id]==\"abc\"", PLAIN, MyPojo.class,
						declared(EQUALS, ValueType.STRING, List.of("abc"), "sub", "id")),
				Arguments.of("rel.tmfEscaped-valueType==\"string\"", PLAIN, MyPojo.class,
						declared(EQUALS, ValueType.STRING, List.of("string"), "rel", "valueType")),
				Arguments.of("rel.name==\"therel\"", ENCLOSED_ATTRIBUTE_IN_LIST, MyPojo.class,
						declared(EQUALS, ValueType.STRING, List.of("therel"), "rel", "name")),
				Arguments.of("rel==\"" + RELATIONSHIP_ID + "\"", PLAIN, MyPojo.class,
						declared(EQUALS, ValueType.STRING, List.of(RELATIONSHIP_ID), "rel", "id")),
				Arguments.of("relList==\"" + RELATIONSHIP_ID + "\"", PLAIN, MyPojo.class,
						declared(EQUALS, ValueType.STRING, List.of(RELATIONSHIP_ID), "relList", "id")),
				Arguments.of("atType==\"A\"", ENCLOSED_ATTRIBUTE_IN_LIST, MyEntityPojo.class,
						declared(EQUALS, ValueType.STRING, List.of("A"), "atType")),
				Arguments.of("relatedParty.role==\"Owner\"", ENCLOSED_DOT, MyPojo.class,
						undeclared(EQUALS, ValueType.STRING, List.of("Owner"), "relatedParty", "role")),
				// values: only strings and date-times are quoted; createdAt is reserved in NGSI-LD
				Arguments.of("temperature>10", DEFAULTS, MyPojo.class,
						declared(GREATER_THAN, ValueType.NUMBER, List.of("10"), "temperature")),
				Arguments.of("active==true", DEFAULTS, MyPojo.class,
						declared(EQUALS, ValueType.BOOLEAN, List.of("true"), "active")),
				Arguments.of("ext==1", DEFAULTS, MyPojo.class,
						undeclared(EQUALS, ValueType.NUMBER, List.of("1"), "ext")),
				Arguments.of("tmfEscaped-createdAt>\"2024-01-01T00:00:00Z\"", DEFAULTS, MyPojo.class,
						declared(GREATER_THAN, ValueType.DATE_TIME, List.of("2024-01-01T00:00:00Z"), "createdAt")),
				Arguments.of("color~=bl.e", DEFAULTS, MyPojo.class,
						declared(REGEX, ValueType.STRING, List.of("bl.e"), "color")),
				// a model value is one exact literal, whatever separators it contains
				Arguments.of("color==\"" + ALL_SEPARATORS + "\"", DEFAULTS, MyPojo.class,
						declared(EQUALS, ValueType.STRING, List.of(ALL_SEPARATORS), "color")),
				Arguments.of("color==\"" + ALL_SEPARATORS + "\"", ENCLOSED_ATTRIBUTE_IN_LIST, MyPojo.class,
						declared(EQUALS, ValueType.STRING, List.of(ALL_SEPARATORS), "color")),
				// "&" and "[]" are no q syntax, so they pass through unquoted
				Arguments.of("color~=[ab]&c", ENCLOSED, MyPojo.class,
						declared(REGEX, ValueType.STRING, List.of("[ab]&c"), "color")),
				// a space and "=" are no q syntax inside a pattern either
				Arguments.of("color~=o w=x", DEFAULTS, MyPojo.class,
						declared(REGEX, ValueType.STRING, List.of("o w=x"), "color")),
				// deliberate change: the not-equals idiom keeps its value as one literal instead of splitting it
				Arguments.of("color!=\"a,b\"", DEFAULTS, MyPojo.class,
						declared(REGEX, ValueType.STRING, List.of("^(?!a,b$).*$"), "color")),
				Arguments.of("color!=\"a|b\"", ENCLOSED_ATTRIBUTE_IN_LIST, MyPojo.class,
						declared(REGEX, ValueType.STRING, List.of("^(?!a|b$).*$"), "color")),
				Arguments.of(null, DEFAULTS, MyPojo.class, null));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("regexesHoldingQuerySyntax")
	void testRegexHoldingQuerySyntaxIsRejected(String pattern, GeneralProperties properties) {
		NgsiLdQueryRenderer renderer = new NgsiLdQueryRenderer(properties);
		EntityQuery query = new EntityQuery(MyPojo.class,
				declared(REGEX, ValueType.STRING, List.of(pattern), "color"), List.of());
		assertThrows(QueryException.class, () -> renderer.renderFilter(query));
	}

	private static Stream<Arguments> regexesHoldingQuerySyntax() {
		return Stream.of(
				Arguments.of("alpha|beta", BROKER_PROFILE),
				Arguments.of("a(bet)", BROKER_PROFILE),
				Arguments.of("a,b", DEFAULTS),
				Arguments.of("a;b", DEFAULTS),
				Arguments.of("y \"h", BROKER_PROFILE),
				Arguments.of("it's", BROKER_PROFILE),
				Arguments.of("a<b", DEFAULTS),
				Arguments.of("a>b", DEFAULTS),
				Arguments.of("a!b", DEFAULTS),
				Arguments.of(ALL_SEPARATORS, ENCLOSED),
				Arguments.of("^(?!a$).*$", withoutRegexOptimization()));
	}

	/**
	 * A value split on a non-default {@code ngsildOrQueryValue} is rendered like any multi-value list now.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("orValueSeparatorQueries")
	void testOrValueSeparatorInsideValue(String expected, GeneralProperties properties) {
		QueryFilter filter = new QueryParser(properties).parse(MyPojo.class, "color=a|b").filter();
		assertEquals(expected, new NgsiLdQueryRenderer(properties).renderFilter(new EntityQuery(MyPojo.class, filter, List.of())));
	}

	private static Stream<Arguments> orValueSeparatorQueries() {
		return Stream.of(
				Arguments.of("(color==\"a\"|color==\"b\")", ENCLOSED_ATTRIBUTE_IN_LIST),
				Arguments.of("color==(\"a\"|\"b\")", ENCLOSED));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("sorts")
	void testRenderOrderBy(String expected, GeneralProperties properties, List<SortKey> sort) {
		NgsiLdQueryRenderer renderer = new NgsiLdQueryRenderer(properties);
		assertEquals(expected, renderer.renderOrderBy(new EntityQuery(MyPojo.class, null, sort)));
	}

	private static Stream<Arguments> sorts() {
		return Stream.of(
				Arguments.of("color,temperature;desc", DEFAULTS, List.of(
						new SortKey(path(true, "color"), ValueType.STRING, false),
						new SortKey(path(true, "temperature"), ValueType.NUMBER, true))),
				Arguments.of("otherSub.status", DEFAULTS,
						List.of(new SortKey(path(true, "otherNamedSub", "status"), ValueType.STRING, false))),
				Arguments.of("sub[tmfEscaped-valueType]", PLAIN,
						List.of(new SortKey(path(true, "sub", "valueType"), ValueType.STRING, false))),
				Arguments.of("rel.tmfEscaped-valueType;desc", DEFAULTS,
						List.of(new SortKey(path(true, "rel", "valueType"), ValueType.STRING, true))),
				Arguments.of("tmfEscaped-createdAt;desc", DEFAULTS,
						List.of(new SortKey(path(true, "createdAt"), ValueType.DATE_TIME, true))),
				Arguments.of("unknown", DEFAULTS, List.of(new SortKey(path(false, "unknown"), null, false))),
				// the orderBy keeps addressing an undeclared key by its translated name
				Arguments.of("atSchemaLocation;desc", DEFAULTS,
						List.of(new SortKey(path(false, "@schemaLocation"), null, true))),
				Arguments.of(null, DEFAULTS, List.of()));
	}

	private static GeneralProperties properties(boolean encloseQuery, String orSeparator,
												boolean includeAttributeInList, boolean useDotSeparator) {
		GeneralProperties properties = new GeneralProperties();
		properties.setEncloseQuery(encloseQuery);
		properties.setNgsildOrQueryKey(orSeparator);
		properties.setNgsildOrQueryValue(orSeparator);
		properties.setIncludeAttributeInList(includeAttributeInList);
		properties.setUseDotSeperator(useDotSeparator);
		return properties;
	}

	private static GeneralProperties brokerProfile() {
		GeneralProperties properties = properties(true, COMMA, true, true);
		properties.setNgsildOrQueryKey(PIPE);
		return properties;
	}

	private static GeneralProperties withoutRegexOptimization() {
		GeneralProperties properties = new GeneralProperties();
		properties.setOptimizeRegexNotEquals(false);
		return properties;
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
