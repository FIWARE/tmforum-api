package org.fiware.tmforum.common.configuration;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.value.PropertyResolver;
import org.fiware.tmforum.common.TmForumApplication;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Aborts startup on a contradictory or incomplete storage backend selection.
 * Off in the test environment; tests name their backend explicitly.
 */
@Context
@Requires(notEnv = Environment.TEST)
public class StorageBackendValidator {

	static final String NGSI_URL_PROPERTY = "micronaut.http.services.ngsi.url";
	static final String SHIPPED_NGSI_URL = "http://localhost:1026";

	private static final String MULTIPLE_BACKENDS_MESSAGE = "More than one storage backend is selected in the "
			+ "Micronaut environments (%s). Set exactly one of %s in MICRONAUT_ENVIRONMENTS.";
	private static final String UNNAMED_BROKER_MESSAGE = "No storage backend is named in MICRONAUT_ENVIRONMENTS, "
			+ "so PostgreSQL is used by default, but %s is set to %s. To keep using the broker, add %s or %s "
			+ "to MICRONAUT_ENVIRONMENTS. To use PostgreSQL, add %s to MICRONAUT_ENVIRONMENTS.";

	public StorageBackendValidator(Environment environment) {
		validate(environment.getActiveNames(), environment);
	}

	/**
	 * Throws a {@link ConfigurationException} naming the offending setting; does nothing without a backend env.
	 */
	static void validate(Collection<String> activeEnvironments, PropertyResolver propertyResolver) {
		Set<StorageBackend> backends = StorageBackend.fromEnvironments(activeEnvironments);
		if (backends.size() > 1) {
			throw new ConfigurationException(String.format(MULTIPLE_BACKENDS_MESSAGE,
					environmentNames(backends), environmentNames(Set.of(StorageBackend.values()))));
		}
		boolean defaulted = propertyResolver.getProperty(TmForumApplication.BACKEND_DEFAULTED_PROPERTY,
				Boolean.class, false);
		String ngsiUrl = propertyResolver.getProperty(NGSI_URL_PROPERTY, String.class, SHIPPED_NGSI_URL);
		if (defaulted && !SHIPPED_NGSI_URL.equals(ngsiUrl)) {
			throw new ConfigurationException(String.format(UNNAMED_BROKER_MESSAGE, NGSI_URL_PROPERTY, ngsiUrl,
					StorageBackend.ORION_LD.environment(), StorageBackend.SCORPIO.environment(),
					StorageBackend.POSTGRESQL.environment()));
		}
	}

	private static String environmentNames(Collection<StorageBackend> backends) {
		return backends.stream()
				.sorted()
				.map(StorageBackend::environment)
				.collect(Collectors.joining(","));
	}
}
