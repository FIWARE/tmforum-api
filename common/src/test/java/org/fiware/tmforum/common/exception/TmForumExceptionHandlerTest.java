package org.fiware.tmforum.common.exception;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TmForumExceptionHandlerTest {

	private static final String WRAPPER_MESSAGE = "Was not able to create product offering urn:ngsi-ld:product-offering:1";
	private static final String REFERENCE_MESSAGE = "Reference version mismatch: [urn:ngsi-ld:product-specification:1 (version 2.0, current 1.0)]";
	private static final String INVALID_RELATIONSHIP_PREFIX = "At least one of the references does not exist - ";

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
}
