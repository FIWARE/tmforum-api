package org.fiware.tmforum.mapping;

/**
 * Exception to be thrown in case the mapping fails.
 */
public class MappingException extends RuntimeException {
	
	public MappingException(String message) {
		super(message);
	}

	public MappingException(String message, Throwable cause) {
		super(message, cause);
	}
}
