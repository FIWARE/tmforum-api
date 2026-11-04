package org.fiware.tmforum.common.mapping;

import io.github.wistefan.mapping.EntitiesRepository;
import io.github.wistefan.mapping.EntityVOMapper;
import io.github.wistefan.mapping.MappingException;
import io.github.wistefan.mapping.MappingProperties;
import io.github.wistefan.mapping.NgsiLdKeyCompactor;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import io.micronaut.context.annotation.Replaces;
import jakarta.inject.Singleton;
import org.fiware.ngsi.model.EntityVO;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.Optional;

/**
 * {@link EntityVOMapper} that maps the stubs from {@link #unresolvedTarget(URI)} to id-only relationship targets.
 * The library drops unresolved entries of relationship lists with two or more entries; a stub keeps them.
 */
@Singleton
@Replaces(EntityVOMapper.class)
public class TolerantEntityVOMapper extends EntityVOMapper {

	// marks a stub by type, since the library maps a copy of the target with the relationship's sub-properties merged in
	private static final String UNRESOLVED_TARGET_TYPE = "tmforum-unresolved-relationship-target";

	private final boolean strictRelationships;

	public TolerantEntityVOMapper(MappingProperties mappingProperties, ObjectMapper objectMapper,
								  EntitiesRepository entitiesRepository, NgsiLdKeyCompactor keyCompactor) {
		super(mappingProperties, objectMapper, entitiesRepository, keyCompactor);
		this.strictRelationships = mappingProperties.isStrictRelationships();
	}

	/**
	 * Placeholder for a relationship target that does not exist, for {@link EntitiesRepository#getEntities} to return.
	 */
	public static EntityVO unresolvedTarget(URI id) {
		return new EntityVO().id(id).type(UNRESOLVED_TARGET_TYPE);
	}

	/**
	 * Maps a stub with the target class's entity type, so only the relationship's own sub-properties are set.
	 * A target without {@link MappingEnabled} is built id-only through its String constructor, as the library does.
	 * In strict mode a stub fails the mapping, as the library does for an unresolved relationship.
	 */
	@Override
	public <T> Mono<T> fromEntityVO(EntityVO entityVO, Class<T> targetClass) {
		if (!UNRESOLVED_TARGET_TYPE.equals(entityVO.getType())) {
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
			return idOnly(entityVO.getId(), targetClass);
		}
		EntityVO typedTarget = new EntityVO().id(entityVO.getId()).type(entityType.get());
		Optional.ofNullable(entityVO.getAdditionalProperties())
				.ifPresent(properties -> properties.forEach(typedTarget::setAdditionalProperties));
		return super.fromEntityVO(typedTarget, targetClass);
	}

	private static <T> Mono<T> idOnly(URI id, Class<T> targetClass) {
		try {
			return Mono.just(targetClass.getDeclaredConstructor(String.class).newInstance(id.toString()));
		} catch (ReflectiveOperationException e) {
			return Mono.error(new MappingException(
					String.format("Was not able to instantiate %s with a string parameter.", targetClass), e));
		}
	}
}
