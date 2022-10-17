package org.fiware.tmforum.party.exception;

public class PartyListException extends RuntimeException {

    public PartyListException(String message) {
        super(message);
    }

    public PartyListException(String message, Throwable cause) {
        super(message, cause);
    }
}
