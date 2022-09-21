package org.fiware.tmforum.common.exception;

/**
 * Wrapper exception for everything that might go wrong when using the {@link org.fiware.tmforum.common.repository.NgsiLdBaseRepository}
 */
public class NgsiLdRepositoryException extends RuntimeException {

	public NgsiLdRepositoryException(String message) {
		super(message);
	}

	public NgsiLdRepositoryException(String message, Throwable cause) {
		super(message, cause);
	}
}
