package org.fiware.tmforum.common.mapping;

import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.sun.net.httpserver.HttpServer;
import org.fiware.tmforum.common.configuration.SchemaCacheProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the schema registry provided for {@code @schemaLocation} validation, especially the caching of the
 * schemas it retrieves.
 */
class SchemaRegistryProviderTest {

	private static final String SCHEMA_PATH = "/the-schema.json";
	private static final String SCHEMA = """
			{
			  "$schema": "https://json-schema.org/draft/2020-12/schema",
			  "type": "object",
			  "properties": {
			    "policy": { "type": "object" }
			  },
			  "required": ["policy"]
			}""";
	private static final String VALID_ENTITY = "{\"policy\":{\"the\":\"policy\"}}";
	private static final String INVALID_ENTITY = "{\"somethingElse\":\"value\"}";
	private static final int ANY_FREE_PORT = 0;
	private static final int HTTP_OK = 200;
	private static final int NO_DELAY = 0;
	private static final long MAX_SIZE = 10L;

	private HttpServer schemaProvider;
	private AtomicInteger requestCount;

	@BeforeEach
	void startSchemaProvider() throws IOException {
		requestCount = new AtomicInteger(0);
		schemaProvider = HttpServer.create(new InetSocketAddress(ANY_FREE_PORT), 0);
		schemaProvider.createContext(SCHEMA_PATH, exchange -> {
			requestCount.incrementAndGet();
			byte[] body = SCHEMA.getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(HTTP_OK, body.length);
			try (OutputStream responseBody = exchange.getResponseBody()) {
				responseBody.write(body);
			}
		});
		schemaProvider.start();
	}

	@AfterEach
	void stopSchemaProvider() {
		schemaProvider.stop(NO_DELAY);
	}

	private String getSchemaAddress() {
		return "http://localhost:%d%s".formatted(schemaProvider.getAddress().getPort(), SCHEMA_PATH);
	}

	private static SchemaRegistry getRegistry(boolean cacheEnabled, Duration timeToLive) {
		SchemaCacheProperties schemaCacheProperties = new SchemaCacheProperties();
		schemaCacheProperties.setEnabled(cacheEnabled);
		schemaCacheProperties.setTimeToLive(timeToLive);
		schemaCacheProperties.setMaxSize(MAX_SIZE);
		return new SchemaRegistryProvider().schemaRegistry(schemaCacheProperties);
	}

	@DisplayName("A schema is only downloaded once while its cached, and on every usage if caching is off.")
	@ParameterizedTest
	@CsvSource({
			"true, PT30M, 3, 1",
			"true, PT30M, 1, 1",
			"false, PT30M, 3, 3",
			"true, PT0S, 3, 3"
	})
	void schemasAreOnlyDownloadedWhenNotCached(boolean cacheEnabled, Duration timeToLive, int numberOfValidations,
			int expectedDownloads) {
		SchemaRegistry schemaRegistry = getRegistry(cacheEnabled, timeToLive);

		for (int i = 0; i < numberOfValidations; i++) {
			Schema schema = schemaRegistry.getSchema(SchemaLocation.of(getSchemaAddress()));
			assertTrue(schema.validate(VALID_ENTITY, InputFormat.JSON).isEmpty(),
					"The valid entity should not lead to any error.");
		}

		assertEquals(expectedDownloads, requestCount.get(),
				"The schema should only have been downloaded when it was not cached.");
	}

	@DisplayName("The cached schema is still applied to the validated entities.")
	@ParameterizedTest
	@CsvSource({ "1", "2", "5" })
	void cachedSchemasAreStillValidating(int numberOfValidations) {
		SchemaRegistry schemaRegistry = getRegistry(true, Duration.ofMinutes(30));

		for (int i = 0; i < numberOfValidations; i++) {
			Schema schema = schemaRegistry.getSchema(SchemaLocation.of(getSchemaAddress()));
			List<Error> errors = schema.validate(INVALID_ENTITY, InputFormat.JSON);
			assertFalse(errors.isEmpty(), "The invalid entity should be rejected, also when the schema is cached.");
		}

		assertEquals(1, requestCount.get(), "The schema should only have been downloaded once.");
	}
}
