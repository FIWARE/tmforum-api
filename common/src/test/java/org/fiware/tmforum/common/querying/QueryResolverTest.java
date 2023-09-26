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
    public void testCreationEvent(MyPojo myPojo, String tmForumQuery, String payloadName, boolean expectedResult) {
        assertEquals(expectedResult, queryResolver.doesQueryMatchCreateEvent(tmForumQuery, myPojo, payloadName),
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
        assertEquals(expectedResult, queryResolver.doesQueryMatchUpdateEvent(tmForumQuery, newState, oldState, payloadName),
                "The query should have been properly translated.");
    }

    private static Stream<Arguments> queriesForUpdateEvent() {
        return Stream.of(
            Arguments.of(MyPojoBuilder.build().color("Red"), MyPojoBuilder.build().color("Blue"), "myPojo.color=Blue", "myPojo", true),
            Arguments.of(MyPojoBuilder.build().color("Red"), MyPojoBuilder.build().color("Blue"), "myPojo.color=Black", "myPojo", false),
            Arguments.of(MyPojoBuilder.build().color("Blue"), MyPojoBuilder.build().color("Red"), "myPojo.color=Blue", "myPojo", false)
        );
    }

    private static class MyPojoBuilder {
        public static MyPojo build() {
            return new MyPojo("id");
        }
    }
}
