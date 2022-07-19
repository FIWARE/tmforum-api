package org.fiware.tmforum.common.exception;

import java.util.Optional;

/**
 * Details of an error provided to the caller
 */
public record ErrorDetails(Optional<String> upstreamStatus, String reason) {
}
