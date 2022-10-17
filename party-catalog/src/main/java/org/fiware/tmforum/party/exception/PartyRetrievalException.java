package org.fiware.tmforum.party.exception;

import lombok.Getter;

public class PartyRetrievalException extends RuntimeException {

    @Getter
    private final PartyExceptionReason reason;

    public PartyRetrievalException(String message, PartyExceptionReason reason) {
        super(message);
        this.reason = reason;
    }

    public PartyRetrievalException(String message, Throwable cause, PartyExceptionReason reason) {
        super(message, cause);
        this.reason = reason;
    }
}
