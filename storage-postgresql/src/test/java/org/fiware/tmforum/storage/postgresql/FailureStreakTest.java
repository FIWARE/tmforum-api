package org.fiware.tmforum.storage.postgresql;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FailureStreakTest {

    private static final boolean FAIL = false;
    private static final boolean SUCCEED = true;

    static Stream<Arguments> outcomes() {
        return Stream.of(
                Arguments.of(List.of(SUCCEED, SUCCEED), List.of(false, false)),
                Arguments.of(List.of(FAIL), List.of(true)),
                Arguments.of(List.of(FAIL, FAIL, FAIL), List.of(true, false, false)),
                Arguments.of(List.of(FAIL, FAIL, SUCCEED, SUCCEED), List.of(true, false, true, false)),
                Arguments.of(List.of(FAIL, SUCCEED, FAIL, FAIL), List.of(true, true, true, false)));
    }

    @ParameterizedTest
    @MethodSource("outcomes")
    void reportsOnlyTheStartAndEndOfAStreak(List<Boolean> outcomes, List<Boolean> expectedReports) {
        FailureStreak streak = new FailureStreak();
        List<Boolean> reports = new ArrayList<>();
        outcomes.forEach(success -> reports.add(success ? streak.succeeded() : streak.failed()));
        assertEquals(expectedReports, reports,
                "Only the first failure of a streak and the success ending it should be reported.");
    }
}
