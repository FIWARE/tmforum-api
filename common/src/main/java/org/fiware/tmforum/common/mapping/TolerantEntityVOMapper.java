package org.fiware.tmforum.common.mapping;

import io.github.wistefan.mapping.EntitiesRepository;
import io.github.wistefan.mapping.EntityVOMapper;
import io.github.wistefan.mapping.MappingException;
import io.github.wistefan.mapping.MappingProperties;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import io.micronaut.context.annotation.Replaces;
import jakarta.inject.Singleton;
import org.fiware.ngsi.model.AdditionalPropertyVO;
import org.fiware.ngsi.model.EntityVO;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * {@link EntityVOMapper} that maps the stubs from {@link #unresolvedTarget(URI)} to id-only relationship targets.
 * The library drops unresolved entries of relationship lists with two or more entries; a stub keeps them.
 */
@Singleton
@Replaces(EntityVOMapper.class)
public class TolerantEntityVOMapper extends EntityVOMapper {

	private final boolean strictRelationships;

	public TolerantEntityVOMapper(MappingProperties mappingProperties, ObjectMapper objectMapper,
								  EntitiesRepository entitiesRepository) {
		super(mappingProperties, objectMapper, entitiesRepository);
		this.strictRelationships = mappingProperties.isStrictRelationships();
	}

	/**
	 * Placeholder for a relationship target that does not exist, for {@link EntitiesRepository#getEntities} to return.
	 */
	public static EntityVO unresolvedTarget(URI id) {
		return new UnresolvedTarget().id(id);
	}

	/**
	 * Maps a stub with the target class's entity type, so only the relationship's own sub-properties are set.
	 * In strict mode a stub fails the mapping, as the library does for an unresolved relationship.
	 */
	@Override
	public <T> Mono<T> fromEntityVO(EntityVO entityVO, Class<T> targetClass) {
		if (!(entityVO instanceof UnresolvedTarget)) {
			return super.fromEntityVO(entityVO, targetClass);
		}
		if (strictRelationships) {
			return Mono.error(new MappingException(
					String.format("Was not able to resolve the relationship %s", entityVO.getId())));
		}
		Optional<String> entityType = Optional.ofNullable(targetClass.getAnnotation(MappingEnabled.class))
				.map(MappingEnabled::entityType)
				.filter(types -> types.length > 0)
				.map(types -> types[0]);
		if (entityType.isEmpty()) {
			return super.fromEntityVO(entityVO, targetClass);
		}
		EntityVO typedTarget = new EntityVO().id(entityVO.getId()).type(entityType.get());
		entityVO.getAdditionalProperties().forEach(typedTarget::setAdditionalProperties);
		return super.fromEntityVO(typedTarget, targetClass);
	}

	/**
	 * The library merges a relationship's sub-properties into its target's map, so the stub needs a mutable one.
	 */
	private static final class UnresolvedTarget extends EntityVO {

		private final Map<String, AdditionalPropertyVO> mergedProperties = new HashMap<>();

		@Override
		public Map<String, AdditionalPropertyVO> getAdditionalProperties() {
			return mergedProperties;
		}
	}
}
