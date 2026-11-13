package org.fiware.tmforum.common.test;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import jakarta.inject.Singleton;
import org.fiware.tmforum.common.configuration.GeneralProperties;

import java.net.URI;
import java.util.Optional;

/**
 * Keeps {@link GeneralProperties#getServerHost()} in sync with the embedded server's actual bound
 * port during tests.
 * <p>
 * Test application.yaml files set {@code micronaut.server.port: -1} (ephemeral port) so that
 * {@code @MicronautTest} classes never race for a fixed port. Without this listener, the statically
 * configured {@code general.serverHost} would go stale, and NGSI-LD subscription callbacks built from
 * it (see {@code AbstractSubscriptionApiController.getCallbackURI}) would point at the wrong port.
 * <p>
 * Only the port is taken from the server: the configured host is kept, so a test can point the callbacks at an
 * address its broker can reach (see {@link BrokerCallbackContextBuilder}), which the server's own
 * {@code localhost} URI is not when the broker runs in a container.
 * <p>
 * Test-only ({@code @Requires(env = Environment.TEST)}): production deployments keep configuring
 * general.serverHost explicitly (e.g. a hostname reachable from outside the pod), unaffected by this
 * bean.
 */
@Singleton
@Requires(env = Environment.TEST)
public class TestServerHostConfigurer {

	private static final String SERVER_HOST_TEMPLATE = "%s://%s:%d";

	private final GeneralProperties generalProperties;

	public TestServerHostConfigurer(GeneralProperties generalProperties) {
		this.generalProperties = generalProperties;
	}

	@EventListener
	void onServerStartup(ServerStartupEvent event) {
		URI boundUri = event.getSource().getURI();
		generalProperties.setServerHost(configuredHost()
				.map(host -> String.format(SERVER_HOST_TEMPLATE, boundUri.getScheme(), host, boundUri.getPort()))
				.orElseGet(boundUri::toString));
	}

	private Optional<String> configuredHost() {
		String configured = generalProperties.getServerHost();
		if (configured == null || configured.isBlank()) {
			return Optional.empty();
		}
		try {
			return Optional.ofNullable(URI.create(configured.trim()).getHost());
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}
}
