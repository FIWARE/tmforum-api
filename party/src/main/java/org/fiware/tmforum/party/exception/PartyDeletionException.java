package org.fiware.tmforum.party.exception;

import lombok.Getter;

/**
 * Should be thrown if a party could not be deleted.
 */
public class PartyDeletionException extends RuntimeException {

    @Getter
    private final PartyExceptionReason partyExceptionReason;

    public PartyDeletionException(String message, PartyExceptionReason partyExceptionReason) {
        super(message);
        this.partyExceptionReason = partyExceptionReason;
    }

    public PartyDeletionException(String message, Throwable cause, PartyExceptionReason partyExceptionReason) {
        super(message, cause);
        this.partyExceptionReason = partyExceptionReason;
    }
}
