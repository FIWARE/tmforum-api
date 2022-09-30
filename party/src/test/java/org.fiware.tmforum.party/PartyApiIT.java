package org.fiware.tmforum.party;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.RequiredArgsConstructor;
import net.bytebuddy.matcher.FilterableList;
import org.fiware.party.api.OrganizationApiTestClient;
import org.fiware.party.api.OrganizationApiTestSpec;
import org.fiware.party.model.OrganizationCreateVO;
import org.fiware.party.model.OrganizationCreateVOTestExample;
import org.fiware.party.model.OrganizationVO;
import org.fiware.party.model.OrganizationVOTestExample;
import org.fiware.party.model.RelatedPartyVO;
import org.fiware.party.model.RelatedPartyVOTestExample;
import org.fiware.party.model.TaxDefinitionVO;
import org.fiware.party.model.TaxDefinitionVOTestExample;
import org.fiware.party.model.TaxExemptionCertificateVO;
import org.fiware.party.model.TaxExemptionCertificateVOTestExample;
import org.fiware.tmforum.party.domain.TaxExemptionCertificate;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RequiredArgsConstructor
@MicronautTest(packages = {"org.fiware.tmforum.party"})
class PartyApiIT implements OrganizationApiTestSpec {

    private final ObjectMapper objectMapper;
    private final OrganizationApiTestClient organizationApiTestClient;

    @Test
    @Override
    public void createOrganization201() throws Exception {

        OrganizationCreateVO organizationCreateVO = OrganizationCreateVOTestExample.build();
        organizationCreateVO.setOrganizationParentRelationship(null);

        OrganizationVO expectedOrganization = OrganizationVOTestExample.build();
        expectedOrganization.setOrganizationParentRelationship(null);

        HttpResponse<OrganizationVO> organizationCreateResponse = callAndCatch(() -> organizationApiTestClient.createOrganization(organizationCreateVO));
        assertEquals(HttpStatus.CREATED, organizationCreateResponse.getStatus(), "Organization should have been created.");

        OrganizationVO createdOrganizationVO = organizationCreateResponse.body();
        expectedOrganization.setId(createdOrganizationVO.getId());
        expectedOrganization.setHref(createdOrganizationVO.getId());
        assertEquals(expectedOrganization, createdOrganizationVO, "The created organization should have been returned.");
    }

    @Test
    @Override
    public void createOrganization400() throws Exception {
        for (OrganizationCreateVO ocVO : provideInvalidOrganizationCreate()) {
            HttpResponse<OrganizationVO> organizationCreateResponse = callAndCatch(() -> organizationApiTestClient.createOrganization(ocVO));
            assertEquals(HttpStatus.BAD_REQUEST, organizationCreateResponse.getStatus(), "Organization should not have been created.");
        }
    }

    // Helper method to catch potential http exceptions and return the status code.
    private <T> HttpResponse<T> callAndCatch(Callable<HttpResponse<T>> request) throws Exception {
        try {
            return request.call();
        } catch (HttpClientResponseException e) {
            return HttpResponse.status(e.getStatus());
        }
    }

