package org.fiware.tmforum.common.configuration;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.value.MapPropertyResolver;
import org.fiware.tmforum.common.TmForumApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the fail-fast checks of the storage backend selection.
 */
class StorageBackendValidatorTest {

	private static final String BROKER_URL = "http://orion-ld:1026";
	private static final String SELECTION_SETTING = "MICRONAUT_ENVIRONMENTS";
	private static final String MULTIPLE_BACKENDS_TEXT = "More than one storage backend";

	static Stream<Arguments> validSelections() {
		return Stream.of(
				Arguments.of(List.of(), false, null),
				Arguments.of(List.of("in-memory"), false, BROKER_URL),
				Arguments.of(List.of("postgresql"), false, null),
				Arguments.of(List.of("redis", "orion-ld"), false, BROKER_URL),
				Arguments.of(List.of("in-memory", "scorpio"), false, BROKER_URL),
				// postgresql named explicitly: a leftover broker url is ignored
				Arguments.of(List.of("redis", "postgresql"), false, BROKER_URL),
				Arguments.of(List.of("redis", "postgresql"), true, StorageBackendValidator.SHIPPED_NGSI_URL),
				Arguments.of(List.of("postgresql"), true, null));
	}

	@DisplayName("Consistent selections are accepted.")
	@ParameterizedTest(name = "{0}, defaulted: {1}, ngsi url: {2}")
	@MethodSource("validSelections")
	void validSelectionIsAccepted(List<String> environments, boolean defaulted, String ngsiUrl) {
		assertDoesNotThrow(() -> StorageBackendValidator.validate(environments, resolver(defaulted, ngsiUrl)),
				"A consistent selection should be accepted.");
	}

	static Stream<Arguments> invalidSelections() {
		return Stream.of(
				Arguments.of(List.of("postgresql", "orion-ld"), false, null, SELECTION_SETTING),
				Arguments.of(List.of("orion-ld", "scorpio"), false, BROKER_URL, SELECTION_SETTING),
				Arguments.of(List.of("redis", "postgresql", "scorpio"), false, null, SELECTION_SETTING),
				// the upgrade case: MICRONAUT_ENVIRONMENTS=redis plus a broker url, postgresql added by the runner
				Arguments.of(List.of("redis", "postgresql"), true, BROKER_URL,
						StorageBackendValidator.NGSI_URL_PROPERTY),
				Arguments.of(List.of("postgresql"), true, "http://broker:1026",
						StorageBackendValidator.NGSI_URL_PROPERTY));
	}

	@DisplayName("Contradictory or incomplete selections are rejected with the offending setting.")
	@ParameterizedTest(name = "{0}, defaulted: {1}, ngsi url: {2}")
	@MethodSource("invalidSelections")
	void invalidSelectionIsRejected(List<String> environments, boolean defaulted, String ngsiUrl,
			String expectedSetting) {
		ConfigurationException exception = assertThrows(ConfigurationException.class,
				() -> StorageBackendValidator.validate(environments, resolver(defaulted, ngsiUrl)),
				"An inconsistent selection should be rejected.");
		assertTrue(exception.getMessage().contains(expectedSetting),
				"The message should name the offending setting.");
	}

	@DisplayName("A context with two backends does not start.")
	@Test
	void contextWithTwoBackendsDoesNotStart() {
		Exception exception = assertThrows(Exception.class, () -> ApplicationContext.builder()
				.deduceEnvironment(false)
				.deduceCloudEnvironment(false)
				.environments(StorageBackend.ORION_LD.environment(), StorageBackend.POSTGRESQL.environment())
				.start()
				.close(), "The context should not start with two backends.");
		Throwable rootCause = rootCause(exception);
		assertInstanceOf(ConfigurationException.class, rootCause, "The validator should abort the start.");
		assertTrue(rootCause.getMessage().contains(MULTIPLE_BACKENDS_TEXT),
				"The start should fail on the two-backend check, not on another bean's configuration.");
	}

	private static Throwable rootCause(Throwable throwable) {
		Throwable cause = throwable;
		while (cause.getCause() != null && cause.getCause() != cause) {
			cause = cause.getCause();
		}
		return cause;
	}

	private static MapPropertyResolver resolver(boolean defaulted, String ngsiUrl) {
		Map<String, Object> properties = new HashMap<>();
		properties.put(TmForumApplication.BACKEND_DEFAULTED_PROPERTY, defaulted);
		if (ngsiUrl != null) {
			properties.put(StorageBackendValidator.NGSI_URL_PROPERTY, ngsiUrl);
		}
		return new MapPropertyResolver(properties);
	}
}
