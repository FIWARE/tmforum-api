package org.fiware.tmforum.common.querying;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryParserTest {

	@ParameterizedTest
	@MethodSource("queries")
	public void testQueryParsing(String tmForumQuery, String ngsiLdQuery, Class<?> targetClass) {
		assertEquals(ngsiLdQuery, QueryParser.toNgsiLdQuery(targetClass, tmForumQuery),
				"The query should have been properly translated.");
	}

	private static Stream<Arguments> queries() {
		return Stream.of(
				Arguments.of("status=Active,Started&color=Red", "status==(\"Active\"|\"Started\");color==\"Red\"", MyPojo.class),
				Arguments.of("status=Active,Started;color=Red", "color==\"Red\"|status==(\"Active\"|\"Started\")", MyPojo.class),
				Arguments.of("status=Active;status=Started", "status==(\"Active\"|\"Started\")", MyPojo.class),
				Arguments.of("status=Active;status=Started;color=Red", "color==\"Red\"|status==(\"Active\"|\"Started\")",
						MyPojo.class),
				Arguments.of("sub.status=Active;status=Started;color=Red",
						"color==\"Red\"|sub.status==\"Active\"|status==\"Started\"", MyPojo.class),
				Arguments.of("sub.status=Active;otherNamedSub.status=Started;color=Red",
						"color==\"Red\"|otherSub.status==\"Started\"|sub.status==\"Active\"", MyPojo.class),
				Arguments.of("temperature<20&temperature>10", "temperature<20;temperature>10", MyPojo.class),
				Arguments.of("temperature<=20;temperature=30", "temperature==30|temperature<=20", MyPojo.class),
				Arguments.of("temperature>=20;temperature<3", "temperature<3|temperature>=20", MyPojo.class),
				Arguments.of("status.eq=Active,Started&color.eq=Red", "status==(\"Active\"|\"Started\");color==\"Red\"",
						MyPojo.class),
				Arguments.of("status.eq=Active,Started;color.eq=Red", "color==\"Red\"|status==(\"Active\"|\"Started\")",
						MyPojo.class),
				Arguments.of("status.eq=Active;status.eq=Started", "status==(\"Active\"|\"Started\")", MyPojo.class),
				Arguments.of("status.eq=Active;status.eq=Started;color.eq=Red", "color==\"Red\"|status==(\"Active\"|\"Started\")",
						MyPojo.class),
				Arguments.of("sub.status.eq=Active;status.eq=Started;color.eq=Red",
						"color==\"Red\"|sub.status==\"Active\"|status==\"Started\"", MyPojo.class),
				Arguments.of("sub.status.eq=Active;otherNamedSub.status.eq=Started;color.eq=Red",
						"color==\"Red\"|otherSub.status==\"Started\"|sub.status==\"Active\"", MyPojo.class),
				Arguments.of("temperature.lt=20&temperature.gt=10", "temperature<20;temperature>10", MyPojo.class),
				Arguments.of("temperature.lte=20;temperature.eq=30", "temperature==30|temperature<=20", MyPojo.class),
				Arguments.of("temperature.gte=20;temperature.lt=3", "temperature<3|temperature>=20", MyPojo.class)

		);
	}
}