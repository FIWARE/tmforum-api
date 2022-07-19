package org.fiware.tmforum.common.exception;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.Optional;


@Produces
@Singleton
@Requires(classes = {Exception.class, ExceptionHandler.class})
@Slf4j
public class CatchAllExceptionHandler implements ExceptionHandler<Exception, HttpResponse<ErrorDetails>> {

	@Override
	public HttpResponse<ErrorDetails> handle(HttpRequest request, Exception exception) {
		log.warn("Received unexpected exception {} for request {}.", exception.getMessage(), request, exception);
		return HttpResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorDetails(Optional.empty(), "Request could not be answered due to an unexpected internal error."));
	}
}
