package org.fiware.tmforum.common.querying;

import org.fiware.tmforum.common.exception.QueryException;
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
                        SubscriptionQueryBuilder.build()
                                .eventTypes(List.of("ProductCreateEvent"))
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

    /**
     * Mixed AND/OR no longer throws, and !attribute passes through the event-prefix stripping
     * correctly (a leading "!" used to stop "event." from being recognised - see
     * removeEventPrefixFromAttributePath).
     */
    @ParameterizedTest
    @MethodSource("mixedAndOrAndNotExistsQueries")
    public void testMixedAndOrAndNotExists(String queryString, List<String> defaultEventGroups,
                                           SubscriptionQuery expectedSubscriptionQuery) {
        assertEquals(expectedSubscriptionQuery, SubscriptionQueryParser.parse(queryString, defaultEventGroups),
                "The subscription query should have been properly parsed.");
    }

    private static Stream<Arguments> mixedAndOrAndNotExistsQueries() {
        return Stream.of(
                // AND between eventType and the query, OR within the query - narrowed
                // checkLogicalOperator only forbids OR directly between eventType and query.
                Arguments.of("eventType=ProductCreateEvent&event.product.name=Some;event.product.color=Red", List.of(),
                        SubscriptionQueryBuilder.build()
                                .eventTypes(List.of("ProductCreateEvent")).query("product.name=Some;product.color=Red")
                                .eventGroups(Set.of("Product"))),
                // !attribute survives the "event." prefix stripping.
                Arguments.of("eventType=ProductCreateEvent&!event.product.name", List.of(),
                        SubscriptionQueryBuilder.build()
                                .eventTypes(List.of("ProductCreateEvent")).query("!product.name")
                                .eventGroups(Set.of("Product"))),
                // OR + fields no longer throws - that rule had no standards basis or functional
                // coupling (fields only affects payload projection, orthogonal to match logic).
                Arguments.of("event.product.name=Some;fields=event.product.id", List.of(),
                        SubscriptionQueryBuilder.build()
                                .query("product.name=Some").fields(List.of("product.id")))
        );
    }

    /**
     * Both remaining checkLogicalOperator rules still fire, but only when the conflicting tokens
     * are directly connected to each other - not merely "this operator appears somewhere".
     */
    @ParameterizedTest
    @MethodSource("stillRejectedQueries")
    public void testNarrowedRulesStillReject(String queryString, String expectedMessage) {
        QueryException exception = org.junit.jupiter.api.Assertions.assertThrows(QueryException.class,
                () -> SubscriptionQueryParser.parse(queryString, List.of()));
        assertEquals(expectedMessage, exception.getMessage());
    }

    private static Stream<Arguments> stillRejectedQueries() {
        return Stream.of(
                // eventType directly OR-connected to a query token.
                Arguments.of("eventType=ProductCreateEvent;event.product.name=Some",
                        "Logical operator OR(;) cannot be used when both 'eventType' and 'query' are defined"),
                // two eventTypes directly AND-connected to each other.
                Arguments.of("eventType=ProductCreateEvent&eventType=ProductDeleteEvent",
                        "Logical operator AND(&) cannot be used when several 'eventType' are defined")
        );
    }

    private static class SubscriptionQueryBuilder {
        public static SubscriptionQuery build() {
            return new SubscriptionQuery();
        }
    }
}
