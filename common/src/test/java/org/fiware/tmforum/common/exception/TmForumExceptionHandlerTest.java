package org.fiware.tmforum.common.exception;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TmForumExceptionHandlerTest {

	private static final String WRAPPER_MESSAGE = "Was not able to create product offering urn:ngsi-ld:product-offering:1";
	private static final String REFERENCE_MESSAGE = "Reference version mismatch: [urn:ngsi-ld:product-specification:1 (version 2.0, current 1.0)]";
	private static final String INVALID_RELATIONSHIP_PREFIX = "At least one of the references does not exist - ";
	private static final String CONFLICT_PREFIX = "At least one of the entities already exists - ";
	private static final String INVALID_DATA_PREFIX = "The request contained invalid data - ";
	private static final String NOT_FOUND_PREFIX = "The requested object could not be found - ";
	private static final String FORBIDDEN_PREFIX = "The requested operation is not permitted - ";
	private static final String UNKNOWN_PREFIX = "An unexpected error happened - ";
	private static final String EXCEPTION_MESSAGE = "Order urn:ngsi-ld:product-order:1 was rejected";

	private final TmForumExceptionHandler handler = new TmForumExceptionHandler();

	private static Stream<Arguments> invalidRelationships() {
		NonExistentReferenceException referenceCause = new NonExistentReferenceException(REFERENCE_MESSAGE);
		return Stream.of(
				Arguments.of("A direct reference cause is appended.",
						new TmForumException(WRAPPER_MESSAGE, referenceCause, TmForumExceptionReason.INVALID_RELATIONSHIP),
						INVALID_RELATIONSHIP_PREFIX + WRAPPER_MESSAGE + " - " + REFERENCE_MESSAGE),
				Arguments.of("A nested reference cause is appended.",
						new TmForumException(WRAPPER_MESSAGE, new IllegalStateException(referenceCause),
								TmForumExceptionReason.INVALID_RELATIONSHIP),
						INVALID_RELATIONSHIP_PREFIX + WRAPPER_MESSAGE + " - " + REFERENCE_MESSAGE),
				Arguments.of("Any other cause is not appended.",
						new TmForumException(WRAPPER_MESSAGE, new IllegalStateException("internal"),
								TmForumExceptionReason.INVALID_RELATIONSHIP),
						INVALID_RELATIONSHIP_PREFIX + WRAPPER_MESSAGE),
				Arguments.of("Without a cause only the message is reported.",
						new TmForumException(WRAPPER_MESSAGE, TmForumExceptionReason.INVALID_RELATIONSHIP),
						INVALID_RELATIONSHIP_PREFIX + WRAPPER_MESSAGE));
	}

	@ParameterizedTest
	@MethodSource("invalidRelationships")
	void invalidRelationshipReportsTheReferenceCause(String message, TmForumException exception, String expected) {
		HttpResponse<ErrorDetails> response = handler.handle(null, exception);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatus(), message);
		assertEquals(expected, response.body().message(), message);
	}

	private static Stream<Arguments> reasons() {
		return Stream.of(
				Arguments.of(TmForumExceptionReason.CONFLICT, HttpStatus.CONFLICT, CONFLICT_PREFIX),
				Arguments.of(TmForumExceptionReason.INVALID_DATA, HttpStatus.BAD_REQUEST, INVALID_DATA_PREFIX),
				Arguments.of(TmForumExceptionReason.INVALID_RELATIONSHIP, HttpStatus.BAD_REQUEST,
						INVALID_RELATIONSHIP_PREFIX),
				Arguments.of(TmForumExceptionReason.NOT_FOUND, HttpStatus.NOT_FOUND, NOT_FOUND_PREFIX),
				Arguments.of(TmForumExceptionReason.FORBIDDEN, HttpStatus.FORBIDDEN, FORBIDDEN_PREFIX),
				Arguments.of(TmForumExceptionReason.UNKNOWN, HttpStatus.INTERNAL_SERVER_ERROR, UNKNOWN_PREFIX));
	}

	@ParameterizedTest
	@MethodSource("reasons")
	void reasonRendersItsStatusAndErrorDetails(TmForumExceptionReason reason, HttpStatus status, String prefix) {
		HttpResponse<ErrorDetails> response = handler.handle(null, new TmForumException(EXCEPTION_MESSAGE, reason));

		assertEquals(status, response.getStatus(), reason.name());
		ErrorDetails body = response.body();
		assertEquals(status.toString(), body.code(), reason.name());
		assertEquals(status.getReason(), body.reason(), reason.name());
		assertEquals(prefix + EXCEPTION_MESSAGE, body.message(), reason.name());
		assertEquals(status.toString(), body.status(), reason.name());
	}

	@Test
	void reasonsCoverEveryExceptionReason() {
		Set<TmForumExceptionReason> covered = reasons()
				.map(arguments -> (TmForumExceptionReason) arguments.get()[0])
				.collect(Collectors.toSet());

		assertEquals(Set.copyOf(Arrays.asList(TmForumExceptionReason.values())), covered,
				"Every TmForumExceptionReason needs a row in reasons().");
	}
}
