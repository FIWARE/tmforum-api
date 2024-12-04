package org.fiware.tmforum.common.exception;

import lombok.Getter;

import java.util.List;

/**
 * * Exceptions thrown on json-schema validation.
 */
public class SchemaValidationException extends RuntimeException {

	@Getter
	private final List<String> assertionMessages;

	public SchemaValidationException(List<String> assertionMessages, String message) {
		super(message);
		this.assertionMessages = assertionMessages;
	}

	public SchemaValidationException(List<String> assertionMessages, String message, Throwable cause) {
		super(message, cause);
		this.assertionMessages = assertionMessages;
	}
}
