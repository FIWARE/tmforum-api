package org.fiware.tmforum.storage.query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Parses the date-time literal forms a TMForum filter accepts, shared by the parser (to pick {@link
 * ValueType#DATE_TIME}) and the renderers (to bind the literal).
 */
public final class DateTimeLiterals {

    private DateTimeLiterals() {
    }

    /**
     * Accepts an ISO-8601 date-time with offset or {@code Z}, a local date-time (read as UTC) and a date (start of
     * day UTC). Empty for anything else.
     */
    public static Optional<OffsetDateTime> parse(String literal) {
        try {
            return Optional.of(OffsetDateTime.parse(literal));
        } catch (DateTimeParseException e) {
            // not offset-qualified, try the local forms
        }
        try {
            return Optional.of(LocalDateTime.parse(literal).atOffset(ZoneOffset.UTC));
        } catch (DateTimeParseException e) {
            // no time part, try a plain date
        }
        try {
            return Optional.of(LocalDate.parse(literal).atStartOfDay().atOffset(ZoneOffset.UTC));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }
}
