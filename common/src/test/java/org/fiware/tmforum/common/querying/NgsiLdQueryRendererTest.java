package org.fiware.tmforum.common.querying;

import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.configuration.NgsiLdBrokerProperties;
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
import org.junit.jupiter.api.Test;
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
	private static final NgsiLdBrokerProperties ENCLOSED_ATTRIBUTE_IN_LIST = properties(true, PIPE, true, false);
	private static final NgsiLdBrokerProperties ENCLOSED = properties(true, PIPE, false, false);
	private static final NgsiLdBrokerProperties ENCLOSED_DOT = properties(true, PIPE, false, true);
	// Scorpio style: plain "," lists
	private static final NgsiLdBrokerProperties PLAIN = properties(false, COMMA, false, false);
	private static final NgsiLdBrokerProperties PLAIN_DOT = properties(false, COMMA, false, true);
	private static final NgsiLdBrokerProperties DEFAULTS = new NgsiLdBrokerProperties();
	private static final GeneralProperties GENERAL_DEFAULTS = new GeneralProperties();
	// the shipped orion-ld and scorpio profiles: both brokers take "|" between whole comparisons
	private static final NgsiLdBrokerProperties BROKER_PROFILE = brokerProfile();

	@ParameterizedTest(name = "{0}")
	@MethodSource("filters")
	void testRenderFilter(String expected, NgsiLdBrokerProperties properties, Class<?> queryClass, QueryFilter filter) {
		NgsiLdQueryRenderer renderer = new NgsiLdQueryRenderer(GENERAL_DEFAULTS, properties);
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
				// Orion-LD lexes a date-like term as a DateTime (and crashes on an invalid one); "\-" keeps it a RegExp
				Arguments.of("color~=2024\\-01", DEFAULTS, MyPojo.class,
						declared(REGEX, ValueType.STRING, List.of("2024-01"), "color")),
				Arguments.of("color~=2024\\-01-01T10\\:00\\:00Z", DEFAULTS, MyPojo.class,
						declared(REGEX, ValueType.STRING, List.of("2024-01-01T10:00:00Z"), "color")),
				Arguments.of("color~=\\-12", DEFAULTS, MyPojo.class,
						declared(REGEX, ValueType.STRING, List.of("-12"), "color")),
				Arguments.of("color~=2.0", DEFAULTS, MyPojo.class,
						declared(REGEX, ValueType.STRING, List.of("2.0"), "color")),
				// Scorpio matches nothing for a ":" between word characters; "\:" matches on both brokers
				Arguments.of("color~=25\\:61\\:00", DEFAULTS, MyPojo.class,
						declared(REGEX, ValueType.STRING, List.of("25:61:00"), "color")),
				Arguments.of("color~=T10\\:00", DEFAULTS, MyPojo.class,
						declared(REGEX, ValueType.STRING, List.of("T10:00"), "color")),
				Arguments.of("color~=b_\\:c", DEFAULTS, MyPojo.class,
						declared(REGEX, ValueType.STRING, List.of("b_:c"), "color")),
				Arguments.of("color~=:a: a\\:b \\d:\\d [[:digit:]]", DEFAULTS, MyPojo.class,
						declared(REGEX, ValueType.STRING, List.of(":a: a\\:b \\d:\\d [[:digit:]]"), "color")),
				Arguments.of("color~=x2024-01", DEFAULTS, MyPojo.class,
						declared(REGEX, ValueType.STRING, List.of("x2024-01"), "color")),
				Arguments.of("color~=[0-9]{4}-01", DEFAULTS, MyPojo.class,
						declared(REGEX, ValueType.STRING, List.of("[0-9]{4}-01"), "color")),
				// deliberate change: the not-equals idiom keeps its value as one literal instead of splitting it
				Arguments.of("color!=\"a,b\"", DEFAULTS, MyPojo.class,
						declared(REGEX, ValueType.STRING, List.of("^(?!a,b$).*$"), "color")),
				Arguments.of("color!=\"a|b\"", ENCLOSED_ATTRIBUTE_IN_LIST, MyPojo.class,
						declared(REGEX, ValueType.STRING, List.of("^(?!a|b$).*$"), "color")),
				Arguments.of(null, DEFAULTS, MyPojo.class, null));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("regexesHoldingQuerySyntax")
	void testRegexHoldingQuerySyntaxIsRejected(String pattern, NgsiLdBrokerProperties properties,
			GeneralProperties generalProperties) {
		NgsiLdQueryRenderer renderer = new NgsiLdQueryRenderer(generalProperties, properties);
		EntityQuery query = new EntityQuery(MyPojo.class,
				declared(REGEX, ValueType.STRING, List.of(pattern), "color"), List.of());
		assertThrows(QueryException.class, () -> renderer.renderFilter(query));
	}

	private static Stream<Arguments> regexesHoldingQuerySyntax() {
		return Stream.of(
				Arguments.of("alpha|beta", BROKER_PROFILE, GENERAL_DEFAULTS),
				Arguments.of("a(bet)", BROKER_PROFILE, GENERAL_DEFAULTS),
				Arguments.of("a,b", DEFAULTS, GENERAL_DEFAULTS),
				Arguments.of("a;b", DEFAULTS, GENERAL_DEFAULTS),
				Arguments.of("y \"h", BROKER_PROFILE, GENERAL_DEFAULTS),
				Arguments.of("it's", BROKER_PROFILE, GENERAL_DEFAULTS),
				Arguments.of("a<b", DEFAULTS, GENERAL_DEFAULTS),
				Arguments.of("a>b", DEFAULTS, GENERAL_DEFAULTS),
				Arguments.of("a!b", DEFAULTS, GENERAL_DEFAULTS),
				Arguments.of(ALL_SEPARATORS, ENCLOSED, GENERAL_DEFAULTS),
				Arguments.of("^(?!a$).*$", DEFAULTS, withoutRegexOptimization()));
	}

	/**
	 * A string value is wrapped in quotes without an escape NGSI-LD's q honours, so a value holding one would close
	 * the string and add a condition: {@code name=a"|description=="x} rendered as an OR with an extra comparison,
	 * and the broker answered a query nobody asked for. Such a value is rejected, as the regex path rejects its own
	 * syntax characters.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("valuesHoldingQuerySyntax")
	void testStringValueHoldingQuerySyntaxIsRejected(String value, ValueType valueType) {
		NgsiLdQueryRenderer renderer = new NgsiLdQueryRenderer(GENERAL_DEFAULTS, DEFAULTS);
		EntityQuery query = new EntityQuery(MyPojo.class,
				declared(EQUALS, valueType, List.of(value), "color"), List.of());
		assertThrows(QueryException.class, () -> renderer.renderFilter(query));
	}

	private static Stream<Arguments> valuesHoldingQuerySyntax() {
		return Stream.of(
				Arguments.of("a\"", ValueType.STRING),
				Arguments.of("a\"|description==\"x", ValueType.STRING),
				// a DATE_TIME literal is validated by the model, so only a string can carry these
				Arguments.of("a\\\\b", ValueType.STRING));
	}

	/**
	 * The not-equals idiom takes its value out of the pattern and renders it as a string, so the same characters
	 * have to be rejected there.
	 */
	@Test
	void testNotEqualsValueHoldingQuerySyntaxIsRejected() {
		NgsiLdQueryRenderer renderer = new NgsiLdQueryRenderer(GENERAL_DEFAULTS, DEFAULTS);
		EntityQuery query = new EntityQuery(MyPojo.class,
				declared(REGEX, ValueType.STRING, List.of("^(?!a\"b$).*$"), "color"), List.of());
		assertThrows(QueryException.class, () -> renderer.renderFilter(query));
	}

	/**
	 * The parser splits only on TMForum's {@code ,}: a {@code |} stays inside the one literal, and two values are
	 * joined with the configured {@code ngsildOrQueryValue}.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("orValueSeparatorQueries")
	void testOrValueSeparatorInsideValue(String expected, NgsiLdBrokerProperties properties, String tmForumQuery) {
		QueryFilter filter = new QueryParser(GENERAL_DEFAULTS).parse(MyPojo.class, tmForumQuery).filter();
		assertEquals(expected, new NgsiLdQueryRenderer(GENERAL_DEFAULTS, properties)
				.renderFilter(new EntityQuery(MyPojo.class, filter, List.of())));
	}

	private static Stream<Arguments> orValueSeparatorQueries() {
		return Stream.of(
				Arguments.of("color==\"a|b\"", ENCLOSED_ATTRIBUTE_IN_LIST, "color=a|b"),
				Arguments.of("color==\"a|b\"", ENCLOSED, "color=a|b"),
				Arguments.of("(color==\"a\"|color==\"b\")", ENCLOSED_ATTRIBUTE_IN_LIST, "color=a,b"),
				Arguments.of("color==(\"a\"|\"b\")", ENCLOSED, "color=a,b"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("sorts")
	void testRenderOrderBy(String expected, NgsiLdBrokerProperties properties, List<SortKey> sort) {
		NgsiLdQueryRenderer renderer = new NgsiLdQueryRenderer(GENERAL_DEFAULTS, properties);
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

	private static NgsiLdBrokerProperties properties(boolean encloseQuery, String orSeparator,
												boolean includeAttributeInList, boolean useDotSeparator) {
		NgsiLdBrokerProperties properties = new NgsiLdBrokerProperties();
		properties.setEncloseQuery(encloseQuery);
		properties.setNgsildOrQueryKey(orSeparator);
		properties.setNgsildOrQueryValue(orSeparator);
		properties.setIncludeAttributeInList(includeAttributeInList);
		properties.setUseDotSeperator(useDotSeparator);
		return properties;
	}

	private static NgsiLdBrokerProperties brokerProfile() {
		NgsiLdBrokerProperties properties = properties(true, COMMA, true, true);
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
