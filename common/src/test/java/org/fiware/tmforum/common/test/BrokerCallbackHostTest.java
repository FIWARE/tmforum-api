package org.fiware.tmforum.common.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Pins that the callback host can be pinned per environment and always resolves to some host.
 */
class BrokerCallbackHostTest {

	@AfterEach
	void clearPinnedHost() {
		System.clearProperty(BrokerCallbackHost.HOST_PROPERTY);
	}

	@ParameterizedTest
	@ValueSource(strings = {"localhost", "10.88.0.1", " host.containers.internal "})
	void usesThePinnedHost(String pinned) {
		System.setProperty(BrokerCallbackHost.HOST_PROPERTY, pinned);

		assertEquals(pinned.trim(), BrokerCallbackHost.resolve());
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "   "})
	void ignoresABlankPinnedHost(String pinned) {
		System.setProperty(BrokerCallbackHost.HOST_PROPERTY, pinned);

		assertNotNull(BrokerCallbackHost.resolve());
	}
}
