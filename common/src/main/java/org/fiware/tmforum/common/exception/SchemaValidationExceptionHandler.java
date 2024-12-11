package org.fiware.tmforum.common.exception;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.stream.Collectors;

/**
 * Handle exceptions thrown on json-schema validation.
 */
@Produces
@Singleton
@Requires(classes = {SchemaValidationException.class, ExceptionHandler.class})
@Slf4j
public class SchemaValidationExceptionHandler implements ExceptionHandler<SchemaValidationException, HttpResponse<ErrorDetails>> {

	@Override
	public HttpResponse<ErrorDetails> handle(HttpRequest request, SchemaValidationException exception) {
		log.debug("Got Schema  validation:", exception);
		String validationMessages = exception.getAssertionMessages().stream().collect(Collectors.joining(", "));
		return HttpResponse.status(HttpStatus.BAD_REQUEST).body(new ErrorDetails(HttpStatus.BAD_REQUEST.toString(),
				HttpStatus.BAD_REQUEST.getReason(),
				String.format("Schema validation failed with message %s. Reasons: %s", exception.getMessage(), validationMessages),
				HttpStatus.BAD_REQUEST.toString(),
				null));
	}
}


