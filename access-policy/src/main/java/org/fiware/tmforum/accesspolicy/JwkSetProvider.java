package org.fiware.tmforum.accesspolicy;

import com.nimbusds.jose.jwk.JWKSet;
import reactor.core.publisher.Mono;

/**
 * Source of the keys bearer tokens are verified against, an interface so tests can supply an in-memory set.
 */
public interface JwkSetProvider {

	/** The key set, possibly cached. Errors only while no key set is available at all. */
	Mono<JWKSet> current();

	/** Refetches the key set for a key the current one lacks; an implementation may rate-limit and answer the cached set. */
	Mono<JWKSet> refresh();
}
