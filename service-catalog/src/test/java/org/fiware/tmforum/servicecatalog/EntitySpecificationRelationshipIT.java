package org.fiware.tmforum.servicecatalog;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.fiware.tmforum.service.AssociationSpecificationRef;
import org.fiware.tmforum.service.EntitySpecificationRelationship;
import org.fiware.tmforum.servicecatalog.domain.ServiceSpecification;
import org.fiware.tmforum.storage.EntityRepository;
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

/**
 * Pins that the storage keeps every field of an {@link EntitySpecificationRelationship}, including the nested
 * associationSpec, stored inline in a {@link ServiceSpecification}, written and read through the storage port.
 */
@MicronautTest(packages = {"org.fiware.tmforum.servicecatalog"}, contextBuilder = FirstMatchConfigContextBuilder.class)
public class EntitySpecificationRelationshipIT extends AbstractApiIT {

	private static final String FIRST_SPECIFICATION_ID = "urn:ngsi-ld:service-specification:first";
	private static final String SECOND_SPECIFICATION_ID = "urn:ngsi-ld:service-specification:second";
	private static final String ASSOCIATION_ID = "urn:ngsi-ld:association-specification:a";
	private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

	private final EntityRepository entityRepository;

	public EntitySpecificationRelationshipIT(EntitiesApiClient entitiesApiClient, EntityRepository entityRepository,
			ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.entityRepository = entityRepository;
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("relationships")
	public void everyFieldSurvivesTheStorage(String message, List<EntitySpecificationRelationship> relationships) {
		ServiceSpecification specification = new ServiceSpecification(UUID.randomUUID().toString());
		specification.setEntitySpecRelationship(relationships);
		entityRepository.createDomainEntity(specification).block();

		ServiceSpecification read = entityRepository.get(specification.getId(), ServiceSpecification.class).block();

		assertNotNull(read.getEntitySpecRelationship(), message);
		// NGSI-LD does not guarantee the order of the instances of a multi-instance attribute
		assertEquals(objectMapper.valueToTree(sortedById(relationships)),
				objectMapper.valueToTree(sortedById(read.getEntitySpecRelationship())), message);
	}

	@Override
	protected String getEntityType() {
		return ServiceSpecification.TYPE_SERVICE_SPECIFICATION;
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
}
