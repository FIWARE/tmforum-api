package org.fiware.tmforum.common.health;

import io.micronaut.core.io.socket.SocketUtils;
import io.micronaut.http.HttpStatus;
import io.micronaut.management.health.indicator.HealthResult;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.fiware.tmforum.common.mapping.StubBrokerController;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks that the readiness indicator still reaches the broker through the {@code ngsihealth} client on Micronaut 5
 * and maps its answer to UP/DOWN.
 */
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NgsiLdBrokerReadinessIndicatorTest implements TestPropertyProvider {

	public static final String SPEC_NAME = "NgsiLdBrokerReadinessIndicatorTest";

	@Inject
	private NgsiLdBrokerReadinessIndicator readinessIndicator;
	@Inject
	private StubBrokerHealthController stubBrokerHealthController;

	@Override
	public Map<String, String> getProperties() {
		int port = SocketUtils.findAvailableTcpPort();
		return Map.of(
				StubBrokerController.SPEC_NAME_PROPERTY, SPEC_NAME,
				"micronaut.server.port", String.valueOf(port),
				"micronaut.http.services.ngsihealth.url", "http://localhost:" + port,
				"micronaut.http.services.ngsihealth.path", StubBrokerHealthController.PATH,
				"endpoints.health.broker.enabled", "true",
				"endpoints.health.broker.allowed-response-codes", String.valueOf(HttpStatus.OK.getCode()));
	}

	@ParameterizedTest(name = "broker {0} -> {1}")
	@CsvSource({"OK, UP", "SERVICE_UNAVAILABLE, DOWN"})
	void reportsBrokerReadiness(HttpStatus brokerStatus, String expectedStatus) {
		stubBrokerHealthController.setStatus(brokerStatus);

		HealthResult result = Mono.from(readinessIndicator.getResult()).block();

		assertEquals(NgsiLdBrokerReadinessIndicator.NAME, result.getName());
		assertEquals(expectedStatus, result.getStatus().getName());
	}
}
