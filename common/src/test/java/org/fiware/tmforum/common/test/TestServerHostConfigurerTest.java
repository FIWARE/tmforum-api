package org.fiware.tmforum.common.test;

import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins that the configured host survives startup and only the port follows the server, so that the callbacks of
 * {@link BrokerCallbackContextBuilder} keep the address the broker can reach.
 */
class TestServerHostConfigurerTest {

	private static final String BOUND_URI = "http://localhost:39507";

	@ParameterizedTest(name = "{0} -> {1}")
	@CsvSource(value = {
			"http://localhost:8632 | http://localhost:39507",
			"http://172.17.0.1 | http://172.17.0.1:39507",
			"http://tmf-api.local:8632 | http://tmf-api.local:39507",
			"NIL | http://localhost:39507",
			"'' | http://localhost:39507",
			"not a uri | http://localhost:39507"
	}, delimiter = '|', nullValues = "NIL")
	void keepsTheConfiguredHostAndFollowsTheServerPort(String configured, String expected) {
		GeneralProperties generalProperties = new GeneralProperties();
		generalProperties.setServerHost(configured == null ? null : configured.trim());

		new TestServerHostConfigurer(generalProperties).onServerStartup(serverStartupEvent());

		assertEquals(expected, generalProperties.getServerHost());
	}

	private ServerStartupEvent serverStartupEvent() {
		EmbeddedServer embeddedServer = mock(EmbeddedServer.class);
		when(embeddedServer.getURI()).thenReturn(URI.create(BOUND_URI));
		return new ServerStartupEvent(embeddedServer);
	}
}
