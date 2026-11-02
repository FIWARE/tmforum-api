package org.fiware.tmforum.common.domain;

import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.EntityId;
import io.github.wistefan.mapping.annotations.EntityType;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.test.NgsiLdRoundTrip;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins that every {@link ConstraintRef} field is stored inline in the relationship, so it reads back without the target.
 */
class ConstraintRefRoundTripTest {

	private static final String HOLDER_TYPE = "holder";
	private static final String HOLDER_ID = "urn:ngsi-ld:holder:h";
	private static final String CONSTRAINT_ID = "urn:ngsi-ld:constraint:c";

	@ParameterizedTest(name = "{0}")
	@MethodSource("constraints")
	public void everyFieldSurvivesTheRoundTrip(String description, ConstraintRef constraint) {
		Holder holder = new Holder(HOLDER_ID);
		holder.setConstraint(constraint);

		Holder read = NgsiLdRoundTrip.roundTrip(holder, Holder.class);

		assertEquals(NgsiLdRoundTrip.toTree(constraint), NgsiLdRoundTrip.toTree(read.getConstraint()), description);
	}

	private static Stream<Arguments> constraints() {
		ConstraintRef full = new ConstraintRef(CONSTRAINT_ID);
		full.setHref(URI.create("https://external.org/constraint/c"));
		full.setName("constraint");
		full.setAtReferredType("constraint");
		full.setVersion("1.0");
		return Stream.of(
				Arguments.of("id only", new ConstraintRef(CONSTRAINT_ID)),
				Arguments.of("every field", full));
	}

	/** Entity holding a {@link ConstraintRef} read from its properties, as every relationship will be. */
	@MappingEnabled(entityType = HOLDER_TYPE)
	public static class Holder {

		@Getter(onMethod = @__({@EntityId}))
		private final URI id;

		@Getter(onMethod = @__({@EntityType}))
		private final String type = HOLDER_TYPE;

		@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "constraint")}))
		@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "constraint",
				fromProperties = true, targetClass = ConstraintRef.class)}))
		private ConstraintRef constraint;

		public Holder(String id) {
			this.id = URI.create(id);
		}
	}
}