    public static List<OrganizationCreateVO> provideInvalidOrganizationCreate() {
        OrganizationCreateVO nonExistentParentCreateVO = OrganizationCreateVOTestExample.build();
        nonExistentParentCreateVO.getOrganizationParentRelationship().getOrganization().setId("urn:ngsi-ld:organization:valid-but-not-existent");

        OrganizationCreateVO invalidRelatedPartyOrg = OrganizationCreateVOTestExample.build();
        RelatedPartyVO invalidRelatedPartyRef = RelatedPartyVOTestExample.build();
        invalidRelatedPartyOrg.setRelatedParty(List.of(invalidRelatedPartyRef));

        OrganizationCreateVO nonExistentRelatedPartyOrg = OrganizationCreateVOTestExample.build();
        RelatedPartyVO nonExistentRelatedPartyRef = RelatedPartyVOTestExample.build();
        nonExistentRelatedPartyRef.setId("urn:ngsi-ld:individual:non-existent");
        nonExistentRelatedPartyOrg.setRelatedParty(List.of(nonExistentRelatedPartyRef));

        return List.of(
                // invalid parent org
                OrganizationCreateVOTestExample.build(),
                nonExistentParentCreateVO,
                invalidRelatedPartyOrg,
                nonExistentRelatedPartyOrg
        );
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void createOrganization401() throws Exception {
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void createOrganization403() throws Exception {
    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void createOrganization405() throws Exception {

    }

    @Test
    @Override
    public void createOrganization409() throws Exception {
        TaxDefinitionVO taxDefinitionVO = TaxDefinitionVOTestExample.build();
        // make the test repeatable
        taxDefinitionVO.setId(UUID.randomUUID().toString());
        TaxExemptionCertificateVO taxExemptionCertificateVO = TaxExemptionCertificateVOTestExample.build();
        taxExemptionCertificateVO.setTaxDefinition(List.of(taxDefinitionVO));
        // make the test repeatable
        taxExemptionCertificateVO.setId(UUID.randomUUID().toString());
        // workaround for the bad example
        taxExemptionCertificateVO.setAttachment(null);
        OrganizationCreateVO organizationCreateVO = OrganizationCreateVOTestExample.build();
        organizationCreateVO.setOrganizationParentRelationship(null);
        organizationCreateVO.setTaxExemptionCertificate(List.of(taxExemptionCertificateVO));

        // first create should succeed
        assertEquals(
                HttpStatus.CREATED,
                callAndCatch(() -> organizationApiTestClient.createOrganization(organizationCreateVO)).getStatus(),
                "Organization should have been created.");

        // second should be a conflict, since the tax-exemption already exists.
        assertEquals(
                HttpStatus.CONFLICT,
                callAndCatch(() -> organizationApiTestClient.createOrganization(organizationCreateVO)).getStatus(),
                "Organization should not have been created, due to the conflicting tax-exemption.");

    }

    @Override
    public void createOrganization500() throws Exception {

    }

    @Test
    @Override
    public void deleteOrganization204() throws Exception {
        // first create one
        OrganizationCreateVO organizationCreateVO = OrganizationCreateVOTestExample.build();
        organizationCreateVO.setOrganizationParentRelationship(null);
        HttpResponse<OrganizationVO> organizationCreateResponse = callAndCatch(() -> organizationApiTestClient.createOrganization(organizationCreateVO));
        assertEquals(HttpStatus.CREATED, organizationCreateResponse.getStatus(), "The organization should have been created first.");

        String orgId = organizationCreateResponse.body().getId();

        assertEquals(HttpStatus.NO_CONTENT,
                callAndCatch(() -> organizationApiTestClient.deleteOrganization(orgId)).getStatus(),
                "The organization should have been deleted.");

        assertEquals(HttpStatus.NOT_FOUND,
                callAndCatch(() -> organizationApiTestClient.retrieveOrganization(orgId, null)).status(),
                "The organization should not exist anymore.");

    }

    @Disabled("400 is impossible to happen on deletion with the current implementation.")
    @Test
    @Override
    public void deleteOrganization400() throws Exception {
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void deleteOrganization401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void deleteOrganization403() throws Exception {

    }

    @Test
    @Override
    public void deleteOrganization404() throws Exception {
        String ngsiLdOrgId = "urn:ngsi-ld:organization:valid";
        String nonNgsiLdOrgId = "non-ngsi";
        assertEquals(HttpStatus.NOT_FOUND,
                callAndCatch(() -> organizationApiTestClient.deleteOrganization(ngsiLdOrgId)).getStatus(),
                "No such organization should exist.");
        assertEquals(HttpStatus.NOT_FOUND,
                callAndCatch(() -> organizationApiTestClient.deleteOrganization(nonNgsiLdOrgId)).getStatus(),
                "No such organization should exist.");
    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void deleteOrganization405() throws Exception {

    }

    @Disabled("Impossible status.")
    @Test
    @Override
    public void deleteOrganization409() throws Exception {

    }

    @Override
    public void deleteOrganization500() throws Exception {

    }

    @Disabled("Needs db cleaning.")
    @Test
    @Override
    public void listOrganization200() throws Exception {
        // find a way to clean before
        List<OrganizationVO> expectedOrganizations = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            OrganizationCreateVO organizationCreateVO = OrganizationCreateVOTestExample.build();
            organizationCreateVO.setOrganizationParentRelationship(null);
            String id = organizationApiTestClient.createOrganization(organizationCreateVO).body().getId();
            OrganizationVO organizationVO = OrganizationVOTestExample.build();
            organizationVO.setId(id);
            organizationVO.setHref(id);
            expectedOrganizations.add(organizationVO);
        }

        HttpResponse<List<OrganizationVO>> organizationListResponse = callAndCatch(() -> organizationApiTestClient.listOrganization(null, null, null));
        assertEquals(HttpStatus.OK, organizationListResponse.getStatus(), "The list should be accessible.");

        // ignore order
        List<OrganizationVO> organizationVOS = organizationListResponse.body();
        assertEquals(expectedOrganizations.size(), organizationVOS.size(), "All organizations should be returned.");
        expectedOrganizations
                .forEach(organizationVO ->
                        assertTrue(organizationVOS.contains(organizationVO),
                                String.format("All organizations should be contained. Missing: %s", organizationVO)));

        // get with pagination
        Integer limit = 5;
        HttpResponse<List<OrganizationVO>> firstPartResponse = callAndCatch(() -> organizationApiTestClient.listOrganization(null, 0, limit));
        assertEquals(limit, firstPartResponse.body(), "Only the requested number of entries should be returend.");
        HttpResponse<List<OrganizationVO>> secondPartResponse = callAndCatch(() -> organizationApiTestClient.listOrganization(null, 0 + limit, limit));
        assertEquals(limit, secondPartResponse.body(), "Only the requested number of entries should be returend.");

        List<OrganizationVO> retrievedOrganizations = firstPartResponse.body();
        retrievedOrganizations.addAll(secondPartResponse.body());
        expectedOrganizations
                .forEach(organizationVO ->
                        assertTrue(retrievedOrganizations.contains(organizationVO),
                                String.format("All organizations should be contained. Missing: %s", organizationVO)));

    }

    @Test
    @Override
    public void listOrganization400() throws Exception {
        assertEquals(HttpStatus.BAD_REQUEST,
                callAndCatch(() -> organizationApiTestClient.listOrganization(null, -1, null)).getStatus(),
                "Negative offsets are impossible.");
        assertEquals(HttpStatus.BAD_REQUEST,
                callAndCatch(() -> organizationApiTestClient.listOrganization(null, null, -1)).getStatus(),
                "Negative limits are impossible.");

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void listOrganization401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void listOrganization403() throws Exception {

    }

    @Disabled("Not found is not possible here, will be answerd with an empty list instead.")
    @Test
    @Override
    public void listOrganization404() throws Exception {

    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void listOrganization405() throws Exception {

    }

    @Disabled("Impossible status.")
    @Test
    @Override
    public void listOrganization409() throws Exception {

    }

    @Override
    public void listOrganization500() throws Exception {

    }

    @Override
    public void patchOrganization200() throws Exception {

    }

    @Override
    public void patchOrganization400() throws Exception {

    }

    @Override
    public void patchOrganization401() throws Exception {

    }

    @Override
    public void patchOrganization403() throws Exception {

    }

    @Override
    public void patchOrganization404() throws Exception {

    }

    @Override
    public void patchOrganization405() throws Exception {

    }

    @Override
    public void patchOrganization409() throws Exception {

    }

    @Override
    public void patchOrganization500() throws Exception {

    }

    @Override
    public void retrieveOrganization200() throws Exception {

    }

    @Override
    public void retrieveOrganization400() throws Exception {

    }

    @Override
    public void retrieveOrganization401() throws Exception {

    }

    @Override
    public void retrieveOrganization403() throws Exception {

    }

    @Override
    public void retrieveOrganization404() throws Exception {

    }

    @Override
    public void retrieveOrganization405() throws Exception {

    }

    @Override
    public void retrieveOrganization409() throws Exception {

    }

    @Override
    public void retrieveOrganization500() throws Exception {

    }

    class TestCharacteristic {
        public final long valuation;
        public final String unit;

        TestCharacteristic(long valuation, String unit) {
            this.valuation = valuation;
            this.unit = unit;
        }
    }

}
