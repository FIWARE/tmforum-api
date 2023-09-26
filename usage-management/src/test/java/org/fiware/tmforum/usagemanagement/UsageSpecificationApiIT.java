package org.fiware.tmforum.usagemanagement;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.net.URI;

import org.fiware.usagemanagement.api.UsageSpecificationApiTestSpec;
import org.fiware.usagemanagement.api.UsageSpecificationApiTestClient;
import org.fiware.tmforum.usagemanagement.domain.UsageSpecification;
import org.fiware.usagemanagement.model.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.fiware.usagemanagement.model.UsageSpecificationCreateVO;
import org.fiware.usagemanagement.model.UsageSpecificationCreateVOTestExample;
import org.fiware.usagemanagement.model.UsageSpecificationUpdateVO;
import org.fiware.usagemanagement.model.UsageSpecificationUpdateVOTestExample;
import org.fiware.usagemanagement.model.RelatedPartyVO;
import org.fiware.usagemanagement.model.RelatedPartyVOTestExample;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@MicronautTest(packages = { "org.fiware.tmforum.usagemanagement" })
public class UsageSpecificationApiIT extends AbstractApiIT implements UsageSpecificationApiTestSpec{

    private final UsageSpecificationApiTestClient usageSpecificationApiTestClient;
    private String message;
    private String fields;
    private UsageSpecificationCreateVO usageSpecificationCreateVO;
    private UsageSpecificationUpdateVO usageSpecificationUpdateVO;
    private UsageSpecificationVO expectedUsageSpecification;

    private final EntitiesApiClient entitiesApiClient;
    private final ObjectMapper objectMapper;
    private final GeneralProperties generalProperties;

    public UsageSpecificationApiIT(UsageSpecificationApiTestClient usageSpecificationApiTestClient, EntitiesApiClient entitiesApiClient,
                      ObjectMapper objectMapper,GeneralProperties generalProperties) {
        super(entitiesApiClient, objectMapper, generalProperties);
        this.usageSpecificationApiTestClient = usageSpecificationApiTestClient;
        this.entitiesApiClient = entitiesApiClient;
        this.objectMapper = objectMapper;
        this.generalProperties = generalProperties;
    }

    @Override
    protected String getEntityType() {
        return UsageSpecification.TYPE_USP;
    }

    @ParameterizedTest
    @MethodSource("provideValidUsageSpecifications")
    public void createUsageSpecification201(String message, UsageSpecificationCreateVO usageSpecificationCreateVO,
            UsageSpecificationVO expectedUsageSpecification) throws Exception {

        this.message = message;
        this.usageSpecificationCreateVO = usageSpecificationCreateVO;
        this.expectedUsageSpecification = expectedUsageSpecification;
        createUsageSpecification201();

    }

    @Override
    public void createUsageSpecification201() throws Exception {
        HttpResponse<UsageSpecificationVO> usageSpecificationVOHttpResponse = callAndCatch(
                () -> usageSpecificationApiTestClient.createUsageSpecification(usageSpecificationCreateVO));

        assertEquals(HttpStatus.CREATED,usageSpecificationVOHttpResponse.getStatus(), message);
        UsageSpecificationVO createdUsageSpecificationVO = usageSpecificationVOHttpResponse.body();
        String usageId = usageSpecificationVOHttpResponse.body().getId();
        expectedUsageSpecification.setId(usageId);
        expectedUsageSpecification.setHref(new URI(usageId));
        assertEquals(expectedUsageSpecification, createdUsageSpecificationVO, message);
    }

    private static Stream<Arguments> provideValidUsageSpecifications() {
        List<Arguments> testEntries = new ArrayList<>();

        UsageSpecificationCreateVO usageSpecificationCreateVO = UsageSpecificationCreateVOTestExample.build()
                .targetEntitySchema(null)
                .validFor(null);
        UsageSpecificationVO expectedUsageSpecification = UsageSpecificationVOTestExample.build()
                .targetEntitySchema(null)
                .validFor(null);
        testEntries.add(
                Arguments.of("Empty usageSpecification should have been created.", usageSpecificationCreateVO, expectedUsageSpecification));

        TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build().endDateTime(Instant.now())
                .startDateTime(Instant.now());
        usageSpecificationCreateVO = UsageSpecificationCreateVOTestExample.build()
                .targetEntitySchema(null)
                .validFor(timePeriodVO);
        expectedUsageSpecification = UsageSpecificationVOTestExample.build()
                .targetEntitySchema(null)
                .validFor(timePeriodVO);
        testEntries.add(
                Arguments.of("AgreementSpecificacion with a validFor should have been created",
                        usageSpecificationCreateVO,
                        expectedUsageSpecification));

        return testEntries.stream();
    }

    @Test
    public void createUsageSpecification201WithEntityRelationship() throws Exception {

    }

    @Override
    public void createUsageSpecification400() throws Exception {

    }

    @Override
    public void createUsageSpecification401() throws Exception {

    }

    @Override
    public void createUsageSpecification403() throws Exception {

    }

    @Override
    public void createUsageSpecification405() throws Exception {

    }

    @Override
    public void createUsageSpecification409() throws Exception {

    }

    @Override
    public void createUsageSpecification500() throws Exception {

    }

    @Override
    public void deleteUsageSpecification204() throws Exception {

    }

    @Override
    public void deleteUsageSpecification400() throws Exception {

    }

    @Override
    public void deleteUsageSpecification401() throws Exception {

    }

    @Override
    public void deleteUsageSpecification403() throws Exception {

    }

    @Override
    public void deleteUsageSpecification404() throws Exception {

    }

    @Override
    public void deleteUsageSpecification405() throws Exception {

    }

    @Override
    public void deleteUsageSpecification409() throws Exception {

    }

    @Override
    public void deleteUsageSpecification500() throws Exception {

    }

    @Override
    public void listUsageSpecification200() throws Exception {

    }

    @Override
    public void listUsageSpecification400() throws Exception {

    }

    @Override
    public void listUsageSpecification401() throws Exception {

    }

    @Override
    public void listUsageSpecification403() throws Exception {

    }

    @Override
    public void listUsageSpecification404() throws Exception {

    }

    @Override
    public void listUsageSpecification405() throws Exception {

    }

    @Override
    public void listUsageSpecification409() throws Exception {

    }

    @Override
    public void listUsageSpecification500() throws Exception {

    }

    @Override
    public void patchUsageSpecification200() throws Exception {

    }

    @Override
    public void patchUsageSpecification400() throws Exception {

    }

    @Override
    public void patchUsageSpecification401() throws Exception {

    }

    @Override
    public void patchUsageSpecification403() throws Exception {

    }

    @Override
    public void patchUsageSpecification404() throws Exception {

    }

    @Override
    public void patchUsageSpecification405() throws Exception {

    }

    @Override
    public void patchUsageSpecification409() throws Exception {

    }

    @Override
    public void patchUsageSpecification500() throws Exception {

    }

    @Override
    public void retrieveUsageSpecification200() throws Exception {

    }

    @Override
    public void retrieveUsageSpecification400() throws Exception {

    }

    @Override
    public void retrieveUsageSpecification401() throws Exception {

    }

    @Override
    public void retrieveUsageSpecification403() throws Exception {

    }

    @Override
    public void retrieveUsageSpecification404() throws Exception {

    }

    @Override
    public void retrieveUsageSpecification405() throws Exception {

    }

    @Override
    public void retrieveUsageSpecification409() throws Exception {

    }

    @Override
    public void retrieveUsageSpecification500() throws Exception {

    }

    // createUsageSpecification


}

