package org.fiware.tmforum.resourcefunction.domain;

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
 * Pins that every field of {@link ConnectionPointRef}, {@link ResourceFunctionRef} and {@link EndpointRef}, including the endpoint's nested
 * connection point, is stored inline in the relationship, so it reads back without the target.
 */
class ResourceFunctionActivationRefRoundTripTest {

	private static final String HOLDER_TYPE = "holder";
	private static final String HOLDER_ID = "urn:ngsi-ld:holder:h";
	private static final String CONNECTION_POINT_ID = "urn:ngsi-ld:connection-point:c";
	private static final String RESOURCE_FUNCTION_ID = "urn:ngsi-ld:resource-function:r";
	private static final String ENDPOINT_ID = "urn:ngsi-ld:endpoint:e";

	@ParameterizedTest(name = "{0}")
	@MethodSource("refs")
	public void everyFieldSurvivesTheRoundTrip(String description, Holder holder, Function<Holder, Object> ref) {
		Holder read = NgsiLdRoundTrip.roundTrip(holder, Holder.class);

		assertEquals(NgsiLdRoundTrip.toTree(ref.apply(holder)), NgsiLdRoundTrip.toTree(ref.apply(read)), description);
	}

	private static Stream<Arguments> refs() {
		ResourceFunctionRef resourceFunction = new ResourceFunctionRef(RESOURCE_FUNCTION_ID);
		resourceFunction.setHref(URI.create("https://external.org/resource-function/r"));
		resourceFunction.setName("resource function");
		resourceFunction.setAtReferredType(ResourceFunction.TYPE_RESOURCE_FUNCTION);
		resourceFunction.setVersion("1.0");
		EndpointRef endpoint = new EndpointRef(ENDPOINT_ID);
		endpoint.setHref(URI.create("https://external.org/endpoint/e"));
		endpoint.setName("endpoint");
		endpoint.setAtReferredType("Endpoint");
		endpoint.setIsRoot(true);
		endpoint.setConnectionPoint(connectionPoint());
		EndpointRef idOnlyConnectionPoint = new EndpointRef(ENDPOINT_ID);
		idOnlyConnectionPoint.setConnectionPoint(new ConnectionPointRef(CONNECTION_POINT_ID));

		Holder fullConnectionPoint = new Holder(HOLDER_ID);
		fullConnectionPoint.setConnectionPoint(connectionPoint());
		Holder fullResourceFunction = new Holder(HOLDER_ID);
		fullResourceFunction.setResourceFunction(resourceFunction);
		Holder idOnlyEndpoint = new Holder(HOLDER_ID);
		idOnlyEndpoint.setEndpoint(new EndpointRef(ENDPOINT_ID));
		Holder endpointWithIdOnlyConnectionPoint = new Holder(HOLDER_ID);
		endpointWithIdOnlyConnectionPoint.setEndpoint(idOnlyConnectionPoint);
		Holder fullEndpoint = new Holder(HOLDER_ID);
		fullEndpoint.setEndpoint(endpoint);
		return Stream.of(
				Arguments.of("ConnectionPointRef every field", fullConnectionPoint,
						(Function<Holder, Object>) Holder::getConnectionPoint),
				Arguments.of("ResourceFunctionRef every field", fullResourceFunction,
						(Function<Holder, Object>) Holder::getResourceFunction),
				Arguments.of("EndpointRef id only", idOnlyEndpoint, (Function<Holder, Object>) Holder::getEndpoint),
				Arguments.of("EndpointRef with id-only connection point", endpointWithIdOnlyConnectionPoint,
						(Function<Holder, Object>) Holder::getEndpoint),
				Arguments.of("EndpointRef every field", fullEndpoint, (Function<Holder, Object>) Holder::getEndpoint));
	}

	private static ConnectionPointRef connectionPoint() {
		ConnectionPointRef connectionPoint = new ConnectionPointRef(CONNECTION_POINT_ID);
		connectionPoint.setHref(URI.create("https://external.org/connection-point/c"));
		connectionPoint.setName("connection point");
		connectionPoint.setAtReferredType("ConnectionPoint");
		connectionPoint.setVersion("2.0");
		return connectionPoint;
	}

	/** Entity holding each ref read from its properties, as every relationship will be. */
	@MappingEnabled(entityType = HOLDER_TYPE)
	public static class Holder {

		@Getter(onMethod = @__({@EntityId}))
		private final URI id;

		@Getter(onMethod = @__({@EntityType}))
		private final String type = HOLDER_TYPE;

		@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "connectionPoint")}))
		@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "connectionPoint",
				fromProperties = true, targetClass = ConnectionPointRef.class)}))
		private ConnectionPointRef connectionPoint;

		@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "resourceFunction")}))
		@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "resourceFunction",
				fromProperties = true, targetClass = ResourceFunctionRef.class)}))
		private ResourceFunctionRef resourceFunction;

		@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "endpoint")}))
		@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "endpoint",
				fromProperties = true, targetClass = EndpointRef.class)}))
		private EndpointRef endpoint;

		public Holder(String id) {
			this.id = URI.create(id);
		}
	}
}
