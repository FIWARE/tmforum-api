package org.fiware.tmforum.servicecatalog;

import io.github.wistefan.mapping.EntityVOMapper;
import io.github.wistefan.mapping.JavaObjectMapper;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.EntityId;
import io.github.wistefan.mapping.annotations.EntityType;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import io.micronaut.context.env.Environment;
import io.micronaut.http.HttpResponse;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.Getter;
import lombok.Setter;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.fiware.tmforum.service.AssociationSpecificationRef;
import org.fiware.tmforum.service.EntitySpecificationRelationship;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Pins that a broker keeps every field of an {@link EntitySpecificationRelationship}, including the nested
 * associationSpec, stored inline in the relationship, written and read with the production mappers.
 * Orion-LD returns the sub-attributes of a multi-instance relationship expanded, so they do not read back there (see
 * the ticket 88 plan, Step 1 result); that case runs on Scorpio only.
 */
@MicronautTest(packages = {"org.fiware.tmforum.servicecatalog"}, contextBuilder = FirstMatchConfigContextBuilder.class)
public class EntitySpecificationRelationshipIT extends AbstractApiIT {

	private static final String HOLDER_TYPE = "entity-spec-relationship-holder";
	private static final String FIRST_SPECIFICATION_ID = "urn:ngsi-ld:service-specification:first";
	private static final String SECOND_SPECIFICATION_ID = "urn:ngsi-ld:service-specification:second";
	private static final String ASSOCIATION_ID = "urn:ngsi-ld:association-specification:a";
	private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
	private static final String ORION_LD_ENVIRONMENT = "orion-ld";

	private final EntitiesApiClient entitiesApiClient;
	private final JavaObjectMapper javaObjectMapper;
	private final EntityVOMapper entityVOMapper;
	private final GeneralProperties generalProperties;
	private final Environment environment;

	public EntitySpecificationRelationshipIT(EntitiesApiClient entitiesApiClient, JavaObjectMapper javaObjectMapper,
			EntityVOMapper entityVOMapper, ObjectMapper objectMapper, GeneralProperties generalProperties,
			Environment environment) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.entitiesApiClient = entitiesApiClient;
		this.javaObjectMapper = javaObjectMapper;
		this.entityVOMapper = entityVOMapper;
		this.generalProperties = generalProperties;
		this.environment = environment;
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("relationships")
	public void everyFieldSurvivesTheBroker(String message, List<EntitySpecificationRelationship> relationships) {
		assumeFalse(relationships.size() > 1 && environment.getActiveNames().contains(ORION_LD_ENVIRONMENT),
				"Orion-LD does not compact the sub-attributes of a multi-instance relationship.");
		Holder holder = new Holder("urn:ngsi-ld:" + HOLDER_TYPE + ":" + UUID.randomUUID());
		holder.setEntitySpecRelationship(relationships);
		entitiesApiClient.createEntity(javaObjectMapper.toEntityVO(holder), null).block();

		EntityVO stored = entitiesApiClient.retrieveEntityById(holder.getId(), null, null, null, null,
						getLinkHeader(generalProperties.getContextUrl()))
				.map(HttpResponse::body)
				.block();
		Holder read = entityVOMapper.fromEntityVO(stored, Holder.class).block();

		assertNotNull(read.getEntitySpecRelationship(), message);
		// NGSI-LD does not guarantee the order of the instances of a multi-instance attribute
		assertEquals(objectMapper.valueToTree(sortedById(relationships)),
				objectMapper.valueToTree(sortedById(read.getEntitySpecRelationship())), message);
	}

	@Override
	protected String getEntityType() {
		return HOLDER_TYPE;
	}

	private static Stream<Arguments> relationships() {
		EntitySpecificationRelationship idOnlyAssociation = new EntitySpecificationRelationship(FIRST_SPECIFICATION_ID);
		idOnlyAssociation.setAssociationSpec(new AssociationSpecificationRef(ASSOCIATION_ID));
		return Stream.of(
				Arguments.of("An id-only relationship should be returned.",
						List.of(new EntitySpecificationRelationship(FIRST_SPECIFICATION_ID))),
				Arguments.of("An id-only associationSpec should be returned.", List.of(idOnlyAssociation)),
				Arguments.of("Every field should be returned.", List.of(full(FIRST_SPECIFICATION_ID))),
				Arguments.of("Every field of several relationships should be returned.",
						List.of(full(FIRST_SPECIFICATION_ID), full(SECOND_SPECIFICATION_ID))));
	}

	private static EntitySpecificationRelationship full(String specificationId) {
		AssociationSpecificationRef associationSpec = new AssociationSpecificationRef(ASSOCIATION_ID);
		associationSpec.setHref(URI.create("https://external.org/association-specification/a"));
		associationSpec.setName("association");
		associationSpec.setAtReferredType("AssociationSpecification");
		TimePeriod validFor = new TimePeriod();
		validFor.setStartDateTime(START);
		EntitySpecificationRelationship relationship = new EntitySpecificationRelationship(specificationId);
		relationship.setHref(URI.create("https://external.org/service-specification/" + specificationId));
		relationship.setName("specification " + specificationId);
		relationship.setAtReferredType("ServiceSpecification");
		relationship.setRelationshipType("dependency");
		relationship.setRole("parent");
		relationship.setAssociationSpec(associationSpec);
		relationship.setValidFor(validFor);
		return relationship;
	}

	private static List<EntitySpecificationRelationship> sortedById(List<EntitySpecificationRelationship> relationships) {
		return relationships.stream()
				.sorted(Comparator.comparing(EntitySpecificationRelationship::getId))
				.toList();
	}

	/** Entity holding the relationships read from their properties, as ServiceSpecification will once Step 3 flips it. */
	@MappingEnabled(entityType = HOLDER_TYPE)
	public static class Holder {

		@Getter(onMethod = @__({@EntityId}))
		private final URI id;

		@Getter(onMethod = @__({@EntityType}))
		private final String type = HOLDER_TYPE;

		@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "entitySpecRelationship")}))
		@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "entitySpecRelationship",
				fromProperties = true, targetClass = EntitySpecificationRelationship.class)}))
		private List<EntitySpecificationRelationship> entitySpecRelationship;

		public Holder(String id) {
			this.id = URI.create(id);
		}
	}
}
