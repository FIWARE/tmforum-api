package org.fiware.tmforum.common.health;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.order.Ordered;
import io.micronaut.health.HealthStatus;
import io.micronaut.management.endpoint.health.HealthEndpoint;
import io.micronaut.management.health.indicator.HealthIndicator;
import io.micronaut.management.health.indicator.HealthResult;
import io.micronaut.management.health.indicator.annotation.Liveness;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.runtime.server.event.ServerShutdownEvent;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * Health indicator that reports liveness based on the server lifecycle
 */
@Singleton
@Requires(beans = HealthEndpoint.class)
@Liveness
public class ServerLivenessIndicator implements HealthIndicator {

	private boolean serverRunning = false;

	@Override
	public Publisher<HealthResult> getResult() {
		return Mono.just(HealthResult.builder("server").status(serverRunning ? HealthStatus.UP : HealthStatus.DOWN).build());
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	@EventListener
	public void onServerStarted(ServerStartupEvent event) {
		serverRunning = true;
	}

	@EventListener
	public void onServerShutdown(ServerShutdownEvent event){
		serverRunning = false;
	}
}
