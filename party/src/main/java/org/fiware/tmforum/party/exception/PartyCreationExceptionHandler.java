package org.fiware.tmforum.party.exception;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.exception.ErrorDetails;

import javax.inject.Singleton;

@Produces
@Singleton
@Requires(classes = {PartyCreationException.class, ExceptionHandler.class})
@Slf4j
public class PartyCreationExceptionHandler implements ExceptionHandler<PartyCreationException, HttpResponse<ErrorDetails>> {

    @Override
    public HttpResponse<ErrorDetails> handle(HttpRequest request, PartyCreationException exception) {
        log.warn("The party could not have been created.", exception);
        return switch (exception.getPartyExceptionReason()) {
            case CONFLICT -> HttpResponse.status(HttpStatus.CONFLICT).body(new ErrorDetails(HttpStatus.CONFLICT.toString(),
                    HttpStatus.CONFLICT.getReason(),
                    "At least one of the entities already exists.",
                    HttpStatus.CONFLICT.toString(),
                    null));
            case INVALID_RELATIONSHIP -> HttpResponse.status(HttpStatus.BAD_REQUEST).body(new ErrorDetails(HttpStatus.BAD_REQUEST.toString(),
                    HttpStatus.BAD_REQUEST.getReason(),
                    "At least one of the references does not exist.",
                    HttpStatus.BAD_REQUEST.toString(),
                    null));
            case INVALID_DATA -> HttpResponse.status(HttpStatus.BAD_REQUEST).body(new ErrorDetails(HttpStatus.BAD_REQUEST.toString(),
                    HttpStatus.BAD_REQUEST.getReason(),
                    "The creation request contained invalid data.",
                    HttpStatus.BAD_REQUEST.toString(),
                    null));
            default -> HttpResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorDetails(HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                    HttpStatus.INTERNAL_SERVER_ERROR.getReason(),
                    "An unexpected error happend.",
                    HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                    null));
        };
    }
}
