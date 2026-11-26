package org.fiware.tmforum.common.repository;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import org.fiware.tmforum.storage.StorageErrorReason;
import org.fiware.tmforum.storage.StorageException;
import org.fiware.tmforum.storage.StorageOperation;

/**
 * Translates broker client failures into {@link StorageException}s, so that no broker error leaves the adapter.
 */
final class BrokerFailures {

	static final String BROKER_CALL_FAILED = "Was not able to successfully call the broker.";

	private BrokerFailures() {
	}

	/**
	 * Failure of a write. Its message is the broker's answer, which call sites interpolate into their response, or the
	 * client's message if the broker did not answer.
	 */
	static StorageException writeFailure(StorageOperation operation, HttpClientException failure) {
		if (failure instanceof HttpClientResponseException responseException) {
			return new StorageException(operation, reasonOf(responseException.getStatus()),
					responseException.getResponse().getBody(String.class).orElse(responseException.getMessage()),
					responseException);
		}
		return new StorageException(operation, StorageErrorReason.UNAVAILABLE, failure.getMessage(), failure);
	}

	/**
	 * Reason of a read failure: UNAVAILABLE if the broker gave no answer, also for a nested storage failure, the
	 * status's own reason if it answered with one - a query the renderer let through and the broker rejected is the
	 * client's error, as on PostgreSQL - and UNKNOWN otherwise.
	 */
	static StorageErrorReason unknownOrUnavailable(Throwable failure) {
		if (failure instanceof StorageException storageException) {
			return storageException.reason() == StorageErrorReason.UNAVAILABLE
					? StorageErrorReason.UNAVAILABLE
					: StorageErrorReason.UNKNOWN;
		}
		if (failure instanceof HttpClientResponseException responseException) {
			return reasonOf(responseException.getStatus());
		}
		return failure instanceof HttpClientException ? StorageErrorReason.UNAVAILABLE : StorageErrorReason.UNKNOWN;
	}

	private static StorageErrorReason reasonOf(HttpStatus status) {
		return switch (status) {
			case NOT_FOUND -> StorageErrorReason.NOT_FOUND;
			case SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT -> StorageErrorReason.UNAVAILABLE;
			case CONFLICT -> StorageErrorReason.CONFLICT;
			case BAD_REQUEST -> StorageErrorReason.INVALID_DATA;
			default -> StorageErrorReason.UNKNOWN;
		};
	}
}
