package org.fiware.tmforum.service;

import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.EntityId;
import io.github.wistefan.mapping.annotations.EntityType;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.common.test.NgsiLdRoundTrip;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.time.Instant;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins that every field of {@link EntitySpecificationRelationship}, including its nested {@link AssociationSpecificationRef} is stored inline in the relationship, so it reads back without the target.
 */
class EntitySpecificationRelationshipRoundTripTest {

	private static final String HOLDER_TYPE = "holder";
	private static final String HOLDER_ID = "urn:ngsi-ld:holder:h";
	private static final String SPECIFICATION_ID = "urn:ngsi-ld:service-specification:s";
	private static final String ASSOCIATION_ID = "urn:ngsi-ld:association-specification:a";
	private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

	@ParameterizedTest(name = "{0}")
	@MethodSource("refs")
	public void everyFieldSurvivesTheRoundTrip(String description, Holder holder, Function<Holder, Object> ref) {
		Holder read = NgsiLdRoundTrip.roundTrip(holder, Holder.class);

		assertEquals(NgsiLdRoundTrip.toTree(ref.apply(holder)), NgsiLdRoundTrip.toTree(ref.apply(read)), description);
	}

	private static Stream<Arguments> refs() {
		AssociationSpecificationRef associationSpec = new AssociationSpecificationRef(ASSOCIATION_ID);
		associationSpec.setHref(URI.create("https://external.org/association-specification/a"));
		associationSpec.setName("association");
		associationSpec.setAtReferredType("AssociationSpecification");
		TimePeriod validFor = new TimePeriod();
		validFor.setStartDateTime(START);
		EntitySpecificationRelationship full = new EntitySpecificationRelationship(SPECIFICATION_ID);
		full.setHref(URI.create("https://external.org/service-specification/s"));
		full.setName("specification");
		full.setAtReferredType("ServiceSpecification");
		full.setRelationshipType("dependency");
		full.setRole("parent");
		full.setAssociationSpec(associationSpec);
		full.setValidFor(validFor);
		EntitySpecificationRelationship idOnlyAssociation = new EntitySpecificationRelationship(SPECIFICATION_ID);
		idOnlyAssociation.setAssociationSpec(new AssociationSpecificationRef(ASSOCIATION_ID));
		return Stream.of(
				Arguments.of("id only", holder(new EntitySpecificationRelationship(SPECIFICATION_ID)),
						(Function<Holder, Object>) Holder::getSpecificationRelationship),
				Arguments.of("id-only association", holder(idOnlyAssociation),
						(Function<Holder, Object>) Holder::getSpecificationRelationship),
				Arguments.of("every field", holder(full),
						(Function<Holder, Object>) Holder::getSpecificationRelationship));
	}

	private static Holder holder(EntitySpecificationRelationship specificationRelationship) {
		Holder holder = new Holder(HOLDER_ID);
		holder.setSpecificationRelationship(specificationRelationship);
		return holder;
	}

	/** Entity holding each ref read from its properties, as every relationship will be. */
	@MappingEnabled(entityType = HOLDER_TYPE)
	public static class Holder {

		@Getter(onMethod = @__({@EntityId}))
		private final URI id;

		@Getter(onMethod = @__({@EntityType}))
		private final String type = HOLDER_TYPE;

		@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "specificationRelationship")}))
		@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "specificationRelationship",
				fromProperties = true, targetClass = EntitySpecificationRelationship.class)}))
		private EntitySpecificationRelationship specificationRelationship;

		public Holder(String id) {
			this.id = URI.create(id);
		}
	}
}
