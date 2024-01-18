package org.fiware.tmforum.agreement;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.agreement.api.AgreementApiTestSpec;
import org.fiware.agreement.api.AgreementSpecificationApiTestClient;
import org.fiware.agreement.model.*;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.agreement.domain.AgreementSpecification;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest(packages = { "org.fiware.tmforum.agreement" })
public class AgreementSpecificationApiIT extends AbstractApiIT implements AgreementApiTestSpec {
        private final AgreementSpecificationApiTestClient agSpecApiTestClient;
        private String message;
        private String fields;
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

        @MockBean(TMForumEventHandler.class)
        public TMForumEventHandler eventHandler() {
                TMForumEventHandler eventHandler = mock(TMForumEventHandler.class);

                when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
                when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());

                return eventHandler;
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
                AgreementSpecificationVO expectedAgSpec = AgreementSpecificationVOTestExample.build()
                                .serviceCategory(null);
                testEntries.add(
                                Arguments.of("Empty AgreementSpecificacion should have been created", agSpecCreateVO,
                                                expectedAgSpec));
                TimePeriodVO timePeriodVO = TimePeriodVOTestExample.build().endDateTime(Instant.now())
                                .startDateTime(Instant.now());
                agSpecCreateVO = AgreementSpecificationCreateVOTestExample.build()
                                .serviceCategory(null).validFor(timePeriodVO);
                expectedAgSpec = AgreementSpecificationVOTestExample.build().serviceCategory(null)
                                .validFor(timePeriodVO);
                testEntries.add(
                                Arguments.of("AgreementSpecificacion with a validFor should have been created",
                                                agSpecCreateVO,
                                                expectedAgSpec));

