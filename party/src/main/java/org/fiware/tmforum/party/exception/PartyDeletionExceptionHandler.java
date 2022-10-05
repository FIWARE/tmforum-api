package org.fiware.tmforum.party.exception;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.exception.ErrorDetails;

import javax.inject.Singleton;

@Produces
@Singleton
@Requires(classes = {PartyDeletionException.class, ExceptionHandler.class})
@Slf4j
public class PartyDeletionExceptionHandler implements ExceptionHandler<PartyDeletionException, HttpResponse<ErrorDetails>> {

    @Override
    public HttpResponse<ErrorDetails> handle(HttpRequest request, PartyDeletionException exception) {
        log.warn("The party could not have been deleted.", exception);
        return switch (exception.getPartyExceptionReason()) {
            case NOT_FOUND -> HttpResponse.status(HttpStatus.NOT_FOUND).body(new ErrorDetails(HttpStatus.NOT_FOUND.toString(),
                    HttpStatus.NOT_FOUND.getReason(),
                    "The requested entity doesn't exist.",
                    HttpStatus.NOT_FOUND.toString(),
                    null));
            default -> HttpResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorDetails(HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                    HttpStatus.INTERNAL_SERVER_ERROR.getReason(),
                    "An unexpected error happend.",
                    HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                    null));
        };
    }
}
