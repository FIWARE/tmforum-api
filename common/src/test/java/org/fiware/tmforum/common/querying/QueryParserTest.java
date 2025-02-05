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

		QueryParser qp = new QueryParser(properties);
		assertEquals(ngsiLdQuery, qp.toNgsiLdQuery(targetClass, tmForumQuery),
				"The query should have been properly translated.");
	}

	@Test
	public void testAtSchema() {
		GeneralProperties properties = new GeneralProperties();
		properties.setEncloseQuery(true);
		properties.setNgsildOrQueryKey("|");
		properties.setNgsildOrQueryValue("|");
		properties.setIncludeAttributeInList(false);
		QueryParser qp = new QueryParser(properties);
		String query = "status=Active&color=Red&relatedParty.role=Owner";
		QueryParams queryParams = qp.toNgsiLdQuery(MyPojo.class, query);
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

	@ParameterizedTest
	@MethodSource("scorpioQueries")
	public void testScorpioQueryParsing(String tmForumQuery, QueryParams ngsiLdQuery, Class<?> targetClass) {
		GeneralProperties properties = new GeneralProperties();
		properties.setNgsildOrQueryKey(",");
		properties.setNgsildOrQueryValue(",");
		properties.setEncloseQuery(false);
		properties.setIncludeAttributeInList(false);

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
}