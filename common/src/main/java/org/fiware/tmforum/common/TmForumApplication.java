package org.fiware.tmforum.common;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import io.micronaut.runtime.Micronaut;
import org.fiware.tmforum.common.configuration.StorageBackend;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Entry point of every module: {@link Micronaut#run(Class, String...)} plus PostgreSQL as the default backend.
 */
public final class TmForumApplication {

	/**
	 * Set to {@code true} when the runner added {@code postgresql} because no backend was named.
	 */
	public static final String BACKEND_DEFAULTED_PROPERTY = "tmforum.storage.backend-defaulted";

	private static final String ENVIRONMENT_SEPARATOR = ",";

	private TmForumApplication() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Starts the application context the way {@code Micronaut.run} does; adds the {@code postgresql} environment
	 * when neither {@code micronaut.environments} nor {@code MICRONAUT_ENVIRONMENTS} names a backend.
	 */
	public static ApplicationContext run(Class<?> mainClass, String... args) {
		Micronaut micronaut = Micronaut.build(args).classes(mainClass);
		if (needsDefaultBackend(System.getProperty(Environment.ENVIRONMENTS_PROPERTY),
				System.getenv(Environment.ENVIRONMENTS_ENV))) {
			micronaut.environments(StorageBackend.POSTGRESQL.environment())
					.properties(Map.of(BACKEND_DEFAULTED_PROPERTY, true));
		}
		return micronaut.start();
	}

	/**
	 * Whether no backend is named in the raw, comma-separated system property and environment variable values.
	 */
	static boolean needsDefaultBackend(String environmentsProperty, String environmentsVariable) {
		Set<String> environments = Stream.of(environmentsProperty, environmentsVariable)
				.filter(Objects::nonNull)
				.flatMap(value -> Arrays.stream(value.split(ENVIRONMENT_SEPARATOR)))
				.map(String::trim)
				.collect(Collectors.toSet());
		return StorageBackend.fromEnvironments(environments).isEmpty();
	}
}
