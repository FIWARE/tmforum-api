package org.fiware.tmforum.common.exception;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;

@Produces
@Singleton
@Requires(classes = { TmForumException.class, ExceptionHandler.class})
@Slf4j
public class TmForumExceptionHandler implements ExceptionHandler<TmForumException, HttpResponse<ErrorDetails>> {

    @Override
    public HttpResponse<ErrorDetails> handle(HttpRequest request, TmForumException exception) {
        log.debug("Got TMForum  Exception:", exception);
        return switch (exception.getExceptionReason()) {
            case CONFLICT -> HttpResponse.status(HttpStatus.CONFLICT).body(new ErrorDetails(HttpStatus.CONFLICT.toString(),
                    HttpStatus.CONFLICT.getReason(),
                    String.format("At least one of the entities already exists - %s", exception.getMessage()),
                    HttpStatus.CONFLICT.toString(),
                    null));
            case INVALID_RELATIONSHIP -> HttpResponse.status(HttpStatus.BAD_REQUEST).body(new ErrorDetails(HttpStatus.BAD_REQUEST.toString(),
                    HttpStatus.BAD_REQUEST.getReason(),
                    String.format("At least one of the references does not exist - %s", exception.getMessage()),
                    HttpStatus.BAD_REQUEST.toString(),
                    null));
            case INVALID_DATA -> HttpResponse.status(HttpStatus.BAD_REQUEST).body(new ErrorDetails(HttpStatus.BAD_REQUEST.toString(),
                    HttpStatus.BAD_REQUEST.getReason(),
                    String.format("The request contained invalid data - %s", exception.getMessage()),
                    HttpStatus.BAD_REQUEST.toString(),
                    null));
            case NOT_FOUND -> HttpResponse.status(HttpStatus.NOT_FOUND).body(new ErrorDetails(HttpStatus.NOT_FOUND.toString(),
                    HttpStatus.NOT_FOUND.getReason(),
                    String.format("The requested object could not be found - %s", exception.getMessage()),
                    HttpStatus.NOT_FOUND.toString(),
                    null));
            default -> HttpResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorDetails(HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                    HttpStatus.INTERNAL_SERVER_ERROR.getReason(),
                    String.format("An unexpected error happened - %s", exception.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                    null));
        };
    }
}
