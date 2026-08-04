package org.fiware.tmforum.common.querying;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SubscriptionQueryResolverTest {
    private final SubscriptionQueryResolver subscriptionQueryResolver = new SubscriptionQueryResolver();

    @ParameterizedTest
    @MethodSource("queriesForCreationEvent")
    public void testCreationEvent(MyPojo myPojo, String tmForumQuery, String payloadName, boolean expectedResult) {
        assertEquals(expectedResult, subscriptionQueryResolver.doesQueryMatchCreateEvent(tmForumQuery, myPojo, payloadName),
                "The query should have been properly translated.");
    }

    private static Stream<Arguments> queriesForCreationEvent() {
        return Stream.of(
                Arguments.of(MyPojoBuilder.build(), "", "", true),
                Arguments.of(MyPojoBuilder.build(), "myPojo.nonExistingField=10", "myPojo", false),
                Arguments.of(MyPojoBuilder.build().color("Red"), "myPojo.color=Red", "myPojo", true),
                Arguments.of(MyPojoBuilder.build().color("Red"), "myPojo.color=Red;myOtherPojo.color=Blue", "myPojo", true),
                Arguments.of(MyPojoBuilder.build().color("Blue"), "myPojo.color=Red;myOtherPojo.color=Blue", "myPojo", false),
                Arguments.of(MyPojoBuilder.build().color("Red"), "myPojo.color=Black", "myPojo", false),
                Arguments.of(MyPojoBuilder.build().temperature(3), "myPojo.temperature>5", "myPojo", false),
                Arguments.of(MyPojoBuilder.build().temperature(3), "myPojo.temperature<5&myPojo.temperature>2", "myPojo", true),
                Arguments.of(MyPojoBuilder.build().temperature(3), "myPojo.temperature>4;myPojo.temperature<2", "myPojo", false),
                Arguments.of(MyPojoBuilder.build().temperature(7), "myPojo.temperature>=7", "myPojo", true),
                Arguments.of(MyPojoBuilder.build().color("Red").temperature(7), "myPojo.color=Red&myPojo.temperature>6", "myPojo", true),
                Arguments.of(MyPojoBuilder.build().color("Red").temperature(5), "myPojo.color=Red&myPojo.temperature>6", "myPojo", false),
                Arguments.of(MyPojoBuilder.build().createdAt(Instant.parse("2023-05-01T00:00:00.000Z")),
                        "myPojo.createdAt>2023-04-01T00:00:00.000Z", "myPojo", true),
                Arguments.of(MyPojoBuilder.build().createdAt(Instant.parse("2023-05-01T00:00:00.000Z")),
                        "myPojo.createdAt<=2023-06-01T00:00:00.000Z", "myPojo", true),
                Arguments.of(MyPojoBuilder.build().createdAt(Instant.parse("2023-05-01T00:00:00.000Z")),
                        "myPojo.createdAt=2023-05-01T00:00:00.000Z", "myPojo", true)
        );
    }

    @ParameterizedTest
    @MethodSource("queriesForUpdateEvent")
    public void testUpdateEvent(MyPojo oldState, MyPojo newState, String tmForumQuery, String payloadName, boolean expectedResult) {
        assertEquals(expectedResult, subscriptionQueryResolver.doesQueryMatchUpdateEvent(tmForumQuery, newState, oldState, payloadName),
                "The query should have been properly translated.");
    }

    private static Stream<Arguments> queriesForUpdateEvent() {
        return Stream.of(
            Arguments.of(MyPojoBuilder.build().color("Red"), MyPojoBuilder.build().color("Blue"), "myPojo.color=Blue", "myPojo", true),
            Arguments.of(MyPojoBuilder.build().color("Red"), MyPojoBuilder.build().color("Blue"), "myPojo.color=Black", "myPojo", false),
            Arguments.of(MyPojoBuilder.build().color("Blue"), MyPojoBuilder.build().color("Red"), "myPojo.color=Blue", "myPojo", false)
        );
    }

    /**
     * (color=Red AND temperature>10) OR status=Active - confirms the left-to-right AND-before-OR
     * fold in evaluateResult, not a plain allMatch/anyMatch over the whole query.
     */
    @ParameterizedTest
    @MethodSource("precedenceQueries")
    public void testAndBeforeOrPrecedence(MyPojo myPojo, boolean expectedResult) {
        String query = "myPojo.color=Red&myPojo.temperature>10;myPojo.status=Active";
        assertEquals(expectedResult, subscriptionQueryResolver.doesQueryMatchCreateEvent(query, myPojo, "myPojo"),
                "AND must bind tighter than OR when evaluating a mixed query.");
    }

    private static Stream<Arguments> precedenceQueries() {
        return Stream.of(
                // AND-branch satisfied (color=Red, temperature>10) -> true regardless of OR-branch.
                Arguments.of(MyPojoBuilder.build().color("Red").temperature(15).status("Inactive"), true),
                // AND-branch fails (temperature not >10) but OR-branch (status=Active) is satisfied.
                Arguments.of(MyPojoBuilder.build().color("Red").temperature(5).status("Active"), true),
                // Neither branch satisfied.
                Arguments.of(MyPojoBuilder.build().color("Blue").temperature(5).status("Inactive"), false)
        );
    }

    @ParameterizedTest
    @MethodSource("notExistsCreateQueries")
    public void testNotExistsOnCreateEvent(MyPojo myPojo, boolean expectedResult) {
        assertEquals(expectedResult, subscriptionQueryResolver.doesQueryMatchCreateEvent("!myPojo.color", myPojo, "myPojo"),
                "!attribute should match iff the field is absent.");
    }

    private static Stream<Arguments> notExistsCreateQueries() {
        return Stream.of(
                Arguments.of(MyPojoBuilder.build(), true),
                Arguments.of(MyPojoBuilder.build().color("Red"), false)
        );
    }

    /**
     * !attribute in an update event is evaluated as a pure state predicate against the new state
     * only - unlike a normal value-comparing QueryPart, it does NOT require the field to have
     * actually changed (see the old=null/new=null "no change" case below).
     */
    @ParameterizedTest
    @MethodSource("notExistsUpdateQueries")
    public void testNotExistsOnUpdateEvent(MyPojo oldState, MyPojo newState, boolean expectedResult) {
        assertEquals(expectedResult, subscriptionQueryResolver.doesQueryMatchUpdateEvent("!myPojo.color", newState, oldState, "myPojo"),
                "!attribute on update should match iff the field is absent in the new state.");
    }

    private static Stream<Arguments> notExistsUpdateQueries() {
        return Stream.of(
                Arguments.of(MyPojoBuilder.build().color("Red"), MyPojoBuilder.build(), true),
                Arguments.of(MyPojoBuilder.build(), MyPojoBuilder.build(), true),
                Arguments.of(MyPojoBuilder.build(), MyPojoBuilder.build().color("Red"), false)
        );
    }

    private static class MyPojoBuilder {
        public static MyPojo build() {
            return new MyPojo("id");
        }
    }
}
