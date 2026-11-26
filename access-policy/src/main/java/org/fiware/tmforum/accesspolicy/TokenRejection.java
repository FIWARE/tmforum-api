package org.fiware.tmforum.accesspolicy;

/**
 * Why a bearer token was not accepted. Safe to log, it never carries any part of the token.
 */
enum TokenRejection {
	MALFORMED,
	UNSUPPORTED_ALGORITHM,
	UNKNOWN_KEY,
	BAD_SIGNATURE,
	EXPIRED,
	NOT_YET_VALID,
	WRONG_ISSUER,
	WRONG_AUDIENCE,
	NO_IDENTITY,
	KEYS_UNAVAILABLE
}
