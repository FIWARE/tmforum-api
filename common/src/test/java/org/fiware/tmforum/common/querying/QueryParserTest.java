package org.fiware.tmforum.common.querying;

import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.QueryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryParserTest {

	@ParameterizedTest
	@MethodSource("queriesAttributeIncluded")
	public void testQueryParsingAttributeIncluded(String tmForumQuery, QueryParams ngsiLdQuery, Class<?> targetClass) {
		GeneralProperties properties = new GeneralProperties();
		properties.setEncloseQuery(true);
		properties.setNgsildOrQueryKey("|");
		properties.setNgsildOrQueryValue("|");
		properties.setIncludeAttributeInList(true);
		properties.setUseDotSeperator(false);

		QueryParser qp = new QueryParser(properties);
		assertEquals(ngsiLdQuery, qp.toNgsiLdQuery(targetClass, tmForumQuery),
				"The query should have been properly translated.");
	}

	@ParameterizedTest
	@MethodSource("queriesAttributeNotIncluded")
	public void testQueryParsingAttributeNotIncluded(String tmForumQuery, QueryParams ngsiLdQuery, Class<?> targetClass) {
		GeneralProperties properties = new GeneralProperties();
		properties.setEncloseQuery(true);
		properties.setNgsildOrQueryKey("|");
		properties.setNgsildOrQueryValue("|");
		properties.setIncludeAttributeInList(false);
		properties.setUseDotSeperator(false);

		QueryParser qp = new QueryParser(properties);
		assertEquals(ngsiLdQuery, qp.toNgsiLdQuery(targetClass, tmForumQuery),
				"The query should have been properly translated.");
	}

	@ParameterizedTest
	@MethodSource("queriesAttributesWithDotPath")
	public void testQueryParsingWithDotPath(String tmForumQuery, QueryParams ngsiLdQuery, Class<?> targetClass) {
		GeneralProperties properties = new GeneralProperties();
		properties.setEncloseQuery(true);
		properties.setNgsildOrQueryKey("|");
		properties.setNgsildOrQueryValue("|");
		properties.setIncludeAttributeInList(false);
		properties.setUseDotSeperator(true);

		QueryParser qp = new QueryParser(properties);
		assertEquals(ngsiLdQuery, qp.toNgsiLdQuery(targetClass, tmForumQuery),
				"The query should have been properly translated.");
	}

	private static Stream<Arguments> queriesAttributeIncluded() {
		return Stream.of(
				// Property attributes queries
				Arguments.of("status=Active,Started&color=Red", new QueryParams(null, null, "(status==\"Active\"|status==\"Started\");color==\"Red\""), MyPojo.class),
				Arguments.of("status=Active,Started;color=Red", new QueryParams(null, null, "color==\"Red\"|(status==\"Active\"|status==\"Started\")"), MyPojo.class),
				Arguments.of("status=Active;status=Started", new QueryParams(null, null, "(status==\"Active\"|status==\"Started\")"), MyPojo.class),
				Arguments.of("status=Active;status=Started;color=Red", new QueryParams(null, null, "color==\"Red\"|(status==\"Active\"|status==\"Started\")"),
						MyPojo.class),
				Arguments.of("sub.status=Active;status=Started;color=Red",
						new QueryParams(null, null, "color==\"Red\"|sub[status]==\"Active\"|status==\"Started\""), MyPojo.class),
				Arguments.of("sub.status=Active;otherNamedSub.status=Started;color=Red",
						new QueryParams(null, null, "color==\"Red\"|otherSub[status]==\"Started\"|sub[status]==\"Active\""), MyPojo.class),
				Arguments.of("temperature<20&temperature>10", new QueryParams(null, null, "temperature<20;temperature>10"), MyPojo.class),
				Arguments.of("temperature<=20;temperature=30", new QueryParams(null, null, "temperature==30|temperature<=20"), MyPojo.class),
				Arguments.of("temperature>=20;temperature<3", new QueryParams(null, null, "temperature<3|temperature>=20"), MyPojo.class),
				Arguments.of("status.eq=Active,Started&color.eq=Red", new QueryParams(null, null, "(status==\"Active\"|status==\"Started\");color==\"Red\""),
						MyPojo.class),
				Arguments.of("status.eq=Active,Started;color.eq=Red", new QueryParams(null, null, "color==\"Red\"|(status==\"Active\"|status==\"Started\")"),
						MyPojo.class),
				Arguments.of("status.eq=Active;status.eq=Started", new QueryParams(null, null, "(status==\"Active\"|status==\"Started\")"), MyPojo.class),
				Arguments.of("status.eq=Active;status.eq=Started;color.eq=Red", new QueryParams(null, null, "color==\"Red\"|(status==\"Active\"|status==\"Started\")"),
						MyPojo.class),
				Arguments.of("sub.status.eq=Active;status.eq=Started;color.eq=Red",
						new QueryParams(null, null, "color==\"Red\"|sub[status]==\"Active\"|status==\"Started\""), MyPojo.class),
				Arguments.of("sub.status.eq=Active;otherNamedSub.status.eq=Started;color.eq=Red",
						new QueryParams(null, null, "color==\"Red\"|otherSub[status]==\"Started\"|sub[status]==\"Active\""), MyPojo.class),
				Arguments.of("temperature.lt=20&temperature.gt=10", new QueryParams(null, null, "temperature<20;temperature>10"), MyPojo.class),
				Arguments.of("temperature.lte=20;temperature.eq=30", new QueryParams(null, null, "temperature==30|temperature<=20"), MyPojo.class),
				Arguments.of("temperature.gte=20;temperature.lt=3", new QueryParams(null, null, "temperature<3|temperature>=20"), MyPojo.class),

				// Relationship attributes queries
				Arguments.of("rel.name=therel", new QueryParams(null, null, "rel.name==\"therel\""), MyPojo.class),
				Arguments.of("relList.name=therel", new QueryParams(null, null, "relList.name==\"therel\""), MyPojo.class),

				// Id queries
				Arguments.of("id=urn:ngsi-ld:service:c2016f17-997d-468a-be23-7657bc5b4c5b,urn:ngsi-ld:service:u2096f17-997d-468a-be23-7657bc5b4c67", new QueryParams("urn:ngsi-ld:service:c2016f17-997d-468a-be23-7657bc5b4c5b,urn:ngsi-ld:service:u2096f17-997d-468a-be23-7657bc5b4c67", null, null), MyPojo.class)
		);
	}

	private static Stream<Arguments> queriesAttributeNotIncluded() {
		return Stream.of(
				// Property attributes queries
				Arguments.of("status=Active,Started&color=Red", new QueryParams(null, null, "status==(\"Active\"|\"Started\");color==\"Red\""), MyPojo.class),
				Arguments.of("status=Active,Started;color=Red", new QueryParams(null, null, "color==\"Red\"|status==(\"Active\"|\"Started\")"), MyPojo.class),
				Arguments.of("status=Active;status=Started", new QueryParams(null, null, "status==(\"Active\"|\"Started\")"), MyPojo.class),
				Arguments.of("status=Active;status=Started;color=Red", new QueryParams(null, null, "color==\"Red\"|status==(\"Active\"|\"Started\")"),
						MyPojo.class),
				Arguments.of("sub.status=Active;status=Started;color=Red",
						new QueryParams(null, null, "color==\"Red\"|sub[status]==\"Active\"|status==\"Started\""), MyPojo.class),
				Arguments.of("sub.status=Active;otherNamedSub.status=Started;color=Red",
						new QueryParams(null, null, "color==\"Red\"|otherSub[status]==\"Started\"|sub[status]==\"Active\""), MyPojo.class),
				Arguments.of("temperature<20&temperature>10", new QueryParams(null, null, "temperature<20;temperature>10"), MyPojo.class),
				Arguments.of("temperature<=20;temperature=30", new QueryParams(null, null, "temperature==30|temperature<=20"), MyPojo.class),
				Arguments.of("temperature>=20;temperature<3", new QueryParams(null, null, "temperature<3|temperature>=20"), MyPojo.class),
				Arguments.of("status.eq=Active,Started&color.eq=Red", new QueryParams(null, null, "status==(\"Active\"|\"Started\");color==\"Red\""),
						MyPojo.class),
				Arguments.of("status.eq=Active,Started;color.eq=Red", new QueryParams(null, null, "color==\"Red\"|status==(\"Active\"|\"Started\")"),
						MyPojo.class),
				Arguments.of("status.eq=Active;status.eq=Started", new QueryParams(null, null, "status==(\"Active\"|\"Started\")"), MyPojo.class),
				Arguments.of("status.eq=Active;status.eq=Started;color.eq=Red", new QueryParams(null, null, "color==\"Red\"|status==(\"Active\"|\"Started\")"),
						MyPojo.class)
		);
	}


	private static Stream<Arguments> queriesAttributesWithDotPath() {
		return Stream.of(
				// Property attributes queries
				Arguments.of("sub.status=Active;status=Started;color=Red",
						new QueryParams(null, null, "color==\"Red\"|sub.status==\"Active\"|status==\"Started\""), MyPojo.class),
				Arguments.of("sub.status=Active;otherNamedSub.status=Started;color=Red",
						new QueryParams(null, null, "color==\"Red\"|otherSub.status==\"Started\"|sub.status==\"Active\""), MyPojo.class),
				Arguments.of("sub.status=Active;relatedParty.role=Owner",
						new QueryParams(null, null, "relatedParty.role==\"Owner\"|sub.status==\"Active\""), MyPojo.class)
		);
	}

	@ParameterizedTest
	@MethodSource("scorpioQueries")
	public void testScorpioQueryParsing(String tmForumQuery, QueryParams ngsiLdQuery, Class<?> targetClass) {
		GeneralProperties properties = new GeneralProperties();
		properties.setNgsildOrQueryKey(",");
		properties.setNgsildOrQueryValue(",");
		properties.setEncloseQuery(false);
		properties.setIncludeAttributeInList(false);
		properties.setUseDotSeperator(false);

		QueryParser qp = new QueryParser(properties);
		assertEquals(ngsiLdQuery, qp.toNgsiLdQuery(targetClass, tmForumQuery),
				"The query should have been properly translated.");
	}

	private static Stream<Arguments> scorpioQueries() {
		return Stream.of(
				Arguments.of("status=Active,Started&color=Red", new QueryParams(null, null, "status==\"Active\",\"Started\";color==\"Red\""), MyPojo.class),
				Arguments.of("status=Active;status=Started", new QueryParams(null, null, "status==\"Active\",\"Started\""), MyPojo.class),
				Arguments.of("sub.status=Active&status=Started&color=Red", new QueryParams(null, null, "sub[status]==\"Active\";status==\"Started\";color==\"Red\""), MyPojo.class),
				Arguments.of("temperature<20&temperature>10", new QueryParams(null, null, "temperature<20;temperature>10"), MyPojo.class),
				Arguments.of("status.eq=Active,Started&color.eq=Red", new QueryParams(null, null, "status==\"Active\",\"Started\";color==\"Red\""), MyPojo.class),
				Arguments.of("status.eq=Active;status.eq=Started", new QueryParams(null, null, "status==\"Active\",\"Started\""), MyPojo.class)
		);
	}

	/**
	 * Verifies that JSON-LD reserved keywords carried in TMF payloads
	 * ({@code @type}, {@code @baseType}, {@code @schemaLocation}, {@code @id})
	 * are rewritten to the persisted internal field names on
	 * {@link org.fiware.tmforum.common.domain.Entity} so they become
	 * filterable from the outside under their natural TMF JSON name.
	 */
	@ParameterizedTest
	@MethodSource("jsonLdReservedTokenQueries")
	public void testJsonLdReservedTokenTranslation(String tmForumQuery, QueryParams ngsiLdQuery, Class<?> targetClass) {
		GeneralProperties properties = new GeneralProperties();
		properties.setEncloseQuery(true);
		properties.setNgsildOrQueryKey("|");
		properties.setNgsildOrQueryValue("|");
		properties.setIncludeAttributeInList(true);
		properties.setUseDotSeperator(false);

		QueryParser qp = new QueryParser(properties);
		assertEquals(ngsiLdQuery, qp.toNgsiLdQuery(targetClass, tmForumQuery),
				"JSON-LD reserved token query should translate to the persisted attribute name.");
	}

	private static Stream<Arguments> jsonLdReservedTokenQueries() {
		return Stream.of(
				// @type → atType (q= filter; not the types collector — line 134 checks contains("type") on the resolved path, which becomes ["atType"]).
				Arguments.of("@type=BlueprintProductSpecification",
						new QueryParams(null, null, "atType==\"BlueprintProductSpecification\""),
						MyEntityPojo.class),
				// @baseType → atBaseType
				Arguments.of("@baseType=ProductSpecification",
						new QueryParams(null, null, "atBaseType==\"ProductSpecification\""),
						MyEntityPojo.class),
				// @id → id, routed via the line-130 shortcut to the ids collector.
				Arguments.of("@id=urn:ngsi-ld:product-specification:1",
						new QueryParams("urn:ngsi-ld:product-specification:1", null, null),
						MyEntityPojo.class),
				// OR grouping survives the translation (combineParts runs on the raw token before translation).
				Arguments.of("@type=A;@type=B",
						new QueryParams(null, null, "(atType==\"A\"|atType==\"B\")"),
						MyEntityPojo.class),
				// AND combination with a regular domain attribute.
				Arguments.of("@type=A&status=Active",
						new QueryParams(null, null, "atType==\"A\";status==\"Active\""),
						MyEntityPojo.class),
				// Mixed routing: @id goes to the ids collector, @type goes to the q= filter.
				Arguments.of("@id=urn:x&@type=A",
						new QueryParams("urn:x", null, "atType==\"A\""),
						MyEntityPojo.class)
		);
	}

	/**
	 * Verifies the translation of the TMForum {@code sort} query parameter (comma-separated
	 * properties, "-" prefix for descending) into NGSI-LD's {@code orderBy} syntax
	 * (comma-separated "property;direction" pairs, direction omitted meaning ascending).
	 */
	@ParameterizedTest
	@MethodSource("sortToOrderByQueries")
	public void testSortToOrderByTranslation(Map<String, List<String>> parameters, String expectedOrderBy,
			Class<?> targetClass) {
		GeneralProperties properties = new GeneralProperties();
		properties.setUseDotSeperator(true);

		QueryParser qp = new QueryParser(properties);
		assertEquals(expectedOrderBy, qp.toOrderBy(targetClass, parameters),
				"The sort parameter should have been properly translated to orderBy.");
	}

	private static Stream<Arguments> sortToOrderByQueries() {
		return Stream.of(
				// no sort requested at all
				Arguments.of(Map.of(), null, MyPojo.class),
				// single ascending field, no direction suffix needed
				Arguments.of(Map.of(QueryParser.SORT_KEY, List.of("color")), "color", MyPojo.class),
				// single descending field
				Arguments.of(Map.of(QueryParser.SORT_KEY, List.of("-color")), "color;desc", MyPojo.class),
				// mixed ascending/descending, comma-separated
				Arguments.of(Map.of(QueryParser.SORT_KEY, List.of("color,-temperature")), "color,temperature;desc", MyPojo.class),
				Arguments.of(Map.of(QueryParser.SORT_KEY, List.of("-color,-temperature")), "color;desc,temperature;desc", MyPojo.class),
				// nested attribute path
				Arguments.of(Map.of(QueryParser.SORT_KEY, List.of("-sub.status")), "sub.status;desc", MyPojo.class),
				// JSON-LD reserved token translation, same as filtering
				Arguments.of(Map.of(QueryParser.SORT_KEY, List.of("-@type")), "atType;desc", MyEntityPojo.class)
		);
	}

	@Test
	public void testSortToOrderByReturnsNullWhenSortValueIsBlank() {
		GeneralProperties properties = new GeneralProperties();
		QueryParser qp = new QueryParser(properties);
		assertNull(qp.toOrderBy(MyPojo.class, Map.of(QueryParser.SORT_KEY, List.of(""))),
				"A blank sort value should not produce an orderBy.");
	}

	/**
	 * Boolean and number values are literals in the NGSI-LD query language and are therefore not
	 * quoted. A value that is not a valid literal of the attribute's type would produce a query that
	 * the broker rejects, so it has to be rejected as an invalid query instead.
	 *
	 * @param message      description of the concrete case
	 * @param tmForumQuery the query to translate
	 */
	@ParameterizedTest
	@MethodSource("invalidTypedValueQueries")
	public void testInvalidTypedValuesAreRejected(String message, String tmForumQuery) {
		QueryParser qp = new QueryParser(new GeneralProperties());
		assertThrows(QueryException.class, () -> qp.toNgsiLdQuery(MyPojo.class, tmForumQuery), message);
	}

	private static Stream<Arguments> invalidTypedValueQueries() {
		return Stream.of(
				Arguments.of("A non-numeric value for a number attribute should be rejected.", "temperature=my-non-int"),
				Arguments.of("An empty value for a number attribute should be rejected.", "temperature="),
				// Double.parseDouble accepts all of those, the query language does not
				Arguments.of("NaN should be rejected for a number attribute.", "temperature=NaN"),
				Arguments.of("Infinity should be rejected for a number attribute.", "temperature=Infinity"),
				Arguments.of("A java type suffix should be rejected for a number attribute.", "temperature=1d"),
				Arguments.of("A hexadecimal float should be rejected for a number attribute.", "temperature=0x1p1"),
				Arguments.of("A non-boolean value for a boolean attribute should be rejected.", "active=my-non-bool"),
				Arguments.of("An upper-case boolean should be rejected, the brokers only accept lower-case.",
						"active=True"),
				Arguments.of("An empty value for a boolean attribute should be rejected.", "active="),
				// the value list is split before the values are encoded, so every entry is checked
				Arguments.of("An invalid value inside a list should be rejected.", "temperature=1,my-non-int"),
				Arguments.of("An invalid value inside a boolean list should be rejected.", "active=true,my-non-bool"),
				// the same check applies to the relational operators
				Arguments.of("An invalid value should also be rejected for a range query.",
						"temperature.gt=my-non-int"));
	}

	/**
	 * Booleans have no order to compare them by, so the ordering operators cannot be applied to them.
	 * Forwarding such a query is not an option - Orion-LD 1.9.0 terminates on it.
	 *
	 * @param message      description of the concrete case
	 * @param tmForumQuery the query to translate
	 */
	@ParameterizedTest
	@MethodSource("orderedBooleanQueries")
	public void testOrderingOperatorsAreRejectedForBooleans(String message, String tmForumQuery) {
		QueryParser qp = new QueryParser(new GeneralProperties());
		assertThrows(QueryException.class, () -> qp.toNgsiLdQuery(MyPojo.class, tmForumQuery), message);
	}

	private static Stream<Arguments> orderedBooleanQueries() {
		return Stream.of(
				Arguments.of("A boolean cannot be greater than another one.", "active.gt=true"),
				Arguments.of("A boolean cannot be greater than or equal to another one.", "active.gte=true"),
				Arguments.of("A boolean cannot be less than another one.", "active.lt=true"),
				Arguments.of("A boolean cannot be less than or equal to another one.", "active.lte=true"),
				// the textual operators have symbolic counterparts, both end up as the same query part
				Arguments.of("The symbolic operators should be rejected as well.", "active>true"),
				Arguments.of("The symbolic operators should be rejected as well.", "active<=true"));
	}

	/**
	 * The rejection has to stay limited to the booleans - ordering numbers and strings is well defined
	 * and supported by the brokers.
	 *
	 * @param message      description of the concrete case
	 * @param tmForumQuery the query to translate
	 * @param ngsiLdQuery  the expected translation
	 */
	@ParameterizedTest
	@MethodSource("orderedNonBooleanQueries")
	public void testOrderingOperatorsAreKeptForOrderedTypes(String message, String tmForumQuery,
			QueryParams ngsiLdQuery) {
		QueryParser qp = new QueryParser(new GeneralProperties());
		assertEquals(ngsiLdQuery, qp.toNgsiLdQuery(MyPojo.class, tmForumQuery), message);
	}

	private static Stream<Arguments> orderedNonBooleanQueries() {
		return Stream.of(
				Arguments.of("Numbers should stay comparable by order.", "temperature.gt=20",
						new QueryParams(null, null, "temperature>20")),
				Arguments.of("Numbers should stay comparable by order.", "temperature.lte=20",
						new QueryParams(null, null, "temperature<=20")),
				Arguments.of("Strings should stay comparable by order.", "color.gt=blue",
						new QueryParams(null, null, "color>\"blue\"")),
				Arguments.of("Booleans should stay comparable by equality.", "active=true",
						new QueryParams(null, null, "active==true")));
	}

	/**
	 * The rejection has to stay limited to the invalid values - everything the query language accepts
	 * as a literal still has to be translated.
	 *
	 * @param message      description of the concrete case
	 * @param tmForumQuery the query to translate
	 * @param ngsiLdQuery  the expected translation
	 */
	@ParameterizedTest
	@MethodSource("validTypedValueQueries")
	public void testValidTypedValuesAreTranslated(String message, String tmForumQuery, QueryParams ngsiLdQuery) {
		QueryParser qp = new QueryParser(new GeneralProperties());
		assertEquals(ngsiLdQuery, qp.toNgsiLdQuery(MyPojo.class, tmForumQuery), message);
	}

	private static Stream<Arguments> validTypedValueQueries() {
		return Stream.of(
				Arguments.of("An integer should be accepted.", "temperature=20",
						new QueryParams(null, null, "temperature==20")),
				Arguments.of("A negative number should be accepted.", "temperature=-20",
						new QueryParams(null, null, "temperature==-20")),
				Arguments.of("A decimal number should be accepted.", "temperature=20.5",
						new QueryParams(null, null, "temperature==20.5")),
				Arguments.of("A number in exponential notation should be accepted.", "temperature=2e3",
						new QueryParams(null, null, "temperature==2e3")),
				Arguments.of("Both boolean literals should be accepted.", "active=true",
						new QueryParams(null, null, "active==true")),
				Arguments.of("Both boolean literals should be accepted.", "active=false",
						new QueryParams(null, null, "active==false")),
				// the brokers ignore it anyway, so it is cleaned up instead of being rejected
				Arguments.of("Surrounding whitespace should be tolerated.", "temperature= 20 ",
						new QueryParams(null, null, "temperature==20")),
				// attributes that are unknown to the queried class are typed by their value, so they
				// can never conflict with the declared type and stay a plain string
				Arguments.of("An unknown attribute should not be type-checked.", "unknownAttribute=my-non-int",
						new QueryParams(null, null, "unknownAttribute==\"my-non-int\"")));
	}
}