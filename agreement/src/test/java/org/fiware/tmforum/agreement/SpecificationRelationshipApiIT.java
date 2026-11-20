package org.fiware.tmforum.agreement;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.agreement.api.AgreementSpecificationApiTestClient;
import org.fiware.agreement.model.AgreementSpecificationCreateVO;
import org.fiware.agreement.model.AgreementSpecificationCreateVOTestExample;
import org.fiware.agreement.model.AgreementSpecificationRelationshipVO;
import org.fiware.agreement.model.AgreementSpecificationUpdateVO;
import org.fiware.agreement.model.AgreementSpecificationUpdateVOTestExample;
import org.fiware.agreement.model.AgreementSpecificationVO;
import org.fiware.agreement.model.TimePeriodVO;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.agreement.domain.AgreementSpecification;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Round-trip of an agreement specification's specificationRelationship to stored specifications through the API.
 * Before ticket 88 the read resolved the stored target and failed, since AgreementSpecificationRelationship has no
 * {@code @MappingEnabled}.
 */
@MicronautTest(packages = {"org.fiware.tmforum.agreement"}, contextBuilder = FirstMatchConfigContextBuilder.class)
public class SpecificationRelationshipApiIT extends AbstractApiIT {

	private static final String RELATIONSHIP_TYPE = "dependency";
	private static final Instant VALID_FROM = Instant.parse("2026-01-01T00:00:00Z");
	// written into each relationship's name, so a read-back tells the patched payload from the created one
	private static final String CREATE = "create";
	private static final String PATCH = "patch";

	/**
	 * How much of a relationship a test case posts.
	 */
	enum Payload {
		// the id only
		MINIMAL,
		// every field
		FULL
	}

	private final AgreementSpecificationApiTestClient agreementSpecificationApiTestClient;

	public SpecificationRelationshipApiIT(EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper,
			GeneralProperties generalProperties,
			AgreementSpecificationApiTestClient agreementSpecificationApiTestClient) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.agreementSpecificationApiTestClient = agreementSpecificationApiTestClient;
	}

	@MockBean(TMForumEventHandler.class)
	public TMForumEventHandler eventHandler() {
		TMForumEventHandler eventHandler = mock(TMForumEventHandler.class);
		when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
		when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());
		return eventHandler;
	}

	private static Stream<Arguments> relationships() {
		return Stream.of(
				Arguments.of("A minimal relationship to a stored specification should round-trip.", Payload.MINIMAL, 1),
				Arguments.of("A full relationship to a stored specification should round-trip.", Payload.FULL, 1),
				Arguments.of("Full relationships to several stored specifications should round-trip.", Payload.FULL,
						2));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("relationships")
	public void specificationRelationshipToStoredSpecificationsRoundTrips(String message, Payload payload, int count)
			throws Exception {
		List<String> targetIds = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			targetIds.add(createSpecification(specificationCreate()));
		}

		List<AgreementSpecificationRelationshipVO> created = relationships(targetIds, payload, CREATE);
		HttpResponse<AgreementSpecificationVO> createResponse = callAndCatch(
				() -> agreementSpecificationApiTestClient.createAgreementSpecification(null,
						specificationCreate().specificationRelationship(created)));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), message);
		String id = createResponse.body().getId();
		assertRoundTrip(created, createResponse.body(), "create response: " + message);
		assertRoundTrip(created, retrieve(id), "retrieval after create: " + message);

		// same target ids, so the patch updates the stored instances on both brokers
		List<AgreementSpecificationRelationshipVO> patched = relationships(targetIds, payload, PATCH);
		AgreementSpecificationUpdateVO updateVO = AgreementSpecificationUpdateVOTestExample.build()
				.atSchemaLocation(null)
				.serviceCategory(null)
				.specificationRelationship(patched);
		HttpResponse<AgreementSpecificationVO> patchResponse = callAndCatch(
				() -> agreementSpecificationApiTestClient.patchAgreementSpecification(null, id, updateVO));
		assertEquals(HttpStatus.OK, patchResponse.getStatus(), message);
		assertRoundTrip(patched, patchResponse.body(), "patch response: " + message);
		assertRoundTrip(patched, retrieve(id), "retrieval after patch: " + message);
	}

	private static List<AgreementSpecificationRelationshipVO> relationships(List<String> targetIds, Payload payload,
			String stage) {
		return targetIds.stream().map(targetId -> relationship(targetId, payload, stage)).toList();
	}

	private static AgreementSpecificationRelationshipVO relationship(String targetId, Payload payload, String stage) {
		AgreementSpecificationRelationshipVO relationship = new AgreementSpecificationRelationshipVO().id(targetId);
		if (payload == Payload.MINIMAL) {
			return relationship;
		}
		return relationship
				.href(targetId)
				.name(String.format("%s (%s)", targetId, stage))
				.relationshipType(RELATIONSHIP_TYPE)
				.validFor(new TimePeriodVO().startDateTime(VALID_FROM))
				.atReferredType(AgreementSpecification.TYPE_AGREEMENT_SPECIFICATION);
	}

	private void assertRoundTrip(List<AgreementSpecificationRelationshipVO> posted,
			AgreementSpecificationVO readBack, String message) {
		assertEquals(multiset(objectMapper.valueToTree(posted)),
				multiset(objectMapper.valueToTree(readBack.getSpecificationRelationship())), message);
	}

	private static AgreementSpecificationCreateVO specificationCreate() {
		return AgreementSpecificationCreateVOTestExample.build().atSchemaLocation(null).serviceCategory(null);
	}

	private String createSpecification(AgreementSpecificationCreateVO createVO) throws Exception {
		HttpResponse<AgreementSpecificationVO> response = callAndCatch(
				() -> agreementSpecificationApiTestClient.createAgreementSpecification(null, createVO));
		assertEquals(HttpStatus.CREATED, response.getStatus(), "The target specification should have been created.");
		return response.body().getId();
	}

	private AgreementSpecificationVO retrieve(String id) throws Exception {
		HttpResponse<AgreementSpecificationVO> retrieved = callAndCatch(
				() -> agreementSpecificationApiTestClient.retrieveAgreementSpecification(null, id, null));
		assertEquals(HttpStatus.OK, retrieved.getStatus(), "The agreement specification should be retrievable.");
		return retrieved.body();
	}

	@Override
	protected String getEntityType() {
		return AgreementSpecification.TYPE_AGREEMENT_SPECIFICATION;
	}
}
