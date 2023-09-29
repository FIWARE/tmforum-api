package org.fiware.tmforum.common.querying;

import org.fiware.tmforum.common.notification.EventConstants;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SubscriptionQueryParserTest {
    @ParameterizedTest
    @MethodSource("queries")
    public void testQueryParsing(String queryString, List<String> defaultEventGroups,
                                 SubscriptionQuery expectedSubscriptionQuery) {
        assertEquals(expectedSubscriptionQuery, SubscriptionQueryParser.parse(queryString, defaultEventGroups),
                "The subscription query should have been properly parsed.");
    }

    private static Stream<Arguments> queries() {
        return Stream.of(
                Arguments.of("eventType=ProductCreateEvent", List.of(),
                        SubscriptionQueryBuilder.build().eventTypes(List.of("ProductCreateEvent"))
                                .eventGroups(Set.of("Product"))),
                Arguments.of("eventType=ProductCreateEvent;eventType=ProductDeleteEvent", List.of(),
                        SubscriptionQueryBuilder.build()
                                .eventTypes(List.of("ProductCreateEvent", "ProductDeleteEvent"))
                                .eventGroups(Set.of("Product"))),
                Arguments.of("eventType=ProductCreateEvent;eventType=CatalogCreateEvent", List.of(),
                        SubscriptionQueryBuilder.build()
                                .eventTypes(List.of("ProductCreateEvent", "CatalogCreateEvent"))
                                .eventGroups(Set.of("Product", "Catalog"))),
                Arguments.of("eventType=ProductCreateEvent&event.product.name=Some", List.of(),
                        SubscriptionQueryBuilder.build()
                                .eventTypes(List.of("ProductCreateEvent")).query("product.name=Some")
                                .eventGroups(Set.of("Product"))),
                Arguments.of("eventType=ProductCreateEvent&event.product.name=Some&fields=event.product.id,event.product.name", List.of(),
                        SubscriptionQueryBuilder.build()
                                .eventTypes(List.of("ProductCreateEvent")).query("product.name=Some")
                                .fields(List.of("product.id", "product.name")).eventGroups(Set.of("Product"))),
                Arguments.of("", List.of(EventConstants.EVENT_GROUP_CATEGORY, EventConstants.EVENT_GROUP_CATALOG),
                        SubscriptionQueryBuilder.build()
                                .eventTypes(Stream.of(EventConstants.EVENT_GROUP_CATEGORY, EventConstants.EVENT_GROUP_CATALOG)
                                        .flatMap(eventGroup ->
                                        EventConstants.ALLOWED_EVENT_TYPES.get(eventGroup).stream().map(
                                                eventType -> eventGroup + eventType)).toList())
                                .eventGroups(Set.of(EventConstants.EVENT_GROUP_CATEGORY, EventConstants.EVENT_GROUP_CATALOG)))
        );
    }

    private static class SubscriptionQueryBuilder {
        public static SubscriptionQuery build() {
            return new SubscriptionQuery();
        }
    }
}
