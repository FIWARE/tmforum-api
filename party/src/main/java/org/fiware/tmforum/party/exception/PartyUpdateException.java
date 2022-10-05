package org.fiware.tmforum.party.exception;

import lombok.Getter;

/**
 * Exception to be thrown in case a party could not have been created.
 */
public class PartyUpdateException extends RuntimeException {

	@Getter
	private final PartyExceptionReason partyExceptionReason;

	public PartyUpdateException(String message, PartyExceptionReason partyExceptionReason) {
		super(message);
		this.partyExceptionReason = partyExceptionReason;
	}

	public PartyUpdateException(String message, Throwable cause, PartyExceptionReason partyExceptionReason) {
		super(message, cause);
		this.partyExceptionReason = partyExceptionReason;
	}
}
