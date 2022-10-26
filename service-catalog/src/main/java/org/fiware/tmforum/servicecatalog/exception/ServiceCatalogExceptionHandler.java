package org.fiware.tmforum.servicecatalog.exception;

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
@Requires(classes = {ServiceCatalogException.class, ExceptionHandler.class})
@Slf4j
public class ServiceCatalogExceptionHandler implements ExceptionHandler<ServiceCatalogException, HttpResponse<ErrorDetails>> {

    @Override
    public HttpResponse<ErrorDetails> handle(HttpRequest request, ServiceCatalogException exception) {
        return switch (exception.getInventoryExceptionReason()) {
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
                    "The request contained invalid data.",
                    HttpStatus.BAD_REQUEST.toString(),
                    null));
            case NOT_FOUND -> HttpResponse.status(HttpStatus.NOT_FOUND).body(new ErrorDetails(HttpStatus.NOT_FOUND.toString(),
                    HttpStatus.NOT_FOUND.getReason(),
                    "The requested object could not be found",
                    HttpStatus.NOT_FOUND.toString(),
                    null));
            default -> HttpResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorDetails(HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                    HttpStatus.INTERNAL_SERVER_ERROR.getReason(),
                    "An unexpected error happened.",
                    HttpStatus.INTERNAL_SERVER_ERROR.toString(),
                    null));
        };
    }
}
