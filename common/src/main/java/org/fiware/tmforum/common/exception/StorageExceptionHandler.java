package org.fiware.tmforum.common.exception;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.fiware.tmforum.storage.StorageErrorReason;
import org.fiware.tmforum.storage.StorageException;

/**
 * Renders a {@link StorageException} no call site mapped, keyed on (operation, reason), through the existing handlers:
 * UNAVAILABLE is 503 for every operation, since the storage gave no answer and the request may be repeated.
 * DELETE: 404 for NOT_FOUND, otherwise 500 (deletion body). LIST: TmForum 400 with the stored message for
 * INVALID_DATA, otherwise 500. READ: catch-all 500. CREATE/UPDATE: TmForum 400 with the stored message for
 * PARTIAL_UPDATE, otherwise 502.
 */
@Produces
@Singleton
@Requires(classes = {StorageException.class, ExceptionHandler.class})
@RequiredArgsConstructor
public class StorageExceptionHandler implements ExceptionHandler<StorageException, HttpResponse<ErrorDetails>> {

	private final DeletionExceptionHandler deletionExceptionHandler;
	private final TmForumExceptionHandler tmForumExceptionHandler;
	private final CatchAllExceptionHandler catchAllExceptionHandler;
	private final HttpClientExceptionHandler httpClientExceptionHandler;

	/** Named after neither backend: the same failure is answered alike whichever storage is configured. */
	static final String UNAVAILABLE_MESSAGE = "The storage backend is currently not available.";

	@Override
	public HttpResponse<ErrorDetails> handle(HttpRequest request, StorageException exception) {
		String message = exception.getMessage();
		if (exception.reason() == StorageErrorReason.UNAVAILABLE) {
			return unavailable();
		}
		return switch (exception.operation()) {
			case DELETE -> deletionExceptionHandler.handle(request, new DeletionException(message, exception,
					exception.reason() == StorageErrorReason.NOT_FOUND
							? DeletionExceptionReason.NOT_FOUND
							: DeletionExceptionReason.UNKNOWN));
			// a filter the backend rejects is the client's error, not an internal one
			case LIST -> tmForumExceptionHandler.handle(request, new TmForumException(message, exception,
					exception.reason() == StorageErrorReason.INVALID_DATA
							? TmForumExceptionReason.INVALID_DATA
							: TmForumExceptionReason.UNKNOWN));
			case READ -> catchAllExceptionHandler.handle(request, exception);
			case CREATE, UPDATE -> exception.reason() == StorageErrorReason.PARTIAL_UPDATE
					? tmForumExceptionHandler.handle(request,
					new TmForumException(message, exception, TmForumExceptionReason.INVALID_DATA))
					: httpClientExceptionHandler.handle(request, new HttpClientException(message, exception));
		};
	}

	private static HttpResponse<ErrorDetails> unavailable() {
		return HttpResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(new ErrorDetails(HttpStatus.SERVICE_UNAVAILABLE.toString(),
						HttpStatus.SERVICE_UNAVAILABLE.getReason(),
						UNAVAILABLE_MESSAGE,
						HttpStatus.SERVICE_UNAVAILABLE.toString(),
						null));
	}
}
