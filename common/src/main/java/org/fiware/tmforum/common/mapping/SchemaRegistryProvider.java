package org.fiware.tmforum.common.mapping;

import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import com.networknt.schema.resource.ClasspathResourceLoader;
import com.networknt.schema.resource.IriResourceLoader;
import com.networknt.schema.resource.ResourceLoader;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.configuration.SchemaCacheProperties;

import java.time.Duration;

/**
 * Provides the (shared) {@link SchemaRegistry} to be used for validating entities against the schemas linked
 * via {@code @schemaLocation}.
 */
@Slf4j
@Factory
public class SchemaRegistryProvider {

	/**
	 * Dialect to be assumed if a schema does not declare its meta-schema.
	 */
	private static final SpecificationVersion DEFAULT_DIALECT = SpecificationVersion.DRAFT_2020_12;

	/**
	 * Create the schema registry. Remote schemas are retrieved through the (optionally caching) iri-loader,
	 * local ones through the classpath-loader. The registry is shared, since it is stateful and expensive to
	 * build - it is thread-safe and explicitly meant to be reused.
	 *
	 * <p>The registry-internal schema cache is intentionally disabled: it never expires its entries, thus it
	 * would keep the parsed schemas forever and the configured time-to-live would never take effect. Caching
	 * is handled by the {@link CachingResourceLoader} instead.</p>
	 *
	 * @param schemaCacheProperties configuration of the schema cache
	 * @return the registry to be used for schema validation
	 */
	@Singleton
	public SchemaRegistry schemaRegistry(SchemaCacheProperties schemaCacheProperties) {
		ResourceLoader iriResourceLoader = getIriResourceLoader(schemaCacheProperties);
		return SchemaRegistry.withDefaultDialect(DEFAULT_DIALECT,
				builder -> builder
						.resourceLoaders(resourceLoaders -> {
							resourceLoaders.add(new ClasspathResourceLoader());
							resourceLoaders.add(iriResourceLoader);
						})
						.schemaCacheEnabled(false));
	}

	/**
	 * Get the loader for remote schemas, wrapped into a caching loader if caching is enabled.
	 */
	private ResourceLoader getIriResourceLoader(SchemaCacheProperties schemaCacheProperties) {
		IriResourceLoader iriResourceLoader = new IriResourceLoader();
		Duration timeToLive = schemaCacheProperties.getTimeToLive();
		if (!schemaCacheProperties.isEnabled() || timeToLive == null || timeToLive.isZero()
				|| timeToLive.isNegative()) {
			log.info("Caching of schemas is disabled, they will be retrieved on every validation.");
			return iriResourceLoader;
		}
		log.info("Schemas will be cached for {}, at most {} of them.", timeToLive,
				schemaCacheProperties.getMaxSize());
		return new CachingResourceLoader(iriResourceLoader, timeToLive, schemaCacheProperties.getMaxSize());
	}
}
