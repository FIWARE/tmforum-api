package org.fiware.tmforum.storage.query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Optional;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

/**
 * Parses the date-time literal forms a TMForum filter accepts, shared by the parser (to pick {@link
 * ValueType#DATE_TIME}) and the renderers (to bind the literal).
 */
public final class DateTimeLiterals {

    private static final int YEAR_DIGITS = 4;
    private static final int MONTH_AND_DAY_DIGITS = 2;
    // Year 0 is 1 BC; PostgreSQL's timestamptz takes neither it nor offsets beyond ±15:59.
    private static final int MIN_YEAR = 1;
    private static final int MAX_OFFSET_SECONDS = 15 * 3600 + 59 * 60;

    // The ISO_* formatters also take signed and 5+ digit years, which no backend binds safely.
    private static final DateTimeFormatter DATE = strict(new DateTimeFormatterBuilder()
            .appendValue(YEAR, YEAR_DIGITS)
            .appendLiteral('-')
            .appendValue(MONTH_OF_YEAR, MONTH_AND_DAY_DIGITS)
            .appendLiteral('-')
            .appendValue(DAY_OF_MONTH, MONTH_AND_DAY_DIGITS));
    private static final DateTimeFormatter LOCAL_DATE_TIME = strict(new DateTimeFormatterBuilder()
            .append(DATE)
            .appendLiteral('T')
            .append(DateTimeFormatter.ISO_LOCAL_TIME));
    private static final DateTimeFormatter OFFSET_DATE_TIME = strict(new DateTimeFormatterBuilder()
            .append(LOCAL_DATE_TIME)
            // As in ISO_OFFSET_DATE_TIME: lenient takes an hours-only offset ("+02") as well.
            .parseLenient()
            .appendOffsetId()
            .parseStrict());

    private DateTimeLiterals() {
    }

    private static DateTimeFormatter strict(DateTimeFormatterBuilder builder) {
        return builder.toFormatter().withResolverStyle(ResolverStyle.STRICT).withChronology(IsoChronology.INSTANCE);
    }

    /**
     * Accepts an ISO-8601 date-time with offset ({@code +HH}, {@code +HH:MM}, {@code +HH:MM:SS}) or {@code Z}, a
     * local date-time (read as UTC) and a date (start of day UTC), with a 4-digit year from 0001 and an offset within
     * ±15:59. Empty for anything else.
     */
    public static Optional<OffsetDateTime> parse(String literal) {
        return parseAnyForm(literal).filter(DateTimeLiterals::isInBindableRange);
    }

    private static Optional<OffsetDateTime> parseAnyForm(String literal) {
        try {
            return Optional.of(OffsetDateTime.parse(literal, OFFSET_DATE_TIME));
        } catch (DateTimeParseException e) {
            // not offset-qualified, try the local forms
        }
        try {
            return Optional.of(LocalDateTime.parse(literal, LOCAL_DATE_TIME).atOffset(ZoneOffset.UTC));
        } catch (DateTimeParseException e) {
            // no time part, try a plain date
        }
        try {
            return Optional.of(LocalDate.parse(literal, DATE).atStartOfDay().atOffset(ZoneOffset.UTC));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private static boolean isInBindableRange(OffsetDateTime dateTime) {
        return dateTime.getYear() >= MIN_YEAR
                && Math.abs(dateTime.getOffset().getTotalSeconds()) <= MAX_OFFSET_SECONDS;
    }
}
