package org.fiware.tmforum.common.querying;

import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

	/**
	 * Mixed AND/OR is safe without any grouping/parenthesization logic on our side: NGSI-LD's q=
	 * already defines AND-before-OR precedence for an un-parenthesized term chain (ETSI GS CIM
	 * 009 §4.9), confirmed against a real broker's SQL translation (see conversation notes).
	 * {@code !attribute} mirrors NGSI-LD's own not-exists syntax directly (no TMForum-side
	 * translation layer).
	 */
	@ParameterizedTest
	@MethodSource("mixedAndOrAndNotExistsQueries")
	public void testMixedAndOrAndNotExists(String tmForumQuery, QueryParams ngsiLdQuery, Class<?> targetClass) {
		GeneralProperties properties = new GeneralProperties();
		properties.setEncloseQuery(true);
		properties.setNgsildOrQueryKey("|");
		properties.setNgsildOrQueryValue("|");
		properties.setIncludeAttributeInList(true);
		properties.setUseDotSeperator(false);

		QueryParser qp = new QueryParser(properties);
		assertEquals(ngsiLdQuery, qp.toNgsiLdQuery(targetClass, tmForumQuery),
				"Mixed AND/OR and !attribute queries should have been properly translated.");
	}

	private static Stream<Arguments> mixedAndOrAndNotExistsQueries() {
		return Stream.of(
				// Mixed AND+OR: AND between "status" and the "color" OR-run - no guard, no parens needed.
				Arguments.of("status=Active&color=Red;color=Blue",
						new QueryParams(null, null, "status==\"Active\";(color==\"Red\"|color==\"Blue\")"), MyPojo.class),
				// Shaped like the motivating relatedParty example: an exact-datasetId-match OR'd
				// against an AND-chained fallback (role match + not-exists on datasetId).
				Arguments.of("relatedParty.datasetId=X;relatedParty.role=Owner&!relatedParty.datasetId",
						new QueryParams(null, null, "relatedParty[role]==\"Owner\"|relatedParty[datasetId]==\"X\";!relatedParty[datasetId]"), MyPojo.class),
				// !attribute alone.
				Arguments.of("!status", new QueryParams(null, null, "!status"), MyPojo.class),
				// !attribute combined with AND.
				Arguments.of("!status&color=Red", new QueryParams(null, null, "!status;color==\"Red\""), MyPojo.class),
				// !attribute combined with OR - must not be merged into color's value list by combineOrRun.
				Arguments.of("!status;color=Red", new QueryParams(null, null, "color==\"Red\"|!status"), MyPojo.class),
				// !attribute on a mapped relationship (rel is @AttributeGetter(RELATIONSHIP)) - isRelationship/getQueryPart unaffected.
				Arguments.of("!rel.name", new QueryParams(null, null, "!rel.name"), MyPojo.class),
				// !attribute on an unmapped ("additional attributes" fallback) path whose last segment is
				// a reserved word - ReservedWordHandler escaping still applies with the ! prefix stripped.
				Arguments.of("!relatedParty.id", new QueryParams(null, null, "!relatedParty[tmfEscaped-id]"), MyPojo.class)
		);
	}

	/**
	 * {@code !type} is rejected by the exact same check as {@code !id} in
	 * {@code translateQueryPart} - not covered by its own case here because none of this test
	 * class's fixtures resolve a bare {@code type} path to NGSI-LD's native type shortcut (the
	 * existing "type=" TMForum shortcut is not exercised by any fixture in this file even before
	 * this change; only the {@code @type} → {@code atType} JSON-LD-reserved-token route is).
	 */
	@Test
	public void testNotExistsOnIdIsRejected() {
		GeneralProperties properties = new GeneralProperties();
		properties.setEncloseQuery(true);
		properties.setNgsildOrQueryKey("|");
		properties.setNgsildOrQueryValue("|");
		properties.setIncludeAttributeInList(true);
		properties.setUseDotSeperator(false);

		QueryParser qp = new QueryParser(properties);
		org.junit.jupiter.api.Assertions.assertThrows(
				org.fiware.tmforum.common.exception.QueryException.class,
				() -> qp.toNgsiLdQuery(MyPojo.class, "!id"),
				"!id must be rejected, id always exists.");
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
}