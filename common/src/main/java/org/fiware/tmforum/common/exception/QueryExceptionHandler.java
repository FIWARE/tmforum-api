package org.fiware.tmforum.common.exception;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;

/**
 * General handler for all un-catched {@link  QueryException}. Translates them into a proper 503.
 */
@Produces
@Singleton
@Requires(classes = { QueryException.class, ExceptionHandler.class })
@Slf4j
public class QueryExceptionHandler implements ExceptionHandler<QueryException, HttpResponse<ErrorDetails>> {

	@Override
	public HttpResponse<ErrorDetails> handle(HttpRequest request, QueryException exception) {
		return HttpResponse.status(HttpStatus.BAD_REQUEST).body(new ErrorDetails(HttpStatus.BAD_REQUEST.toString(),
				HttpStatus.BAD_REQUEST.getReason(),
				String.format("Invalid query - %s.", exception.getMessage()),
				HttpStatus.BAD_REQUEST.toString(),
				null));
	}
}
