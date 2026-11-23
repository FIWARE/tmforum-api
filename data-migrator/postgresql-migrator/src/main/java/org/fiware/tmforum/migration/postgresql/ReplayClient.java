package org.fiware.tmforum.migration.postgresql;

import java.net.URI;

/**
 * Sends one replayed request to a deployment; {@link HttpReplayClient} in production.
 */
interface ReplayClient {

	/**
	 * Throws on a transport failure; any HTTP status is a response.
	 */
	Response send(URI baseUrl, ReplayRequest request);

	/**
	 * {@code body} is empty, never null, without one.
	 */
	record Response(int status, String body) {
	}
}
