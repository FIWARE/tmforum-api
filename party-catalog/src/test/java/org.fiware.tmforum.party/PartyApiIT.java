package org.fiware.tmforum.party;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.party.api.IndividualApiTestClient;
import org.fiware.party.api.OrganizationApiTestClient;
import org.fiware.party.model.*;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest(packages = {"org.fiware.tmforum.party"})
public class PartyApiIT extends AbstractApiIT {

	private final OrganizationApiTestClient organizationApiTestClient;
	private final IndividualApiTestClient individualApiTestClient;

	public PartyApiIT(OrganizationApiTestClient organizationApiTestClient,
					  IndividualApiTestClient individualApiTestClient, EntitiesApiClient entitiesApiClient,
					  ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.organizationApiTestClient = organizationApiTestClient;
		this.individualApiTestClient = individualApiTestClient;
	}

	@MockBean(TMForumEventHandler.class)
	public TMForumEventHandler eventHandler() {
		TMForumEventHandler eventHandler = mock(TMForumEventHandler.class);

		when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
		when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());

		return eventHandler;
	}

	@Test
	public void testRelatedParties() throws Exception {
		IndividualCreateVO individualCreateVO = IndividualCreateVOTestExample.build().atSchemaLocation(null);
		HttpResponse<IndividualVO> individualCreateResponse = callAndCatch(
				() -> individualApiTestClient.createIndividual(null, individualCreateVO));
		assertEquals(HttpStatus.CREATED, individualCreateResponse.getStatus(),
				"Initial individual should have been created");
		String individualId = individualCreateResponse.body().getId();

		IndividualVO expectedIndividual = IndividualVOTestExample.build().atSchemaLocation(null);
		expectedIndividual.setHref(individualId);
		expectedIndividual.setId(individualId);

		assertEquals(expectedIndividual, individualCreateResponse.body(), "The individual should have been created.");

		OrganizationCreateVO parentOrgCreateVO = OrganizationCreateVOTestExample.build().atSchemaLocation(null);
		parentOrgCreateVO.setOrganizationParentRelationship(null);
		HttpResponse<OrganizationVO> parentOrgCreateResponse = callAndCatch(
				() -> organizationApiTestClient.createOrganization(null, parentOrgCreateVO));
		assertEquals(HttpStatus.CREATED, parentOrgCreateResponse.getStatus(),
				"Initial parent organization should have been created");
		String parentOrgId = parentOrgCreateResponse.body().getId();

		OrganizationVO expectedParent = OrganizationVOTestExample.build().atSchemaLocation(null);
		expectedParent.setId(parentOrgId);
		expectedParent.setHref(parentOrgId);
		// we have no parent
		expectedParent.setOrganizationParentRelationship(null);

		assertEquals(expectedParent, parentOrgCreateResponse.body(), "The parent org should have been created.");

		OrganizationCreateVO childOrganizationCreateVO = OrganizationCreateVOTestExample.build().atSchemaLocation(null);
		OrganizationParentRelationshipVO organizationParentRelationshipVO = OrganizationParentRelationshipVOTestExample.build().atSchemaLocation(null);
		OrganizationRefVO organizationRefVO = OrganizationRefVOTestExample.build().atSchemaLocation(null);
		organizationRefVO.setId(parentOrgId);
		organizationRefVO.setName("Parent");

		organizationParentRelationshipVO.setOrganization(organizationRefVO);
		childOrganizationCreateVO.setOrganizationParentRelationship(organizationParentRelationshipVO);
		RelatedPartyVO relatedPartyVOParent = RelatedPartyVOTestExample.build().atSchemaLocation(null);
		relatedPartyVOParent.setId(parentOrgId);
		RelatedPartyVO relatedPartyVOIndividual = RelatedPartyVOTestExample.build().atSchemaLocation(null);
		relatedPartyVOIndividual.setId(individualId);
		childOrganizationCreateVO.setRelatedParty(List.of(relatedPartyVOParent, relatedPartyVOIndividual));

		HttpResponse<OrganizationVO> childOrgCreateResponse = callAndCatch(
				() -> organizationApiTestClient.createOrganization(null, childOrganizationCreateVO));
		assertEquals(HttpStatus.CREATED, childOrgCreateResponse.getStatus(),
				"Initial child organization should have been created");
		String childId = childOrgCreateResponse.body().getId();

		OrganizationVO expectedChild = OrganizationVOTestExample.build().atSchemaLocation(null);
		expectedChild.setHref(childId);
		expectedChild.setId(childId);
		OrganizationRefVO expectedParentRef = OrganizationRefVOTestExample.build().atSchemaLocation(null);
		expectedParentRef.setId(parentOrgId);
		expectedParentRef.setName("Parent");

		OrganizationParentRelationshipVO expectedParentRelationship = OrganizationParentRelationshipVOTestExample.build().atSchemaLocation(null);
		expectedParentRelationship.setOrganization(expectedParentRef);
		expectedChild.setOrganizationParentRelationship(expectedParentRelationship);

		RelatedPartyVO parentParty = RelatedPartyVOTestExample.build().atSchemaLocation(null);
		parentParty.setId(parentOrgId);
		RelatedPartyVO individualParty = RelatedPartyVOTestExample.build().atSchemaLocation(null);
		individualParty.setId(individualId);
		expectedChild.setRelatedParty(List.of(parentParty, individualParty));

		assertEquals(expectedChild, childOrgCreateResponse.body(), "The child organization should have been created.");

		// update the parent
		OrganizationUpdateVO parentUpdateVO = OrganizationUpdateVOTestExample.build().atSchemaLocation(null);
		OrganizationRefVO childRef = OrganizationRefVOTestExample.build().atSchemaLocation(null);
		// we still dont have one
		parentUpdateVO.setOrganizationParentRelationship(null);
		childRef.setName("Child");
		childRef.setId(childId);
		childRef.setHref(childId);
		OrganizationChildRelationshipVO childRelationshipVO = OrganizationChildRelationshipVOTestExample.build().atSchemaLocation(null);
		childRelationshipVO.setOrganization(childRef);
		parentUpdateVO.setOrganizationChildRelationship(List.of(childRelationshipVO));

		HttpResponse<OrganizationVO> parentUpdateResponse = callAndCatch(
				() -> organizationApiTestClient.patchOrganization(null, parentOrgId, parentUpdateVO));
		assertEquals(HttpStatus.OK, parentUpdateResponse.getStatus(), "The parent should have been updated.");
		expectedParent.setOrganizationChildRelationship(List.of(childRelationshipVO));
		expectedParent.relatedParty(null);

		assertEquals(expectedParent, parentUpdateResponse.body(), "The parent should have been updated.");

		HttpResponse<OrganizationVO> parentGet = callAndCatch(
				() -> organizationApiTestClient.retrieveOrganization(null, parentOrgId, null));
		assertEquals(expectedParent, parentGet.body(), "The parent via get should also be equal.");
	}

	@Override
	protected String getEntityType() {
		return "nothing-to-clean";
	}
}
