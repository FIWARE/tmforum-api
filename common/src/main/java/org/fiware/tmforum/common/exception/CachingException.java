package org.fiware.tmforum.common.exception;

public class CachingException extends RuntimeException {

	public CachingException(String message) {
		super(message);
	}

	public CachingException(String message, Throwable cause) {
		super(message, cause);
	}
}
