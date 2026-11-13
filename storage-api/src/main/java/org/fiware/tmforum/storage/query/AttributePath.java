package org.fiware.tmforum.storage.query;

import java.util.List;
import java.util.Objects;

/**
 * Attribute addressed by a filter or sort, as TMForum bean property names (after reserved-token translation, e.g.
 * {@code @type} to {@code atType}). Each renderer maps them to its own storage layout. {@code declared} is false
 * for a free-form extension attribute.
 */
public record AttributePath(List<String> segments, boolean declared) {

    public AttributePath {
        Objects.requireNonNull(segments, "segments");
        segments = List.copyOf(segments);
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("An attribute path needs at least one segment.");
        }
        if (segments.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("An attribute path must not contain blank segments: " + segments);
        }
    }
}
