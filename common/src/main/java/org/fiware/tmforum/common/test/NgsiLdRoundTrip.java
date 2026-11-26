package org.fiware.tmforum.common.test;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.wistefan.mapping.EntityVOMapper;
import io.github.wistefan.mapping.JavaObjectMapper;
import io.github.wistefan.mapping.MappingProperties;
import io.github.wistefan.mapping.NgsiLdMappingModule;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.mapping.InlineReferencesEntitiesRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Broker-less round trip of an entity through the NGSI-LD write mapping, JSON and the read mapping.
 * Reads through the production {@link InlineReferencesEntitiesRepository}, so a relationship reads back only what it
 * stored inline, and one whose setter lacks {@code fromProperties = true} fails the read.
 * Uses a hand-built mapper with only {@link NgsiLdMappingModule}, not the production one: it pins the annotation
 * wiring, not production serialization parity.
 */
public final class NgsiLdRoundTrip {

	// non_null like every module's application.yaml, otherwise a null field of a structured value is written as {}
	private static final JsonMapper OBJECT_MAPPER = JsonMapper.builder()
			.addModule(new NgsiLdMappingModule())
			.changeDefaultPropertyInclusion(inclusion -> inclusion.withValueInclusion(JsonInclude.Include.NON_NULL))
			.build();

	private NgsiLdRoundTrip() {
	}

	/** Writes, serializes and reads back the entity. Blocks, and throws if the read mapping fails. */
	public static <T> T roundTrip(T entity, Class<T> entityClass) {
		return read(store(entity), entityClass);
	}

	private static EntityVO store(Object entity) {
		EntityVO written = new JavaObjectMapper(strictMappingProperties(), OBJECT_MAPPER).toEntityVO(entity);
		return OBJECT_MAPPER.readValue(OBJECT_MAPPER.writeValueAsString(written), EntityVO.class);
	}

	private static <T> T read(EntityVO stored, Class<T> entityClass) {
		return new EntityVOMapper(strictMappingProperties(), OBJECT_MAPPER, new InlineReferencesEntitiesRepository())
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
