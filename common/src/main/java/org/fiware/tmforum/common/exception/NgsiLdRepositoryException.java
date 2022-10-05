package org.fiware.tmforum.common.exception;

import lombok.Getter;

import java.util.Optional;

/**
 * Wrapper exception for everything that might go wrong when using the {@link org.fiware.tmforum.common.repository.NgsiLdBaseRepository}
 */
public class NgsiLdRepositoryException extends RuntimeException {

	@Getter
	private final Optional<Throwable> optionalCause;


	public NgsiLdRepositoryException(String message,  Optional<Throwable> cause) {
		super(message);
		optionalCause = cause;
	}
}
