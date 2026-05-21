package org.fiware.tmforum.common.querying;

import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}