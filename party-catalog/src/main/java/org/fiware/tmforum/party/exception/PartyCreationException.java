package org.fiware.tmforum.party.exception;

import lombok.Getter;

/**
 * Exception to be thrown in case a party could not have been created.
 */
public class PartyCreationException extends RuntimeException {

	@Getter
	private final PartyExceptionReason partyExceptionReason;

	public PartyCreationException(String message, PartyExceptionReason partyExceptionReason) {
		super(message);
		this.partyExceptionReason = partyExceptionReason;
	}

	public PartyCreationException(String message, Throwable cause, PartyExceptionReason partyExceptionReason) {
		super(message, cause);
		this.partyExceptionReason = partyExceptionReason;
	}
}
