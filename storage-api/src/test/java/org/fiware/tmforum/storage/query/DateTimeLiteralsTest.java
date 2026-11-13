package org.fiware.tmforum.storage.query;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateTimeLiteralsTest {

    private static final ZoneOffset PLUS_TWO = ZoneOffset.ofHours(2);

    static Stream<Arguments> parsableLiterals() {
        return Stream.of(
                Arguments.of("2024-01-01T10:15:30Z", OffsetDateTime.of(2024, 1, 1, 10, 15, 30, 0, ZoneOffset.UTC)),
                Arguments.of("2024-01-01T10:15:30.500Z",
                        OffsetDateTime.of(2024, 1, 1, 10, 15, 30, 500_000_000, ZoneOffset.UTC)),
                Arguments.of("2024-01-01T10:15:30+02:00", OffsetDateTime.of(2024, 1, 1, 10, 15, 30, 0, PLUS_TWO)),
                Arguments.of("2024-01-01T10:15:30", OffsetDateTime.of(2024, 1, 1, 10, 15, 30, 0, ZoneOffset.UTC)),
                Arguments.of("2024-01-01T10:15", OffsetDateTime.of(2024, 1, 1, 10, 15, 0, 0, ZoneOffset.UTC)),
                Arguments.of("2024-01-01", OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)),
                Arguments.of("0001-01-01", OffsetDateTime.of(1, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)),
                Arguments.of("9999-12-31T23:59:59-15:59",
                        OffsetDateTime.of(9999, 12, 31, 23, 59, 59, 0, ZoneOffset.ofHoursMinutes(-15, -59))));
    }

    @ParameterizedTest
    @MethodSource("parsableLiterals")
    void parsesSupportedForms(String literal, OffsetDateTime expected) {
        assertEquals(Optional.of(expected), DateTimeLiterals.parse(literal));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "now", "42", "true", "2024-13-01", "2024-01-01T25:00:00Z", "01.01.2024",
            "2024-01-01 10:15:30", "2024-01-01T10:15:30+0200", "2024-01-01T10:15:30Z[Europe/Berlin]",
            "+10000-01-01", "-0001-01-01", "+2024-01-01", "0000-01-01", "24-01-01", "2024-1-1",
            "2024-01-01T00:00+16:00", "2024-01-01T00:00+18:00", "2024-01-01T00:00-18:00"})
    void rejectsOtherLiterals(String literal) {
        assertEquals(Optional.empty(), DateTimeLiterals.parse(literal));
    }
}
