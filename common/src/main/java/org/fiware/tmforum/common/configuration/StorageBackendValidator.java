package org.fiware.tmforum.common.configuration;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.annotation.Order;
import io.micronaut.core.naming.NameUtils;
import io.micronaut.core.order.Ordered;
import io.micronaut.core.value.PropertyResolver;
import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.TmForumApplication;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Aborts startup on a contradictory or incomplete storage backend selection.
 * Off in the test environment; tests name their backend explicitly.
 * First among the eager beans, so it fails before Flyway's eager migration tries to reach PostgreSQL.
 */
@Slf4j
@Context
@Order(Ordered.HIGHEST_PRECEDENCE)
@Requires(notEnv = Environment.TEST)
public class StorageBackendValidator {

	static final String NGSI_URL_PROPERTY = "micronaut.http.services.ngsi.url";
	static final String SHIPPED_NGSI_URL = "http://localhost:1026";
	static final String GENERAL_PREFIX = "general.";
	/**
	 * The {@link NgsiLdBrokerProperties} fields, by their camelCase name; derived so a new field is checked too.
	 */
	static final List<String> BROKER_ONLY_PROPERTIES = Arrays.stream(NgsiLdBrokerProperties.class.getDeclaredFields())
			// synthetic: coverage agents (JaCoCo) add their own fields
			.filter(field -> !Modifier.isStatic(field.getModifiers()) && !field.isSynthetic())
			.map(Field::getName)
			// getDeclaredFields has no defined order; keeps the reported keys stable
			.sorted()
			.toList();

	private static final String SETTINGS_SEPARATOR = ", ";
	private static final String MULTIPLE_BACKENDS_MESSAGE = "More than one storage backend is selected in the "
			+ "Micronaut environments (%s). Set exactly one of %s in MICRONAUT_ENVIRONMENTS.";
	private static final String UNNAMED_BROKER_MESSAGE = "No storage backend is named in MICRONAUT_ENVIRONMENTS, "
			+ "so PostgreSQL is used by default, but broker settings are configured: %s. To keep using the broker, "
			+ "add %s or %s to MICRONAUT_ENVIRONMENTS. To use PostgreSQL, add %s to MICRONAUT_ENVIRONMENTS.";
	private static final String IGNORED_BROKER_PROPERTIES_MESSAGE = "The NGSI-LD broker settings {} are ignored, "
			+ "since {} is selected as storage backend. To use a broker, select {} or {} in MICRONAUT_ENVIRONMENTS.";
	private static final String DEFAULTED_BACKEND_MESSAGE = "No storage backend is named in MICRONAUT_ENVIRONMENTS, "
			+ "so {} is used by default and the data is read from and written to the configured PostgreSQL database. "
			+ "A deployment that used an NGSI-LD broker before has to name {} or {} in MICRONAUT_ENVIRONMENTS, "
			+ "otherwise it starts on an empty database.";
	private static final String SELECTED_BACKEND_MESSAGE = "{} is selected as storage backend.";

	public StorageBackendValidator(Environment environment) {
		validate(environment.getActiveNames(), environment);
	}

	/**
	 * Throws a {@link ConfigurationException} naming the offending setting; does nothing without a backend env.
	 * Returns the configured broker-only {@code general.*} keys that an explicitly named PostgreSQL backend ignores.
	 */
	static List<String> validate(Collection<String> activeEnvironments, PropertyResolver propertyResolver) {
		Set<StorageBackend> backends = StorageBackend.fromEnvironments(activeEnvironments);
		if (backends.size() > 1) {
			throw new ConfigurationException(String.format(MULTIPLE_BACKENDS_MESSAGE,
					environmentNames(backends), environmentNames(Set.of(StorageBackend.values()))));
		}
		if (!backends.contains(StorageBackend.POSTGRESQL)) {
			backends.stream().findFirst().ifPresent(backend ->
					log.info(SELECTED_BACKEND_MESSAGE, backend.environment()));
			return List.of();
		}
		List<String> brokerProperties = configuredBrokerProperties(propertyResolver);
		boolean defaulted = propertyResolver.getProperty(TmForumApplication.BACKEND_DEFAULTED_PROPERTY,
				Boolean.class, false);
		if (defaulted) {
			// the broker was the backend before this was configurable, so an upgrade that keeps its settings at the
			// shipped defaults is indistinguishable from a deployment that wants PostgreSQL; it must be visible
			log.warn(DEFAULTED_BACKEND_MESSAGE, StorageBackend.POSTGRESQL.environment(),
					StorageBackend.ORION_LD.environment(), StorageBackend.SCORPIO.environment());
			List<String> brokerSettings = new ArrayList<>();
			String ngsiUrl = propertyResolver.getProperty(NGSI_URL_PROPERTY, String.class, SHIPPED_NGSI_URL);
			if (!SHIPPED_NGSI_URL.equals(ngsiUrl)) {
				brokerSettings.add(NGSI_URL_PROPERTY + "=" + ngsiUrl);
			}
			brokerSettings.addAll(brokerProperties);
			if (!brokerSettings.isEmpty()) {
				throw new ConfigurationException(String.format(UNNAMED_BROKER_MESSAGE,
						String.join(SETTINGS_SEPARATOR, brokerSettings), StorageBackend.ORION_LD.environment(),
						StorageBackend.SCORPIO.environment(), StorageBackend.POSTGRESQL.environment()));
			}
			return List.of();
		}
		log.info(SELECTED_BACKEND_MESSAGE, StorageBackend.POSTGRESQL.environment());
		if (!brokerProperties.isEmpty()) {
			log.warn(IGNORED_BROKER_PROPERTIES_MESSAGE, String.join(SETTINGS_SEPARATOR, brokerProperties),
					StorageBackend.POSTGRESQL.environment(), StorageBackend.ORION_LD.environment(),
					StorageBackend.SCORPIO.environment());
		}
		return brokerProperties;
	}

	// env vars (GENERAL_REPLACE_ON_UPDATE) resolve to the kebab-case key, yaml may use either form
	private static List<String> configuredBrokerProperties(PropertyResolver propertyResolver) {
		return BROKER_ONLY_PROPERTIES.stream()
				.filter(name -> propertyResolver.containsProperty(GENERAL_PREFIX + name)
						|| propertyResolver.containsProperty(GENERAL_PREFIX + NameUtils.hyphenate(name)))
				.map(name -> GENERAL_PREFIX + NameUtils.hyphenate(name))
				.toList();
	}

	private static String environmentNames(Collection<StorageBackend> backends) {
		return backends.stream()
				.sorted()
				.map(StorageBackend::environment)
				.collect(Collectors.joining(","));
	}
}
