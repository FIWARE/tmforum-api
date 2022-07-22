package org.fiware.tmforum.party.exception;

/**
 * Exception to be thrown in case a party could not have been created.
 */
public class PartyCreationException extends RuntimeException {
	public PartyCreationException(String message) {
		super(message);
	}

	public PartyCreationException(String message, Throwable cause) {
		super(message, cause);
	}
}
