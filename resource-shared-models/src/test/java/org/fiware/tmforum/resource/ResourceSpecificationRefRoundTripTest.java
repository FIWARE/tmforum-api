package org.fiware.tmforum.resource;

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
 * Pins that every {@link ResourceSpecificationRef} field, including its version, is stored inline in the relationship.
 */
class ResourceSpecificationRefRoundTripTest {

	private static final String HOLDER_TYPE = "holder";
	private static final String HOLDER_ID = "urn:ngsi-ld:holder:h";
	private static final String SPECIFICATION_ID = "urn:ngsi-ld:resource-specification:s";

	@ParameterizedTest(name = "{0}")
	@MethodSource("specifications")
	public void everyFieldSurvivesTheRoundTrip(String description, ResourceSpecificationRef specification) {
		Holder holder = new Holder(HOLDER_ID);
		holder.setResourceSpecification(specification);

		Holder read = NgsiLdRoundTrip.roundTrip(holder, Holder.class);

		assertEquals(NgsiLdRoundTrip.toTree(specification), NgsiLdRoundTrip.toTree(read.getResourceSpecification()),
				description);
	}

	private static Stream<Arguments> specifications() {
		ResourceSpecificationRef full = new ResourceSpecificationRef(SPECIFICATION_ID);
		full.setHref(URI.create("https://external.org/resource-specification/s"));
		full.setName("specification");
		full.setAtReferredType("ResourceSpecification");
		full.setVersion("1.0");
		return Stream.of(
				Arguments.of("id only", new ResourceSpecificationRef(SPECIFICATION_ID)),
				Arguments.of("every field", full));
	}

	/** Entity holding a {@link ResourceSpecificationRef} read from its properties, as every relationship will be. */
	@MappingEnabled(entityType = HOLDER_TYPE)
	public static class Holder {

		@Getter(onMethod = @__({@EntityId}))
		private final URI id;

		@Getter(onMethod = @__({@EntityType}))
		private final String type = HOLDER_TYPE;

		@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "resourceSpecification")}))
		@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "resourceSpecification",
				fromProperties = true, targetClass = ResourceSpecificationRef.class)}))
		private ResourceSpecificationRef resourceSpecification;

		public Holder(String id) {
			this.id = URI.create(id);
		}
	}
}
