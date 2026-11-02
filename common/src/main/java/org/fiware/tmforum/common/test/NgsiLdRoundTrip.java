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

import java.util.List;

/**
 * Broker-less round trip of an entity through the NGSI-LD write mapping, JSON and the read mapping.
 * No relationship target resolves and relationships are strict, so a relationship only reads back what it stored inline.
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

	public static <T> T roundTrip(T entity, Class<T> entityClass) {
		MappingProperties mappingProperties = new MappingProperties();
		mappingProperties.setStrictRelationships(true);
		EntityVO written = new JavaObjectMapper(mappingProperties, OBJECT_MAPPER).toEntityVO(entity);
		EntityVO stored = OBJECT_MAPPER.readValue(OBJECT_MAPPER.writeValueAsString(written), EntityVO.class);
		return new EntityVOMapper(mappingProperties, OBJECT_MAPPER, NOTHING_RESOLVES)
				.fromEntityVO(stored, entityClass)
				.block();
	}

	/** JSON view of an object, to compare all of its serialized fields regardless of its equals. */
	public static JsonNode toTree(Object object) {
		return OBJECT_MAPPER.valueToTree(object);
	}
}
