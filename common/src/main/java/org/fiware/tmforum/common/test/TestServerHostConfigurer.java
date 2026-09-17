package org.fiware.tmforum.common.test;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import jakarta.inject.Singleton;
import org.fiware.tmforum.common.configuration.GeneralProperties;

/**
 * Keeps {@link GeneralProperties#getServerHost()} in sync with the embedded server's actual bound
 * address during tests.
 * <p>
 * Test application.yaml files set {@code micronaut.server.port: -1} (ephemeral port) so that
 * {@code @MicronautTest} classes never race for a fixed port. Without this listener, the statically
 * configured {@code general.serverHost} would go stale, and NGSI-LD subscription callbacks built from
 * it (see {@code AbstractSubscriptionApiController.getCallbackURI}) would point at the wrong port.
 * <p>
 * Test-only ({@code @Requires(env = Environment.TEST)}): production deployments keep configuring
 * general.serverHost explicitly (e.g. a hostname reachable from outside the pod), unaffected by this
 * bean.
 */
@Singleton
@Requires(env = Environment.TEST)
public class TestServerHostConfigurer {

	private final GeneralProperties generalProperties;

	public TestServerHostConfigurer(GeneralProperties generalProperties) {
		this.generalProperties = generalProperties;
	}

	@EventListener
	void onServerStartup(ServerStartupEvent event) {
		generalProperties.setServerHost(event.getSource().getURI().toString());
	}
}
