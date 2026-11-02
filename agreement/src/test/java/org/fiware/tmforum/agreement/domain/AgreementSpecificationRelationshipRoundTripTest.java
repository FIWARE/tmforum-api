package org.fiware.tmforum.agreement.domain;

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
 * Pins that every field of {@link AgreementSpecificationRelationship} is stored inline in the relationship, so it reads back without the target.
 */
class AgreementSpecificationRelationshipRoundTripTest {

	private static final String HOLDER_TYPE = "holder";
	private static final String HOLDER_ID = "urn:ngsi-ld:holder:h";
	private static final String SPECIFICATION_ID = "urn:ngsi-ld:agreement-specification:s";
	private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
	private static final Instant END = Instant.parse("2027-01-01T00:00:00Z");

	@ParameterizedTest(name = "{0}")
	@MethodSource("refs")
	public void everyFieldSurvivesTheRoundTrip(String description, Holder holder, Function<Holder, Object> ref) {
		Holder read = NgsiLdRoundTrip.roundTrip(holder, Holder.class);

		assertEquals(NgsiLdRoundTrip.toTree(ref.apply(holder)), NgsiLdRoundTrip.toTree(ref.apply(read)), description);
	}

	private static Stream<Arguments> refs() {
		TimePeriod validFor = new TimePeriod();
		validFor.setStartDateTime(START);
		validFor.setEndDateTime(END);
		AgreementSpecificationRelationship full = new AgreementSpecificationRelationship(SPECIFICATION_ID);
		full.setHref(URI.create("https://external.org/agreement-specification/s"));
		full.setName("specification");
		full.setAtReferredType(AgreementSpecification.TYPE_AGREEMENT_SPECIFICATION);
		full.setRelationshipType("substitution");
		full.setValidFor(validFor);
		return Stream.of(
				Arguments.of("id only", holder(new AgreementSpecificationRelationship(SPECIFICATION_ID)),
						(Function<Holder, Object>) Holder::getSpecificationRelationship),
				Arguments.of("every field", holder(full),
						(Function<Holder, Object>) Holder::getSpecificationRelationship));
	}

	private static Holder holder(AgreementSpecificationRelationship specificationRelationship) {
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
				fromProperties = true, targetClass = AgreementSpecificationRelationship.class)}))
		private AgreementSpecificationRelationship specificationRelationship;

		public Holder(String id) {
			this.id = URI.create(id);
		}
	}
}
