package org.fiware.tmforum.usagemanagement.domain;

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
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins that every field of {@link UsageSpecificationRef} is stored inline in the relationship, so it reads back without the target.
 */
class UsageSpecificationRefRoundTripTest {

	private static final String HOLDER_TYPE = "holder";
	private static final String HOLDER_ID = "urn:ngsi-ld:holder:h";
	private static final String USAGE_SPECIFICATION_ID = "urn:ngsi-ld:usage-specification:u";

	@ParameterizedTest(name = "{0}")
	@MethodSource("refs")
	public void everyFieldSurvivesTheRoundTrip(String description, Holder holder, Function<Holder, Object> ref) {
		Holder read = NgsiLdRoundTrip.roundTrip(holder, Holder.class);

		assertEquals(NgsiLdRoundTrip.toTree(ref.apply(holder)), NgsiLdRoundTrip.toTree(ref.apply(read)), description);
	}

	private static Stream<Arguments> refs() {
		UsageSpecificationRef full = new UsageSpecificationRef(USAGE_SPECIFICATION_ID);
		full.setHref(URI.create("https://external.org/usage-specification/u"));
		full.setName("usage specification");
		full.setAtReferredType(UsageSpecification.TYPE_USP);
		return Stream.of(
				Arguments.of("id only", holder(new UsageSpecificationRef(USAGE_SPECIFICATION_ID)),
						(Function<Holder, Object>) Holder::getUsageSpecification),
				Arguments.of("every field", holder(full), (Function<Holder, Object>) Holder::getUsageSpecification));
	}

	private static Holder holder(UsageSpecificationRef usageSpecification) {
		Holder holder = new Holder(HOLDER_ID);
		holder.setUsageSpecification(usageSpecification);
		return holder;
	}

	/** Entity holding each ref read from its properties, as every relationship will be. */
	@MappingEnabled(entityType = HOLDER_TYPE)
	public static class Holder {

		@Getter(onMethod = @__({@EntityId}))
		private final URI id;

		@Getter(onMethod = @__({@EntityType}))
		private final String type = HOLDER_TYPE;

		@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "usageSpecification")}))
		@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "usageSpecification",
				fromProperties = true, targetClass = UsageSpecificationRef.class)}))
		private UsageSpecificationRef usageSpecification;

		public Holder(String id) {
			this.id = URI.create(id);
		}
	}
}
