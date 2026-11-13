package org.fiware.tmforum.common.test;

import java.util.Map;

/**
 * Test context builder for the ITs that need the broker to call the test server back, e.g. to deliver a
 * subscription notification. It configures {@code general.serverHost} with a host the broker can connect to (see
 * {@link BrokerCallbackHost}); {@link TestServerHostConfigurer} adds the port the server actually bound to.
 * Use via {@code @MicronautTest(contextBuilder = BrokerCallbackContextBuilder.class)}.
 */
public class BrokerCallbackContextBuilder extends FirstMatchConfigContextBuilder {

	private static final String SERVER_HOST_PROPERTY = "general.serverHost";
	private static final String HTTP_SCHEME = "http://";

	public BrokerCallbackContextBuilder() {
		properties(Map.of(SERVER_HOST_PROPERTY, HTTP_SCHEME + BrokerCallbackHost.resolve()));
	}
}
