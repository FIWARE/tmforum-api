package org.fiware.tmforum.party;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.RequiredArgsConstructor;
import org.fiware.party.api.IndividualApiTestClient;
import org.fiware.party.api.OrganizationApiTestClient;
import org.fiware.party.model.IndividualCreateVO;
import org.fiware.party.model.IndividualCreateVOTestExample;
import org.fiware.party.model.IndividualVO;
import org.fiware.party.model.IndividualVOTestExample;
import org.fiware.party.model.OrganizationChildRelationshipVO;
import org.fiware.party.model.OrganizationChildRelationshipVOTestExample;
import org.fiware.party.model.OrganizationCreateVO;
import org.fiware.party.model.OrganizationCreateVOTestExample;
import org.fiware.party.model.OrganizationParentRelationshipVO;
import org.fiware.party.model.OrganizationParentRelationshipVOTestExample;
import org.fiware.party.model.OrganizationRefVO;
import org.fiware.party.model.OrganizationRefVOTestExample;
import org.fiware.party.model.OrganizationUpdateVO;
import org.fiware.party.model.OrganizationUpdateVOTestExample;
import org.fiware.party.model.OrganizationVO;
import org.fiware.party.model.OrganizationVOTestExample;
import org.fiware.party.model.RelatedPartyVO;
import org.fiware.party.model.RelatedPartyVOTestExample;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RequiredArgsConstructor
@MicronautTest(packages = { "org.fiware.tmforum.party" })
public class PartyApiIT extends AbstractApiIT {

	private final OrganizationApiTestClient organizationApiTestClient;
	private final IndividualApiTestClient individualApiTestClient;

	@Test
	public void testRelatedParties() throws Exception {
		IndividualCreateVO individualCreateVO = IndividualCreateVOTestExample.build();
		HttpResponse<IndividualVO> individualCreateResponse = callAndCatch(
				() -> individualApiTestClient.createIndividual(individualCreateVO));
		assertEquals(HttpStatus.CREATED, individualCreateResponse.getStatus(),
				"Initial individual should have been created");
		String individualId = individualCreateResponse.body().getId();

		IndividualVO expectedIndividual = IndividualVOTestExample.build();
		expectedIndividual.setHref(individualId);
		expectedIndividual.setId(individualId);

		assertEquals(expectedIndividual, individualCreateResponse.body(), "The individual should have been created.");

		OrganizationCreateVO parentOrgCreateVO = OrganizationCreateVOTestExample.build();
		parentOrgCreateVO.setOrganizationParentRelationship(null);
		HttpResponse<OrganizationVO> parentOrgCreateResponse = callAndCatch(
				() -> organizationApiTestClient.createOrganization(parentOrgCreateVO));
		assertEquals(HttpStatus.CREATED, parentOrgCreateResponse.getStatus(),
				"Initial parent organization should have been created");
		String parentOrgId = parentOrgCreateResponse.body().getId();

		OrganizationVO expectedParent = OrganizationVOTestExample.build();
		expectedParent.setId(parentOrgId);
		expectedParent.setHref(parentOrgId);
		// we have no parent
		expectedParent.setOrganizationParentRelationship(null);

		assertEquals(expectedParent, parentOrgCreateResponse.body(), "The parent org should have been created.");

		OrganizationCreateVO childOrganizationCreateVO = OrganizationCreateVOTestExample.build();
		OrganizationParentRelationshipVO organizationParentRelationshipVO = OrganizationParentRelationshipVOTestExample.build();
		OrganizationRefVO organizationRefVO = OrganizationRefVOTestExample.build();
		organizationRefVO.setId(parentOrgId);
		organizationRefVO.setName("Parent");

		organizationParentRelationshipVO.setOrganization(organizationRefVO);
		childOrganizationCreateVO.setOrganizationParentRelationship(organizationParentRelationshipVO);
		RelatedPartyVO relatedPartyVOParent = RelatedPartyVOTestExample.build();
		relatedPartyVOParent.setId(parentOrgId);
		RelatedPartyVO relatedPartyVOIndividual = RelatedPartyVOTestExample.build();
		relatedPartyVOIndividual.setId(individualId);
		childOrganizationCreateVO.setRelatedParty(List.of(relatedPartyVOParent, relatedPartyVOIndividual));

		HttpResponse<OrganizationVO> childOrgCreateResponse = callAndCatch(
				() -> organizationApiTestClient.createOrganization(childOrganizationCreateVO));
		assertEquals(HttpStatus.CREATED, childOrgCreateResponse.getStatus(),
				"Initial child organization should have been created");
		String childId = childOrgCreateResponse.body().getId();

		OrganizationVO expectedChild = OrganizationVOTestExample.build();
		expectedChild.setHref(childId);
		expectedChild.setId(childId);
		OrganizationRefVO expectedParentRef = OrganizationRefVOTestExample.build();
		expectedParentRef.setId(parentOrgId);
		expectedParentRef.setName("Parent");
		// nothing of this is set
		expectedParentRef.setHref(null);

		OrganizationParentRelationshipVO expectedParentRelationship = OrganizationParentRelationshipVOTestExample.build();
		expectedParentRelationship.setOrganization(expectedParentRef);
		expectedChild.setOrganizationParentRelationship(expectedParentRelationship);

		RelatedPartyVO parentParty = RelatedPartyVOTestExample.build();
		parentParty.setId(parentOrgId);
		RelatedPartyVO individualParty = RelatedPartyVOTestExample.build();
		individualParty.setId(individualId);
		expectedChild.setRelatedParty(List.of(parentParty, individualParty));

		assertEquals(expectedChild, childOrgCreateResponse.body(), "The child organization should have been created.");

		// update the parent
		OrganizationUpdateVO parentUpdateVO = OrganizationUpdateVOTestExample.build();
		OrganizationRefVO childRef = OrganizationRefVOTestExample.build();
		// we still dont have one
		parentUpdateVO.setOrganizationParentRelationship(null);
		childRef.setName("Child");
		childRef.setId(childId);
		childRef.setHref(childId);
		OrganizationChildRelationshipVO childRelationshipVO = OrganizationChildRelationshipVOTestExample.build();
		childRelationshipVO.setOrganization(childRef);
		parentUpdateVO.setOrganizationChildRelationship(List.of(childRelationshipVO));

		HttpResponse<OrganizationVO> parentUpdateResponse = callAndCatch(
				() -> organizationApiTestClient.patchOrganization(parentOrgId, parentUpdateVO));
		assertEquals(HttpStatus.OK, parentUpdateResponse.getStatus(), "The parent should have been updated.");
		expectedParent.setOrganizationChildRelationship(List.of(childRelationshipVO));
		expectedParent.setRelatedParty(null);

		assertEquals(expectedParent, parentUpdateResponse.body(), "The parent should have been updated.");

		HttpResponse<OrganizationVO> parentGet = callAndCatch(
				() -> organizationApiTestClient.retrieveOrganization(parentOrgId, null));
		assertEquals(expectedParent, parentGet.body(), "The parent via get should also be equal.");
	}
}
