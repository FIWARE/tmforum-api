package org.fiware.tmforum.common.mapping;

import io.github.wistefan.mapping.MappingProperties;
import io.micronaut.context.env.Environment;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Turns off the mapping library's strict relationship handling, unless an operator configured it explicitly.
 * The write path accepts references to entities this deployment does not hold, so the read path has to tolerate
 * their unresolvable targets ({@link TolerantEntityVOMapper}). The library default ({@code true}) would fail the
 * GET of an entity that was created successfully, also for a deployment that replaces the shipped
 * {@code application.yaml}.
 */
@Slf4j
@Singleton
public class StrictRelationshipsDefault implements BeanCreatedEventListener<MappingProperties> {

	/**
	 * The configured property, in the spellings Micronaut resolves it under.
	 */
	static final String[] STRICT_RELATIONSHIPS_PROPERTIES = {"mapping.strict-relationships",
			"mapping.strictRelationships"};

	private final Environment environment;

	public StrictRelationshipsDefault(Environment environment) {
		this.environment = environment;
	}

	@Override
	public MappingProperties onCreated(BeanCreatedEvent<MappingProperties> event) {
		MappingProperties mappingProperties = event.getBean();
		if (isConfigured()) {
			if (mappingProperties.isStrictRelationships()) {
				log.warn("{} is enabled. References to entities held by another deployment are accepted on write, "
						+ "but fail the read of the referencing entity.", STRICT_RELATIONSHIPS_PROPERTIES[0]);
			}
			return mappingProperties;
		}
		mappingProperties.setStrictRelationships(false);
		return mappingProperties;
	}

	private boolean isConfigured() {
		for (String property : STRICT_RELATIONSHIPS_PROPERTIES) {
			if (environment.containsProperty(property)) {
				return true;
			}
		}
		return false;
	}
}
