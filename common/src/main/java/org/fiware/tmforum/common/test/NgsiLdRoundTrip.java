package org.fiware.tmforum.common.test;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.wistefan.mapping.EntitiesRepository;
import io.github.wistefan.mapping.EntityVOMapper;
import io.github.wistefan.mapping.JavaObjectMapper;
import io.github.wistefan.mapping.MappingProperties;
import io.github.wistefan.mapping.NgsiLdMappingModule;
import org.fiware.ngsi.model.EntityVO;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Broker-less round trip of an entity through the NGSI-LD write mapping, JSON and the read mapping.
 * Unless targets are given, nothing resolves and relationships are strict, so a relationship only reads back what it stored inline.
 * Uses a hand-built mapper with only {@link NgsiLdMappingModule}, not the production one: it pins the annotation
 * wiring, not production serialization parity.
 */
public final class NgsiLdRoundTrip {

	// non_null like every module's application.yaml, otherwise a null field of a structured value is written as {}
	private static final JsonMapper OBJECT_MAPPER = JsonMapper.builder()
			.addModule(new NgsiLdMappingModule())
			.changeDefaultPropertyInclusion(inclusion -> inclusion.withValueInclusion(JsonInclude.Include.NON_NULL))
			.build();
	private static final EntitiesRepository NOTHING_RESOLVES = entityIds -> Mono.just(List.of());

	private NgsiLdRoundTrip() {
	}

	/** Writes, serializes and reads back the entity. Blocks, and throws if the read mapping fails. */
	public static <T> T roundTrip(T entity, Class<T> entityClass) {
		return read(store(entity), entityClass, NOTHING_RESOLVES);
	}

	/**
	 * Like {@link #roundTrip(Object, Class)}, but a relationship without {@code fromProperties} resolves against the
	 * stored {@code targets}, as on a broker, so its sub-properties are merged over the target's.
	 */
	public static <T> T roundTrip(T entity, Class<T> entityClass, Object... targets) {
		Map<URI, EntityVO> stored = Arrays.stream(targets)
				.map(NgsiLdRoundTrip::store)
				.collect(Collectors.toMap(EntityVO::getId, Function.identity()));
		EntitiesRepository resolving = entityIds -> Mono.just(
				entityIds.stream().map(stored::get).filter(Objects::nonNull).toList());
		return read(store(entity), entityClass, resolving);
	}

	private static EntityVO store(Object entity) {
		EntityVO written = new JavaObjectMapper(strictMappingProperties(), OBJECT_MAPPER).toEntityVO(entity);
		return OBJECT_MAPPER.readValue(OBJECT_MAPPER.writeValueAsString(written), EntityVO.class);
	}

	private static <T> T read(EntityVO stored, Class<T> entityClass, EntitiesRepository entitiesRepository) {
		return new EntityVOMapper(strictMappingProperties(), OBJECT_MAPPER, entitiesRepository)
				.fromEntityVO(stored, entityClass)
				.block();
	}

	private static MappingProperties strictMappingProperties() {
		MappingProperties mappingProperties = new MappingProperties();
		mappingProperties.setStrictRelationships(true);
		return mappingProperties;
	}

	/** JSON view of an object, to compare all of its serialized fields regardless of its equals. */
	public static JsonNode toTree(Object object) {
		return OBJECT_MAPPER.valueToTree(object);
	}
}