                return testEntries.stream();
        }

        @Override
        public void createAgreement201() throws Exception {
                HttpResponse<AgreementSpecificationVO> agSpecCreateResponse = callAndCatch(
                                () -> agSpecApiTestClient.createAgreementSpecification(agSpecCreateVO));
                assertEquals(HttpStatus.CREATED, agSpecCreateResponse.getStatus(), message);
                String id = agSpecCreateResponse.body().getId();
                expectedAgSpec.id(id).href(id);
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
                                .specificationRelationship(List
                                                .of(AgreementSpecificationRelationshipVOTestExample.build().id(id)));
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
                                                .of(RelatedPartyVOTestExample.build().id(
                                                                "urn:ngsi-ld:agreementSpecification:non-existent")));
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

        }

        @Override
        public void deleteAgreement204() throws Exception {
                AgreementSpecificationCreateVO agSpecCreate = AgreementSpecificationCreateVOTestExample.build()
                                .serviceCategory(null);
                HttpResponse<AgreementSpecificationVO> createAgSpecResponse = agSpecApiTestClient
                                .createAgreementSpecification(agSpecCreate);
                assertEquals(HttpStatus.CREATED, createAgSpecResponse.getStatus(),
                                "Agreement specification should be created");

                String agId = createAgSpecResponse.body().getId();

                assertEquals(HttpStatus.NO_CONTENT,
                                callAndCatch(() -> agSpecApiTestClient.deleteAgreementSpecification(agId)).getStatus(),
                                "The agreement specification should have been deleted.");

                assertEquals(HttpStatus.NOT_FOUND,
                                callAndCatch(() -> agSpecApiTestClient.retrieveAgreementSpecification(agId, null))
                                                .status(),
                                "The agreement specification should not exist anymore.");
        }

        @Disabled("400 is impossible to happen on deletion with the current implementation.")
        @Test
        @Override
        public void deleteAgreement400() throws Exception {

        }

        @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
        @Test
        @Override
        public void deleteAgreement401() throws Exception {

        }

        @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
        @Test
        @Override
        public void deleteAgreement403() throws Exception {

        }

        @Test
        @Override
        public void deleteAgreement404() throws Exception {
                String agId = "urn:ngsi-ld:agreementSpecification:non-existent";

                assertEquals(HttpStatus.NOT_FOUND,
                                callAndCatch(() -> agSpecApiTestClient.deleteAgreementSpecification(agId)).getStatus(),
                                "The customer should have been deleted.");
        }

        @Disabled("Prohibited by the framework.")
        @Test
        @Override
        public void deleteAgreement405() throws Exception {

        }

        @Disabled("Impossible status.")
        @Test
        @Override
        public void deleteAgreement409() throws Exception {

        }

        @Override
        public void deleteAgreement500() throws Exception {

        }

        @Test
        @Override
        public void listAgreement200() throws Exception {
                List<AgreementSpecificationVO> expectedAgSpec = new ArrayList<>();
                HttpResponse<AgreementSpecificationVO> createAgSpecResponse;
                AgreementSpecificationCreateVO createAgSpec;
                for (int i = 0; i < 10; i++) {
                        createAgSpec = AgreementSpecificationCreateVOTestExample.build()
                                        .serviceCategory(null);
                        createAgSpecResponse = agSpecApiTestClient.createAgreementSpecification(createAgSpec);
                        expectedAgSpec.add(createAgSpecResponse.body());
                }
                HttpResponse<List<AgreementSpecificationVO>> listResponse = callAndCatch(
                                () -> agSpecApiTestClient.listAgreementSpecification(null, null, null));
                assertEquals(HttpStatus.OK, listResponse.getStatus(),
                                "Agreement specification list should be accessible");
                assertEquals(expectedAgSpec.size(), listResponse.body().size(),
                                "The number of agreement specifications should be the same");
                Map<String, AgreementSpecificationVO> expectedmap = expectedAgSpec.stream()
                                .collect(Collectors.toMap((r) -> r.getId(), (t) -> t));
                listResponse.body().forEach((obj) -> assertNotNull(expectedmap.get(obj.getId()),
                                "Retrieved agreement specification list should contain all objects created previously"));

                // get with limit
                Integer limit = 5;
                listResponse = callAndCatch(
                                () -> agSpecApiTestClient.listAgreementSpecification(null, null, limit));
                assertEquals(HttpStatus.OK, listResponse.getStatus(),
                                "Agreement specification list should be accessible");
                assertEquals(limit, listResponse.body().size(),
                                "The number of agreement specifications should be the same");
        }

        @Test
        @Override
        public void listAgreement400() throws Exception {
                HttpResponse<List<AgreementSpecificationVO>> listResponse = callAndCatch(
                                () -> agSpecApiTestClient.listAgreementSpecification(null, -1, null));
                assertEquals(HttpStatus.BAD_REQUEST, listResponse.getStatus(),
                                "Agreement specification list should be accessible");
                listResponse = callAndCatch(
                                () -> agSpecApiTestClient.listAgreementSpecification(null, null, -1));
                assertEquals(HttpStatus.BAD_REQUEST, listResponse.getStatus(),
                                "Agreement specification list should be accessible");
        }

        @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
        @Test
        @Override
        public void listAgreement401() throws Exception {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'listAgreement401'");
        }

        @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
        @Test
        @Override
        public void listAgreement403() throws Exception {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'listAgreement403'");
        }

        @Disabled("Not found is not possible here, will be answered with an empty list instead.")
        @Test
        @Override
        public void listAgreement404() throws Exception {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'listAgreement404'");
        }

        @Disabled("Prohibited by the framework.")
        @Test
        @Override
        public void listAgreement405() throws Exception {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'listAgreement405'");
        }

        @Disabled("Impossible status.")
        @Test
        @Override
        public void listAgreement409() throws Exception {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'listAgreement409'");
        }

        @Override
        public void listAgreement500() throws Exception {
        }

        @ParameterizedTest
        @MethodSource("provideAgSpecUpdates")
        public void patchAgreement200(String message, AgreementSpecificationUpdateVO updateVO,
                        AgreementSpecificationVO agSpecVO) throws Exception {
                this.message = message;
                this.agSpecUpdateVO = updateVO;
                this.expectedAgSpec = agSpecVO;
                patchAgreement200();

        }

        private static Stream<Arguments> provideAgSpecUpdates() {
                List<Arguments> result = new ArrayList<>();
                result.add(Arguments.of("The name should have been updated",
                                AgreementSpecificationUpdateVOTestExample.build().serviceCategory(null).name("Updated"),
                                AgreementSpecificationVOTestExample.build().serviceCategory(null).name("Updated")
                                                .relatedParty(null)
                                                .specificationRelationship(null)));
                result.add(Arguments.of("The version should have been updated",
                                AgreementSpecificationUpdateVOTestExample.build().serviceCategory(null).version("2.2"),
                                AgreementSpecificationVOTestExample.build().serviceCategory(null).version("2.2")
                                                .relatedParty(null)
                                                .specificationRelationship(null)));
                Instant now = Instant.now();
                result.add(Arguments.of("The last updated info should have been updated",
                                AgreementSpecificationUpdateVOTestExample.build().serviceCategory(null).lastUpdate(now),
                                AgreementSpecificationVOTestExample.build().serviceCategory(null).lastUpdate(now)
                                                .relatedParty(null)
                                                .specificationRelationship(null)));

                return result.stream();
        }

        @Override
        public void patchAgreement200() throws Exception {
                // Agreement specification creation
                AgreementSpecificationCreateVO agSpecCreate = AgreementSpecificationCreateVOTestExample.build()
                                .serviceCategory(null);
                HttpResponse<AgreementSpecificationVO> agSpecCreateResponse = callAndCatch(
                                () -> agSpecApiTestClient.createAgreementSpecification(agSpecCreate));
                String id = agSpecCreateResponse.body().getId();
                assertEquals(HttpStatus.CREATED, agSpecCreateResponse.getStatus(),
                                "An agreeement specification should have been created firstly");
                HttpResponse<AgreementSpecificationVO> updateAgSpecResponse = callAndCatch(
                                () -> agSpecApiTestClient.patchAgreementSpecification(id, agSpecUpdateVO));
                assertEquals(HttpStatus.OK, updateAgSpecResponse.getStatus(), message);
                AgreementSpecificationVO updatedAgSpec = updateAgSpecResponse.body();
                expectedAgSpec.id(id).href(id);
                assertEquals(expectedAgSpec, updatedAgSpec, message);
        }

        @ParameterizedTest
        @MethodSource("provideInvalidUpdates")
        public void patchAgreement400(String messages, AgreementSpecificationUpdateVO invalidVO) throws Exception {
                this.message = message;
                this.agSpecUpdateVO = invalidVO;
                patchAgreement400();
        }

        private static Stream<Arguments> provideInvalidUpdates() {
                List<Arguments> result = new ArrayList<>();
                result.add(Arguments.of("An update with an invalid service category is not allowed.",
                                AgreementSpecificationUpdateVOTestExample.build()
                                                .serviceCategory(CategoryRefVOTestExample.build().id("invalid"))));
                result.add(Arguments.of("An update with an invalid related party is not allowed.",
                                AgreementSpecificationUpdateVOTestExample.build()
                                                .serviceCategory(null).relatedParty(List
                                                                .of(RelatedPartyVOTestExample.build().id("invalid")))));
                return result.stream();
        }

        @Override
        public void patchAgreement400() throws Exception {
                // Agreement specification creation
                AgreementSpecificationCreateVO agSpecCreate = AgreementSpecificationCreateVOTestExample.build()
                                .serviceCategory(null);
                HttpResponse<AgreementSpecificationVO> agSpecCreateResponse = callAndCatch(
                                () -> agSpecApiTestClient.createAgreementSpecification(agSpecCreate));
                String id = agSpecCreateResponse.body().getId();
                assertEquals(HttpStatus.CREATED, agSpecCreateResponse.getStatus(),
                                "An agreeement specification should have been created firstly");
                HttpResponse<AgreementSpecificationVO> updateAgSpecResponse = callAndCatch(
                                () -> agSpecApiTestClient.patchAgreementSpecification(id, agSpecUpdateVO));
                assertEquals(HttpStatus.BAD_REQUEST, updateAgSpecResponse.getStatus(), message);
        }

        @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
        @Test
        @Override
        public void patchAgreement401() throws Exception {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'patchAgreement401'");
        }

        @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
        @Test
        @Override
        public void patchAgreement403() throws Exception {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'patchAgreement403'");
        }

        @Test
        @Override
        public void patchAgreement404() throws Exception {
                AgreementSpecificationUpdateVO agspec = AgreementSpecificationUpdateVOTestExample.build();
                assertEquals(HttpStatus.NOT_FOUND,
                                callAndCatch(() -> agSpecApiTestClient.patchAgreementSpecification("non-existent",
                                                agspec)).getStatus(),
                                "It should not be able to find a non-existent agreement specification");
        }

        @Disabled("Prohibited by the framework.")
        @Test
        @Override
        public void patchAgreement405() throws Exception {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'patchAgreement405'");
        }

        @Override
        public void patchAgreement409() throws Exception {

        }

        @Override
        public void patchAgreement500() throws Exception {

        }

        @ParameterizedTest
        @MethodSource("provideFieldsRetrieve")
        public void retrieveAgreement200(String message, String fields, AgreementSpecificationVO agspec)
                        throws Exception {
                this.fields = fields;
                this.message = message;
                this.expectedAgSpec = agspec;
                retrieveAgreement200();
        }

        private static Stream<Arguments> provideFieldsRetrieve() {
                List<Arguments> result = new ArrayList<>();
                result.add(Arguments.of("If no fields are established, all attributes should be returned", null,
                                AgreementSpecificationVOTestExample.build().serviceCategory(null).relatedParty(null)
                                                .specificationRelationship(null)));
                result.add(Arguments.of(
                                "It should only show name, version and description attributes with attachment empty",
                                "name,version,description",
                                AgreementSpecificationVOTestExample.build().serviceCategory(null).relatedParty(null)
                                                .specificationRelationship(null).atType(null).atSchemaLocation(null)
                                                .atBaseType(null).specificationCharacteristic(null).isBundle(null)
                                                .lifecycleStatus(null)
                                                .validFor(null)));
                return result.stream();
        }

        @Override
        public void retrieveAgreement200() throws Exception {
                // Agreement specification creation
                AgreementSpecificationCreateVO agSpecCreate = AgreementSpecificationCreateVOTestExample.build()
                                .serviceCategory(null);
                HttpResponse<AgreementSpecificationVO> agSpecCreateResponse = callAndCatch(
                                () -> agSpecApiTestClient.createAgreementSpecification(agSpecCreate));
                String id = agSpecCreateResponse.body().getId();
                assertEquals(HttpStatus.CREATED, agSpecCreateResponse.getStatus(),
                                "An agreeement specification should have been created firstly");
                expectedAgSpec.id(id).href(id);

                HttpResponse<AgreementSpecificationVO> retrieveResponse = callAndCatch(
                                () -> agSpecApiTestClient.retrieveAgreementSpecification(id, fields));
                assertEquals(HttpStatus.OK, retrieveResponse.getStatus(), message);
                assertEquals(expectedAgSpec, retrieveResponse.body(), message);
        }

        @Disabled("400 cannot happen, only 404")
        @Test
        @Override
        public void retrieveAgreement400() throws Exception {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'retrieveAgreement400'");
        }

        @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
        @Test
        @Override
        public void retrieveAgreement401() throws Exception {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'retrieveAgreement401'");
        }

        @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
        @Test
        @Override
        public void retrieveAgreement403() throws Exception {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'retrieveAgreement403'");
        }

        @Override
        public void retrieveAgreement404() throws Exception {
                HttpResponse<AgreementSpecificationVO> response = callAndCatch(
                                () -> agSpecApiTestClient.retrieveAgreementSpecification(
                                                "urn:ngsi-ld:agreementSpecification:non-existent", null));
                assertEquals(HttpStatus.NOT_FOUND, response.getStatus(),
                                "No such agreement specification should exist");

                Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);

                assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
        }

        @Disabled("Prohibited by the framework.")
        @Test
        @Override
        public void retrieveAgreement405() throws Exception {
        }

        @Disabled("Conflict not possible on retrieval")
        @Test
        @Override
        public void retrieveAgreement409() throws Exception {
        }

        @Override
        public void retrieveAgreement500() throws Exception {

        }

}
