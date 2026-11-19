package org.fiware.tmforum.resourcecatalog;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.resourcecatalog.api.ResourceCandidateApiTestClient;
import org.fiware.resourcecatalog.api.ResourceSpecificationApiTestClient;
import org.fiware.resourcecatalog.model.ResourceCandidateCreateVO;
import org.fiware.resourcecatalog.model.ResourceCandidateCreateVOTestExample;
import org.fiware.resourcecatalog.model.ResourceCandidateVO;
import org.fiware.resourcecatalog.model.ResourceSpecificationCreateVO;
import org.fiware.resourcecatalog.model.ResourceSpecificationCreateVOTestExample;
import org.fiware.resourcecatalog.model.ResourceSpecificationRefVO;
import org.fiware.resourcecatalog.model.ResourceSpecificationVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.fiware.tmforum.resource.LogicalResourceSpecification;
import org.fiware.tmforum.resource.ResourceCandidate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A {@code ResourceSpecification} sub-type stores its own entity type but inherits versioning, so a pinned
 * {@code ResourceSpecificationRef} to it is checked against its version on every backend.
 */
@MicronautTest(packages = {"org.fiware.tmforum.resourcecatalog"}, contextBuilder = FirstMatchConfigContextBuilder.class)
public class ResourceSpecificationVersionIT extends AbstractApiIT {

	private static final String LOGICAL_SPECIFICATION_AT_TYPE = "LogicalResourceSpecification";
	private static final String CURRENT_VERSION = "1.0";
	private static final String OTHER_VERSION = "2.0";
	private static final String LIFECYCLE_STATUS = "created";

	private final ResourceCandidateApiTestClient resourceCandidateApiTestClient;
	private final ResourceSpecificationApiTestClient resourceSpecificationApiTestClient;

	// cleanUp only removes resource candidates
	private final List<String> createdSpecifications = new ArrayList<>();

	public ResourceSpecificationVersionIT(EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper,
										  GeneralProperties generalProperties,
										  ResourceCandidateApiTestClient resourceCandidateApiTestClient,
										  ResourceSpecificationApiTestClient resourceSpecificationApiTestClient) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.resourceCandidateApiTestClient = resourceCandidateApiTestClient;
		this.resourceSpecificationApiTestClient = resourceSpecificationApiTestClient;
	}

	@MockBean(TMForumEventHandler.class)
	public TMForumEventHandler eventHandler() {
		TMForumEventHandler eventHandler = mock(TMForumEventHandler.class);
		when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
		when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());
		return eventHandler;
	}

	@AfterEach
	public void removeSpecifications() throws Exception {
		for (String id : createdSpecifications) {
			callAndCatch(() -> resourceSpecificationApiTestClient.deleteResourceSpecification(null, id));
		}
	}

	private static Stream<Arguments> subTypeReferences() {
		return Stream.of(
				Arguments.of("A reference pinned to the sub-type's version resolves.", CURRENT_VERSION,
						HttpStatus.CREATED),
				Arguments.of("A reference pinned to another version of the sub-type is rejected.", OTHER_VERSION,
						HttpStatus.BAD_REQUEST),
				Arguments.of("An unpinned reference to the sub-type resolves.", null, HttpStatus.CREATED));
	}

	@ParameterizedTest
	@MethodSource("subTypeReferences")
	public void referenceToLogicalSpecification(String message, String refVersion, HttpStatus expectedStatus)
			throws Exception {
		String specificationId = createLogicalSpecification();
		assertTrue(specificationId.contains(LogicalResourceSpecification.TYPE_LOGICAL_RESOURCE_SPECIFICATION),
				"The specification should be stored as the sub-type.");

		ResourceCandidateCreateVO createVO = ResourceCandidateCreateVOTestExample.build().atSchemaLocation(null)
				.lifecycleStatus(LIFECYCLE_STATUS)
				.resourceSpecification(new ResourceSpecificationRefVO().id(specificationId).version(refVersion));
		HttpResponse<ResourceCandidateVO> response = callAndCatch(
				() -> resourceCandidateApiTestClient.createResourceCandidate(null, createVO));
		assertEquals(expectedStatus, response.getStatus(), message);
	}

	private String createLogicalSpecification() throws Exception {
		ResourceSpecificationCreateVO createVO = ResourceSpecificationCreateVOTestExample.build()
				.atSchemaLocation(null)
				.atType(LOGICAL_SPECIFICATION_AT_TYPE)
				.version(CURRENT_VERSION);
		HttpResponse<ResourceSpecificationVO> response = callAndCatch(
				() -> resourceSpecificationApiTestClient.createResourceSpecification(null, createVO));
		assertEquals(HttpStatus.CREATED, response.getStatus(), "The logical specification should have been created.");
		createdSpecifications.add(response.body().getId());
		return response.body().getId();
	}

	@Override
	protected String getEntityType() {
		return ResourceCandidate.TYPE_RESOURCE_CANDIDATE;
	}
}
