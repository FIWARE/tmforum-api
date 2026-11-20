package org.fiware.tmforum.migration.postgresql;

import io.micronaut.context.ApplicationContext;
import org.fiware.tmforum.common.mapping.ForbiddenCharacterEscaper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class BrokerSourceTest {

	private static final String READ_TIMEOUT = "1s";
	// well above the read timeout, well below a hang
	private static final Duration TEST_TIMEOUT = Duration.ofSeconds(20);
	private static final String LOOPBACK_URL = "http://127.0.0.1:%d";

	private ApplicationContext context;

	@AfterEach
	void closeAndResetEscaper() {
		if (context != null) {
			context.close();
		}
		ForbiddenCharacterEscaper.setEnabled(true);
	}

	@Test
	void listingTheTypesOfASilentBrokerTimesOut() throws IOException {
		// the kernel accepts connections into the backlog, but nothing ever answers
		try (ServerSocket silentBroker = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
			BrokerSource source = new BrokerSource(sourceContext(silentBroker.getLocalPort()));

			IllegalStateException failure = assertTimeoutPreemptively(TEST_TIMEOUT,
					() -> assertThrows(IllegalStateException.class, source::types));

			assertInstanceOf(HttpTimeoutException.class, failure.getCause());
		}
	}

	private ApplicationContext sourceContext(int port) {
		MigrationContexts contexts = new MigrationContexts();
		new CommandLine(contexts).parseArgs("--broker-url", String.format(LOOPBACK_URL, port));
		Map<String, Object> properties = contexts.sourceProperties();
		properties.put(BrokerSource.READ_TIMEOUT_PROPERTY, READ_TIMEOUT);
		context = ApplicationContext.builder()
				.deduceEnvironment(false)
				.deduceCloudEnvironment(false)
				.environments(contexts.sourceEnvironments().toArray(String[]::new))
				.properties(properties)
				.start();
		return context;
	}
}
