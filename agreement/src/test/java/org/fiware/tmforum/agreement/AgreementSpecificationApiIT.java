package org.fiware.tmforum.agreement;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.fiware.agreement.api.AgreementApiTestSpec;
import org.fiware.agreement.api.AgreementSpecificationApiTestClient;
import org.fiware.agreement.model.AgreementSpecificationCreateVO;
import org.fiware.agreement.model.AgreementSpecificationCreateVOTestExample;
import org.fiware.agreement.model.AgreementSpecificationRelationshipVOTestExample;
import org.fiware.agreement.model.AgreementSpecificationUpdateVO;
import org.fiware.agreement.model.AgreementSpecificationVO;
import org.fiware.agreement.model.AgreementSpecificationVOTestExample;
import org.fiware.agreement.model.CategoryRefVO;
import org.fiware.agreement.model.CategoryRefVOTestExample;
import org.fiware.agreement.model.RelatedPartyVOTestExample;
import org.fiware.agreement.model.TimePeriodVO;
import org.fiware.agreement.model.TimePeriodVOTestExample;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.agreement.domain.AgreementSpecification;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@MicronautTest(packages = { "org.fiware.tmforum.agreement" })
public class AgreementSpecificationApiIT extends AbstractApiIT implements AgreementApiTestSpec {
    private final AgreementSpecificationApiTestClient agSpecApiTestClient;
    private String message;
    private AgreementSpecificationCreateVO agSpecCreateVO;
    private AgreementSpecificationUpdateVO agSpecUpdateVO;
    private AgreementSpecificationVO expectedAgSpec;

    private final EntitiesApiClient entitiesApiClient;
    private final ObjectMapper objectMapper;
    private final GeneralProperties generalProperties;

    public AgreementSpecificationApiIT(AgreementSpecificationApiTestClient agSpecApiTestClient,
            EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper,
            GeneralProperties generalProperties) {
        super(entitiesApiClient, objectMapper, generalProperties);
        this.agSpecApiTestClient = agSpecApiTestClient;
        this.entitiesApiClient = entitiesApiClient;
        this.objectMapper = objectMapper;
        this.generalProperties = generalProperties;
    }

    @Override
    protected String getEntityType() {
        return AgreementSpecification.TYPE_AGSP;
    }

    @ParameterizedTest
    @MethodSource({ "provideValidAgSpec" })
    public void createAgreement201(String message, AgreementSpecificationCreateVO agSpecCreateVO,
            AgreementSpecificationVO expectedAgSpec) throws Exception {
        this.message = message;
        this.agSpecCreateVO = agSpecCreateVO;
        this.expectedAgSpec = expectedAgSpec;
        createAgreement201();
    }

    private static Stream<Arguments> provideValidAgSpec() {
        List<Arguments> testEntries = new ArrayList<>();
        AgreementSpecificationCreateVO agSpecCreateVO = AgreementSpecificationCreateVOTestExample.build()
                .serviceCategory(null);
        AgreementSpecificationVO expectedAgSpec = AgreementSpecificationVOTestExample.build().serviceCategory(null);
        testEntries.add(
                Arguments.of("Empty AgreementSpecificacion should have been created", agSpecCreateVO, expectedAgSpec));
        TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build().endDateTime(Instant.now())
                .startDateTime(Instant.now());
        agSpecCreateVO = AgreementSpecificationCreateVOTestExample.build()
                .serviceCategory(null).validFor(timePeriodVO);
        expectedAgSpec = AgreementSpecificationVOTestExample.build().serviceCategory(null).validFor(timePeriodVO);
        testEntries.add(
                Arguments.of("AgreementSpecificacion with a validFor should have been created", agSpecCreateVO,
                        expectedAgSpec));

        return testEntries.stream();
    }

    @Override
    public void createAgreement201() throws Exception {
        HttpResponse<AgreementSpecificationVO> agSpecCreateResponse = callAndCatch(
                () -> agSpecApiTestClient.createAgreementSpecification(agSpecCreateVO));
        String id = agSpecCreateResponse.body().getId();
        expectedAgSpec.id(id).href(id);
        assertEquals(HttpStatus.CREATED, agSpecCreateResponse.getStatus(), message);
        assertEquals(expectedAgSpec, agSpecCreateResponse.body(), message);
    }

    @Test
    public void createAgreement201WithAgSpecRelationship() throws Exception {
        AgreementSpecificationCreateVO auxAgSpecCreateVO = AgreementSpecificationCreateVOTestExample.build()
                .serviceCategory(null).isBundle(true);
        HttpResponse<AgreementSpecificationVO> auxAgSpecCreateResponse = callAndCatch(
                () -> agSpecApiTestClient.createAgreementSpecification(auxAgSpecCreateVO));
        assertEquals(HttpStatus.CREATED, auxAgSpecCreateResponse.getStatus(),
                "Auxiliar AgreementSpecification should have been created");
        String id = auxAgSpecCreateResponse.body().getId();
        agSpecCreateVO = AgreementSpecificationCreateVOTestExample.build().serviceCategory(null)
                .specificationRelationship(List.of(AgreementSpecificationRelationshipVOTestExample.build().id(id)));
        HttpResponse<AgreementSpecificationVO> agSpecCreateResponse = callAndCatch(
                () -> agSpecApiTestClient.createAgreementSpecification(agSpecCreateVO));
        message = "AgreementSpecification with a relationship with another valid AgreementSpecification should have been created";
        assertEquals(HttpStatus.CREATED, agSpecCreateResponse.getStatus(), message);
        assertEquals(id, agSpecCreateResponse.body().getSpecificationRelationship().get(0).getId(), message);
    }

