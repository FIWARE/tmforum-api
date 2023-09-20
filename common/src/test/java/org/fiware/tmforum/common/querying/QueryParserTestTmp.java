package org.fiware.tmforum.common.querying;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryParserTestTmp {

	@ParameterizedTest
	@MethodSource("queriesForNgsild")
	public void testNgsiLdQueryParsing(String tmForumQuery, String ngsiLdQuery, Class<?> targetClass) {
		assertEquals(ngsiLdQuery, QueryParser.toNgsiLdQuery(targetClass, tmForumQuery),
				"The query should have been properly translated.");
	}

	private static Stream<Arguments> queriesForNgsild() {
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
				Arguments.of("temperature.gte=20;temperature.lt=3", "temperature<3|temperature>=20", MyPojo.class),
				Arguments.of("createdAt=2023-01-01T00:00:00.000Z", "createdAt==\"2023-01-01T00:00:00.000Z\"", MyPojo.class)

		);
	}

	@ParameterizedTest
	@MethodSource("queriesForNotification")
	public void testNotificationQueryParsing(String tmForumQuery, SubscriptionQuery subscriptionQuery) {
		assertEquals(subscriptionQuery, QueryParserTmp.parseNotificationQuery(tmForumQuery, List.of()),
				"The query should have been properly translated.");
	}

	private static Stream<Arguments> queriesForNotification() {
		return Stream.of(
				Arguments.of("eventType=MyPojoCreateEvent",
						createNotificationQuery(List.of("MyPojoCreateEvent"), "", null)),
				Arguments.of("eventType=MyPojoDeleteEvent",
						createNotificationQuery(List.of("MyPojoDeleteEvent"), "", null)),
				Arguments.of("eventType=MyPojoCreateEvent&event.pojo.color=Red",
						createNotificationQuery(List.of("MyPojoCreateEvent"), "color=Red",
								null)),
				Arguments.of("eventType=MyPojoCreateEvent&event.pojo.color=Red" +
								"&fields=event.pojo.color,event.pojo.status",
						createNotificationQuery(List.of("MyPojoCreateEvent"), "color=Red",
								List.of("color", "status"))),
				Arguments.of("eventType=MyPojoCreateEvent&eventType=MyPojoDeleteEvent&event.pojo.color=Red",
						createNotificationQuery(List.of("MyPojoCreateEvent", "MyPojoDeleteEvent"), "color=Red",
								null)),
				Arguments.of("eventType=MyPojoCreateEvent" +
								"&fields=event.pojo.color,event.pojo.status",
						createNotificationQuery(List.of("MyPojoCreateEvent"), "",
								List.of("color", "status")))
		);
	}

	private static SubscriptionQuery createNotificationQuery(List<String> eventTypes, String query,
                                                             List<String> fields) {
		SubscriptionQuery subscriptionQuery = new SubscriptionQuery();
		if (eventTypes != null) {
			eventTypes.forEach(subscriptionQuery::addEventType);
		}
		subscriptionQuery.setQuery(query);
		subscriptionQuery.setFields(fields);
		subscriptionQuery.setEventGroupName("MyPojo");
		return subscriptionQuery;
	}
}