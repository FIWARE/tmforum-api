package org.fiware.tmforum.accesspolicy;

/**
 * Signals a rejected bearer token. The message is only the reason and there is no stack trace or cause, so neither
 * the token nor a library message describing it can leak.
 */
final class TokenRejectedException extends RuntimeException {

	private final TokenRejection reason;

	TokenRejectedException(TokenRejection reason) {
		super(reason.name(), null, false, false);
		this.reason = reason;
	}

	TokenRejection reason() {
		return reason;
	}
}
