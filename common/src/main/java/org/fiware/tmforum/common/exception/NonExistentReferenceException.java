package org.fiware.tmforum.common.exception;

/**
 * Exception to be thrown if a reference does not exist
 */
public class NonExistentReferenceException extends RuntimeException {

	public NonExistentReferenceException(String message) {
		super(message);
	}

	public NonExistentReferenceException(String message, Throwable cause) {
		super(message, cause);
	}
}
