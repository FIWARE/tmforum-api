package org.fiware.tmforum.common.querying;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class QueryResolverTest {
    private final QueryResolver queryResolver = new QueryResolver();

    @ParameterizedTest
    @MethodSource("queriesForCreationEvent")
    public void testCreationEvent(MyPojo myPojo, String tmForumQuery, boolean expectedResult) {
        assertEquals(expectedResult, queryResolver.doesQueryMatchCreateEvent(tmForumQuery, myPojo, ""),
                "The query should have been properly translated.");
    }

    private static Stream<Arguments> queriesForCreationEvent() {
        return Stream.of(
                Arguments.of(MyPojoBuilder.build(), "", true),
                Arguments.of(MyPojoBuilder.build(), "nonExistingField=10", false),
                Arguments.of(MyPojoBuilder.build().color("Red"), "color=Red", true),
                Arguments.of(MyPojoBuilder.build().color("Red"), "color=Black", false),
                Arguments.of(MyPojoBuilder.build().temperature(3), "temperature>5", false),
                Arguments.of(MyPojoBuilder.build().temperature(3), "temperature<5&temperature>2", true),
                Arguments.of(MyPojoBuilder.build().temperature(3), "temperature>4;temperature<2", false),
                Arguments.of(MyPojoBuilder.build().temperature(7), "temperature>=7", true),
                Arguments.of(MyPojoBuilder.build().color("Red").temperature(7), "color=Red&temperature>6", true),
                Arguments.of(MyPojoBuilder.build().color("Red").temperature(5), "color=Red&temperature>6", false),
                Arguments.of(MyPojoBuilder.build().createdAt(Instant.parse("2023-05-01T00:00:00.000Z")),
                        "createdAt>2023-04-01T00:00:00.000Z", true),
                Arguments.of(MyPojoBuilder.build().createdAt(Instant.parse("2023-05-01T00:00:00.000Z")),
                        "createdAt<=2023-06-01T00:00:00.000Z", true),
                Arguments.of(MyPojoBuilder.build().createdAt(Instant.parse("2023-05-01T00:00:00.000Z")),
                        "createdAt=2023-05-01T00:00:00.000Z", true)
        );
    }

    @ParameterizedTest
    @MethodSource("queriesForUpdateEvent")
    public void testUpdateEvent(MyPojo oldState, MyPojo newState, String tmForumQuery, boolean expectedResult) {
        assertEquals(expectedResult, queryResolver.doesQueryMatchUpdateEvent(tmForumQuery, newState, oldState, ""),
                "The query should have been properly translated.");
    }

    private static Stream<Arguments> queriesForUpdateEvent() {
        return Stream.of(
            Arguments.of(MyPojoBuilder.build().color("Red"), MyPojoBuilder.build().color("Blue"), "color=Blue", true),
            Arguments.of(MyPojoBuilder.build().color("Red"), MyPojoBuilder.build().color("Blue"), "color=Black", false),
            Arguments.of(MyPojoBuilder.build().color("Blue"), MyPojoBuilder.build().color("Red"), "color=Blue", false)
        );
    }

    private static class MyPojoBuilder {
        public static MyPojo build() {
            return new MyPojo("id");
        }
    }
}
