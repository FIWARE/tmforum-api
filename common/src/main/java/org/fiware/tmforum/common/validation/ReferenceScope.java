package org.fiware.tmforum.common.validation;

/**
 * Where a referenced entity is held, as classified by {@link ReferenceClassifier}.
 */
public enum ReferenceScope {
	/** Held by this deployment, validated strictly. */
	LOCAL,
	/** External by its id prefix, never looked up. */
	EXTERNAL,
	/** External by its href prefix only, looked up locally by id to guard against a wrong-typed local entity. */
	EXTERNAL_BY_HREF
}
