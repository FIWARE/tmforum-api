package org.fiware.tmforum.party.exception;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.exception.ErrorDetails;

import javax.inject.Singleton;

@Produces
@Singleton
@Requires(classes = {PartyListException.class, ExceptionHandler.class})
@Slf4j
public class PartyListExceptionHandler implements ExceptionHandler<PartyListException, HttpResponse<ErrorDetails>> {

    @Override
    public HttpResponse<ErrorDetails> handle(HttpRequest request, PartyListException exception) {
        log.warn("The parties could not have been listed.", exception);
        if(exception.getCause() instanceof HttpClientResponseException){
            return HttpResponse.status(HttpStatus.BAD_REQUEST).body(new ErrorDetails(HttpStatus.BAD_REQUEST.toString(),
                    HttpStatus.BAD_REQUEST.getReason(),
                    "The list request was invalid.",
                    HttpStatus.BAD_REQUEST.toString(),
                    null));
        }
        return HttpResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorDetails(HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReason(),
                "Something unexpected happend.",
                HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                null));
    }

}
