package org.fiware.tmforum.common.health;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.order.Ordered;
import io.micronaut.health.HealthStatus;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.management.endpoint.health.HealthEndpoint;
import io.micronaut.management.health.indicator.HealthIndicator;
import io.micronaut.management.health.indicator.HealthResult;
import io.micronaut.management.health.indicator.annotation.Readiness;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;


/**
 * Health indicator that reports the readiness based on the NGSI-LD broker's health state
 */
@Singleton
@Requires(beans = {HealthEndpoint.class, NgsiLdBrokerHealthConfiguration.class})
@Readiness
public class NgsiLdBrokerReadinessIndicator implements HealthIndicator {

	protected static final String NAME = "broker";
	private final NgsiLdBrokerHealthConfiguration brokerHealthConfiguration;
	private final HttpClient brokerHealthClient;

	public NgsiLdBrokerReadinessIndicator(NgsiLdBrokerHealthConfiguration brokerHealthConfiguration, @Client(id = "ngsihealth") HttpClient brokerHealthClient) {
		this.brokerHealthConfiguration = brokerHealthConfiguration;
		this.brokerHealthClient = brokerHealthClient;
	}

	@Override
	public Publisher<HealthResult> getResult() {
		return Mono.from(brokerHealthClient.exchange("", String.class))
				.map(response -> {
					HealthResult.Builder result = HealthResult.builder(NAME);
					if (brokerHealthConfiguration.allowedResponseCodes().contains(response.code())) {
						result
								.status(HealthStatus.UP);
					} else {
						result
								.status(HealthStatus.DOWN)
								.details(Map.of("error", response.getBody().orElse(""),
										"status", response.code()));
					}
					return result.build();
				})
				.onErrorResume(throwable -> Mono.just(HealthResult.builder(NAME).status(HealthStatus.DOWN).exception(throwable).build()));
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}
}