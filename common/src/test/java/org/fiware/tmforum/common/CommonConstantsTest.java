package org.fiware.tmforum.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The JSON-LD context Link header sent to the brokers is well-formed per RFC 8288.
 */
class CommonConstantsTest {

	private static final String CONTEXT_URL = "http://context-server/context.jsonld";

	@Test
	void contextLinkHeaderIsWellFormed() {
		assertEquals(
				"<http://context-server/context.jsonld>; rel=\"http://www.w3.org/ns/json-ld#context\"; "
						+ "type=\"application/ld+json\"",
				String.format(CommonConstants.JSON_LD_CONTEXT_LINK_TEMPLATE, CONTEXT_URL));
	}
}
