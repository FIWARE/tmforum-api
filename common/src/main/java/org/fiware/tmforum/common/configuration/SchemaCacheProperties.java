package org.fiware.tmforum.common.configuration;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

import java.time.Duration;

/**
 * Properties for the cache of JSON Schemas, downloaded for validating entities that provide an
 * {@code @schemaLocation}. Without such a cache, every single request referencing a schema would lead to a
 * (blocking) download of that schema.
 */
@ConfigurationProperties(SchemaCacheProperties.PREFIX)
@Data
public class SchemaCacheProperties {

	/**
	 * Configuration prefix of the schema cache.
	 */
	public static final String PREFIX = "general.schema-cache";

	/**
	 * Default state of the cache. Caching is enabled, since schemas are expected to be rather stable.
	 */
	private static final boolean DEFAULT_ENABLED = true;

	/**
	 * Default time a downloaded schema is considered to be valid. Its a compromise between the cost of
	 * re-downloading the schema and the time it takes until changes at the schema provider are picked up.
	 */
	private static final Duration DEFAULT_TIME_TO_LIVE = Duration.ofMinutes(30);

	/**
	 * Default number of schemas to be kept in the cache. Since the schema addresses are provided by the clients,
	 * the cache needs to be bounded to prevent unlimited growth.
	 */
	private static final long DEFAULT_MAX_SIZE = 100L;

	/**
	 * Whether downloaded schemas should be cached. If disabled, every validation downloads the schema again.
	 */
	private boolean enabled = DEFAULT_ENABLED;

	/**
	 * Time a downloaded schema is kept in the cache, before it is downloaded again. A duration of zero disables
	 * caching, e.g. has the same effect as {@code enabled: false}.
	 */
	private Duration timeToLive = DEFAULT_TIME_TO_LIVE;

	/**
	 * Maximum number of schemas to be kept in the cache. If more schemas are used, the least recently used ones
	 * are evicted.
	 */
	private long maxSize = DEFAULT_MAX_SIZE;
}
