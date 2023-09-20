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
        assertEquals(expectedResult, queryResolver.doesQueryMatchCreateEvent(myPojo, tmForumQuery),
                "The query should have been properly translated.");
    }

    private static Stream<Arguments> queriesForCreationEvent() {
        MyPojo cR = new MyPojo("id");
        cR.setColor("Red");

        MyPojo t3 = new MyPojo("id");
        t3.setTemperature(3);

        MyPojo t7 = new MyPojo("id");
        t7.setTemperature(7);

        MyPojo cRt7 = new MyPojo("id");
        cRt7.setColor("Red");
        cRt7.setTemperature(7);

        MyPojo cRt5 = new MyPojo("id");
        cRt5.setColor("Red");
        cRt5.setTemperature(5);

        MyPojo inst = new MyPojo("id");
        inst.setCreatedAt(Instant.parse("2023-05-01T00:00:00.000Z"));

        return Stream.of(
                Arguments.of(new MyPojo(""), "", true),
                Arguments.of(new MyPojo(""), "nonExistingField=10", false),
                Arguments.of(cR, "color=Red", true),
                Arguments.of(cR, "color=Black", false),
                Arguments.of(t3, "temperature>5", false),
                Arguments.of(t3, "temperature<5&temperature>2", true),
                Arguments.of(t3, "temperature>4;temperature<2", false),
                Arguments.of(t7, "temperature>=7", true),
                Arguments.of(cRt7, "color=Red&temperature>6", true),
                Arguments.of(cRt5, "color=Red&temperature>6", false),
                Arguments.of(inst, "createdAt>2023-04-01T00:00:00.000Z", true),
                Arguments.of(inst, "createdAt<=2023-06-01T00:00:00.000Z", true),
                Arguments.of(inst, "createdAt=2023-05-01T00:00:00.000Z", true)
        );
    }

    @ParameterizedTest
    @MethodSource("queriesForUpdateEvent")
    public void testUpdateEvent(MyPojo oldState, MyPojo newState, String tmForumQuery, boolean expectedResult) {
        assertEquals(expectedResult, queryResolver.doesQueryMatchUpdateEvent(newState, oldState, tmForumQuery),
                "The query should have been properly translated.");
    }

    private static Stream<Arguments> queriesForUpdateEvent() {
        MyPojo cR = new MyPojo("id");
        cR.setColor("Red");
        MyPojo cB = new MyPojo("id");
        cB.setColor("Blue");

        return Stream.of(
            Arguments.of(cR, cB, "color=Blue", true),
            Arguments.of(cR, cB, "color=Black", false),
            Arguments.of(cB, cR, "color=Blue", false)
        );
    }
}