    @ParameterizedTest
    @MethodSource({ "provideInvalidAgSpec" })
    public void createCustomer400(String message, AgreementSpecificationCreateVO agSpecCreateVO) throws Exception {
        this.message = message;
        this.agSpecCreateVO = agSpecCreateVO;
        createAgreement400();
    }

    private static Stream<Arguments> provideInvalidAgSpec() {
        List<Arguments> testEntries = new ArrayList<>();
        CategoryRefVO category = CategoryRefVOTestExample.build();
        category.setId("urn:ngsi-ld:agreementSpecification:non-existent");
        AgreementSpecificationCreateVO agSpecCreateVO = AgreementSpecificationCreateVOTestExample.build()
                .serviceCategory(category);
        testEntries.add(
                Arguments.of("An AgreementSpecificacion with an invalid serviceCategory should not be created",
                        agSpecCreateVO));

        agSpecCreateVO = AgreementSpecificationCreateVOTestExample.build()
                .serviceCategory(null).relatedParty(List
                        .of(RelatedPartyVOTestExample.build().id("urn:ngsi-ld:agreementSpecification:non-existent")));
        testEntries.add(
                Arguments.of("AgreementSpecificacion with an invalid relatedParty should not be created",
                        agSpecCreateVO));

        return testEntries.stream();
    }

    @Override
    public void createAgreement400() throws Exception {
        HttpResponse<AgreementSpecificationVO> agSpecCreateResponse = callAndCatch(
                () -> agSpecApiTestClient.createAgreementSpecification(agSpecCreateVO));
        assertEquals(HttpStatus.BAD_REQUEST, agSpecCreateResponse.getStatus(), message);
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void createAgreement401() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createAgreement401'");
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void createAgreement403() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createAgreement403'");
    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void createAgreement405() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createAgreement405'");
    }

    @Disabled("No implicit creation, impossible state.")
    @Test
    @Override
    public void createAgreement409() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createAgreement409'");
    }

    @Override
    public void createAgreement500() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createAgreement500'");
    }

    @Override
    public void deleteAgreement204() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteAgreement204'");
    }

    @Override
    public void deleteAgreement400() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteAgreement400'");
    }

    @Override
    public void deleteAgreement401() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteAgreement401'");
    }

    @Override
    public void deleteAgreement403() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteAgreement403'");
    }

    @Override
    public void deleteAgreement404() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteAgreement404'");
    }

    @Override
    public void deleteAgreement405() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteAgreement405'");
    }

    @Override
    public void deleteAgreement409() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteAgreement409'");
    }

    @Override
    public void deleteAgreement500() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteAgreement500'");
    }

    @Override
    public void listAgreement200() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'listAgreement200'");
    }

    @Override
    public void listAgreement400() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'listAgreement400'");
    }

    @Override
    public void listAgreement401() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'listAgreement401'");
    }

    @Override
    public void listAgreement403() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'listAgreement403'");
    }

    @Override
    public void listAgreement404() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'listAgreement404'");
    }

    @Override
    public void listAgreement405() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'listAgreement405'");
    }

    @Override
    public void listAgreement409() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'listAgreement409'");
    }

    @Override
    public void listAgreement500() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'listAgreement500'");
    }

    @Override
    public void patchAgreement200() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'patchAgreement200'");
    }

    @Override
    public void patchAgreement400() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'patchAgreement400'");
    }

    @Override
    public void patchAgreement401() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'patchAgreement401'");
    }

    @Override
    public void patchAgreement403() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'patchAgreement403'");
    }

    @Override
    public void patchAgreement404() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'patchAgreement404'");
    }

    @Override
    public void patchAgreement405() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'patchAgreement405'");
    }

    @Override
    public void patchAgreement409() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'patchAgreement409'");
    }

    @Override
    public void patchAgreement500() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'patchAgreement500'");
    }

    @Override
    public void retrieveAgreement200() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'retrieveAgreement200'");
    }

    @Override
    public void retrieveAgreement400() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'retrieveAgreement400'");
    }

    @Override
    public void retrieveAgreement401() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'retrieveAgreement401'");
    }

    @Override
    public void retrieveAgreement403() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'retrieveAgreement403'");
    }

    @Override
    public void retrieveAgreement404() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'retrieveAgreement404'");
    }

    @Override
    public void retrieveAgreement405() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'retrieveAgreement405'");
    }

    @Override
    public void retrieveAgreement409() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'retrieveAgreement409'");
    }

    @Override
    public void retrieveAgreement500() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'retrieveAgreement500'");
    }

}
