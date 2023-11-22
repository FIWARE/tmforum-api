import org.fiware.serviceinventory.model.*;
import org.fiware.tmforum.common.test.AbstractApiIT;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.serviceinventory.api.ServiceApiTestClient;
import org.fiware.serviceinventory.api.ServiceApiTestSpec;
import org.fiware.tmforum.common.notification.EventHandler;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.common.test.ArgumentPair;
import org.fiware.tmforum.serviceinventory.domain.Service;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;


import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServiceApiIT extends AbstractApiIT implements ServiceApiTestSpec{
    public final ServiceApiTestClient serviceApiTestClient;

    private String message;
    private String fieldsParameter;
    private ServiceCreateVO serviceCreateVO;
    private ServiceUpdateVO serviceUpdateVO;
    private ServiceVO expectedService;

    public ServiceApiIT(ServiceApiTestClient serviceApiTestClient, EntitiesApiClient entitiesApiClient,
                        ObjectMapper objectMapper, GeneralProperties generalProperties) {
        super(entitiesApiClient, objectMapper, generalProperties);
        this.serviceApiTestClient = serviceApiTestClient;
    }

    @MockBean(EventHandler.class)
    public EventHandler eventHandler() {
        EventHandler eventHandler = mock(EventHandler.class);

        when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
        when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());
        when(eventHandler.handleDeleteEvent(any())).thenReturn(Mono.empty());

        return eventHandler;
    }

    @Override protected String getEntityType() {
        return Service.TYPE_SERVICE;
    }

    @ParameterizedTest
    @MethodSource("provideValidServices")
    public void createService201(String message, ServiceCreateVO serviceCreateVO, ServiceVO expectedService)
            throws Exception {
        this.message = message;
        this.serviceCreateVO = serviceCreateVO;
        this.expectedService = expectedService;
        createService201();
    }

    @Override
    public void createService201() throws Exception {

        HttpResponse<ServiceVO> serviceVOHttpResponse = callAndCatch(
                () -> serviceApiTestClient.createService(serviceCreateVO));
        assertEquals(HttpStatus.CREATED, serviceVOHttpResponse.getStatus(), message);
        String rfId = serviceVOHttpResponse.body().getId();
        expectedService.setId(rfId);
        expectedService.setHref(rfId);

        assertEquals(expectedService, serviceVOHttpResponse.body(), message);
    }

    private static Stream<Arguments> provideValidServices() {
        List<Arguments> testEntries = new ArrayList<>();

        testEntries.add(
                Arguments.of("An empty service should have been created.",
                        ServiceCreateVOTestExample.build()
                                .serviceSpecification(null)
                                .supportingResource(null),
                        ServiceVOTestExample.build()
                                .serviceSpecification(null)
                                .supportingResource(null)));

        Instant start = Instant.now();
        Instant end = Instant.now();
        testEntries.add(
                Arguments.of("A service with operating times should have been created.",
                        ServiceCreateVOTestExample.build()
                                .serviceSpecification(null)
                                .startDate(start).endDate(end),
                        ServiceVOTestExample.build()
                                .serviceSpecification(null)
                                .startDate(start).endDate(end)));

        testEntries.add(
                Arguments.of("A service with characteristic should have been created.",
                        ServiceCreateVOTestExample.build()
                                .serviceSpecification(null)
                                .serviceCharacteristic(List.of(CharacteristicVOTestExample.build())),
                        ServiceVOTestExample.build()
                                .serviceSpecification(null)
                                .serviceCharacteristic(List.of(CharacteristicVOTestExample.build()))));
        testEntries.add(
                Arguments.of("A service with feature should have been created.",
                        ServiceCreateVOTestExample.build()
                                .serviceSpecification(null)
                                .feature(List.of(FeatureVOTestExample.build()))
                                .serviceCharacteristic(List.of(CharacteristicVOTestExample.build())),
                        ServiceVOTestExample.build()
                                .serviceSpecification(null)
                                .feature(List.of(FeatureVOTestExample.build()))
                                .serviceCharacteristic(List.of(CharacteristicVOTestExample.build()))));
        testEntries.add(
                Arguments.of("A service with place should have been created.",
                        ServiceCreateVOTestExample.build()
                                .serviceSpecification(null)
                                .feature(List.of(FeatureVOTestExample.build()))
                                .place(List.of(RelatedPlaceRefOrValueVOTestExample.build()))
                                .serviceCharacteristic(List.of(CharacteristicVOTestExample.build())),
                        ServiceVOTestExample.build()
                                .serviceSpecification(null)
                                .feature(List.of(FeatureVOTestExample.build()))
                                .place(List.of(RelatedPlaceRefOrValueVOTestExample.build()))
                                .serviceCharacteristic(List.of(CharacteristicVOTestExample.build()))));
        testEntries.add(
                Arguments.of("A service with related entity should have been created.",
                        ServiceCreateVOTestExample.build()
                                .serviceSpecification(null)
                                .feature(List.of(FeatureVOTestExample.build()))
                                .place(List.of(RelatedPlaceRefOrValueVOTestExample.build()))
                                .relatedEntity(List.of(RelatedEntityRefOrValueVOTestExample.build()))
                                .serviceCharacteristic(List.of(CharacteristicVOTestExample.build())),
                        ServiceVOTestExample.build()
                                .serviceSpecification(null)
                                .feature(List.of(FeatureVOTestExample.build()))
                                .place(List.of(RelatedPlaceRefOrValueVOTestExample.build()))
                                .relatedEntity(List.of(RelatedEntityRefOrValueVOTestExample.build()))
                                .serviceCharacteristic(List.of(CharacteristicVOTestExample.build()))));
        testEntries.add(
                Arguments.of("A service with note should have been created.",
                        ServiceCreateVOTestExample.build()
                                .serviceSpecification(null)
                                .feature(List.of(FeatureVOTestExample.build()))
                                .place(List.of(RelatedPlaceRefOrValueVOTestExample.build()))
                                .note(List.of(NoteVOTestExample.build()))
                                .serviceCharacteristic(List.of(CharacteristicVOTestExample.build())),
                        ServiceVOTestExample.build()
                                .serviceSpecification(null)
                                .feature(List.of(FeatureVOTestExample.build()))
                                .place(List.of(RelatedPlaceRefOrValueVOTestExample.build()))
                                .note(List.of(NoteVOTestExample.build()))
                                .serviceCharacteristic(List.of(CharacteristicVOTestExample.build()))));
        testEntries.add(
                Arguments.of("A service with supporting service should have been created.",
                        ServiceCreateVOTestExample.build()
                                .serviceSpecification(null)
                                .feature(List.of(FeatureVOTestExample.build()))
                                .place(List.of(RelatedPlaceRefOrValueVOTestExample.build()))
                                .note(List.of(NoteVOTestExample.build()))
                                .supportingService(List.of(ServiceRefOrValueVOTestExample.build().supportingService(List.of(ServiceRefOrValueVOTestExample.build()))))
                                .serviceCharacteristic(List.of(CharacteristicVOTestExample.build())),
                        ServiceVOTestExample.build()
                                .serviceSpecification(null)
                                .feature(List.of(FeatureVOTestExample.build()))
                                .place(List.of(RelatedPlaceRefOrValueVOTestExample.build()))
                                .note(List.of(NoteVOTestExample.build()))
                                .supportingService(List.of(ServiceRefOrValueVOTestExample.build().supportingService(List.of(ServiceRefOrValueVOTestExample.build()))))
                                .serviceCharacteristic(List.of(CharacteristicVOTestExample.build()))));
        return testEntries.stream();
    }

    @ParameterizedTest
    @MethodSource("provideInvalidServices")
    public void createService400(String message, ServiceCreateVO invalidCreateVO) throws Exception {
        this.message = message;
        this.serviceCreateVO = invalidCreateVO;
        createService400();
    }

    @Override
    public void createService400() throws Exception {
        HttpResponse<ServiceVO> creationResponse = callAndCatch(
                () -> serviceApiTestClient.createService(serviceCreateVO));
        assertEquals(HttpStatus.BAD_REQUEST, creationResponse.getStatus(), message);
        Optional<ErrorDetails> optionalErrorDetails = creationResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
    }

    private static Stream<Arguments> provideInvalidServices() {
        List<Arguments> testEntries = new ArrayList<>();

        testEntries.add(Arguments.of("A service with invalid related parties should not be created.",
                ServiceCreateVOTestExample.build()
                        .serviceSpecification(null)
                        .relatedParty(List.of(RelatedPartyVOTestExample.build()))));
        testEntries.add(Arguments.of("A service with non-existent related parties should not be created.",
                ServiceCreateVOTestExample.build()
                        .serviceSpecification(null)
                        .relatedParty(
                                List.of((RelatedPartyVOTestExample.build()
                                        .id("urn:ngsi-ld:organisation:non-existent"))))));

        testEntries.add(Arguments.of("A service with invalid service specification should not be created.",
                ServiceCreateVOTestExample.build()
                        .serviceSpecification(ServiceSpecificationRefVOTestExample.build())));
        testEntries.add(Arguments.of("A service with non-existent service specifications should not be created.",
                ServiceCreateVOTestExample.build()
                        .serviceSpecification(
                                (ServiceSpecificationRefVOTestExample.build()
                                        .id("urn:ngsi-ld:organisation:non-existent")))));

        testEntries.add(Arguments.of("A service with invalid service specification should not be created.",
                ServiceCreateVOTestExample.build()
                        .serviceSpecification(null)
                        .supportingResource(List.of(ResourceRefVOTestExample.build()))));
        testEntries.add(Arguments.of("A service with non-existent service specifications should not be created.",
                ServiceCreateVOTestExample.build()
                        .serviceSpecification(null)
                        .supportingResource(
                                (List.of(ResourceRefVOTestExample.build()
                                        .id("urn:ngsi-ld:organisation:non-existent"))))));

        return testEntries.stream();
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void createService401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void createService403() throws Exception {

    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void createService405() throws Exception {
    }

    @Disabled("No implicit creation, impossible state.")
    @Test
    @Override
    public void createService409() throws Exception {

    }

    @Override
    public void createService500() throws Exception {

    }

    @Test
    @Override
    public void deleteService204() throws Exception {
        ServiceCreateVO emptyCreate = ServiceCreateVOTestExample.build()
                .serviceSpecification(null);

        HttpResponse<ServiceVO> createResponse = serviceApiTestClient.createService(emptyCreate);
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The service should have been created first.");

        String rfId = createResponse.body().getId();

        assertEquals(HttpStatus.NO_CONTENT,
                callAndCatch(() -> serviceApiTestClient.deleteService(rfId)).getStatus(),
                "The service should have been deleted.");

        assertEquals(HttpStatus.NOT_FOUND,
                callAndCatch(() -> serviceApiTestClient.retrieveService(rfId, null)).status(),
                "The service should not exist anymore.");
    }

    @Disabled("400 is impossible to happen on deletion with the current implementation.")
    @Test
    @Override
    public void deleteService400() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void deleteService401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void deleteService403() throws Exception {

    }

    @Test
    @Override
    public void deleteService404() throws Exception {
        HttpResponse<?> notFoundResponse = callAndCatch(
                () -> serviceApiTestClient.deleteService("urn:ngsi-ld:service-catalog:no-pop"));
        assertEquals(HttpStatus.NOT_FOUND,
                notFoundResponse.getStatus(),
                "No such service-catalog should exist.");

        Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

        notFoundResponse = callAndCatch(() -> serviceApiTestClient.deleteService("invalid-id"));
        assertEquals(HttpStatus.NOT_FOUND,
                notFoundResponse.getStatus(),
                "No such service-catalog should exist.");

        optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void deleteService405() throws Exception {

    }

    @Disabled("Impossible status.")
    @Test
    @Override
    public void deleteService409() throws Exception {

    }

    @Override
    public void deleteService500() throws Exception {

    }

    @Test
    @Override
    public void listService200() throws Exception {

        List<ServiceVO> expectedServices = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ServiceCreateVO serviceCreateVO = ServiceCreateVOTestExample.build()
                    .serviceSpecification(null);
            String id = serviceApiTestClient.createService(serviceCreateVO)
                    .body().getId();
            ServiceVO serviceVO = ServiceVOTestExample.build();
            serviceVO
                    .id(id)
                    .href(id)
                    .serviceSpecification(null)
                    .place(null)
                    .relatedParty(null)
                    .serviceOrderItem(null);
            expectedServices.add(serviceVO);
        }

        HttpResponse<List<ServiceVO>> serviceResponse = callAndCatch(
                () -> serviceApiTestClient.listService(null, null, null));

        assertEquals(HttpStatus.OK, serviceResponse.getStatus(), "The list should be accessible.");
        assertEquals(expectedServices.size(), serviceResponse.getBody().get().size(),
                "All services should have been returned.");
        List<ServiceVO> retrievedServices = serviceResponse.getBody().get();

        Map<String, ServiceVO> retrievedMap = retrievedServices.stream()
                .collect(Collectors.toMap(service -> service.getId(),
                        service -> service));

        expectedServices.stream()
                .forEach(
                        expectedService -> assertTrue(
                                retrievedMap.containsKey(expectedService.getId()),
                                String.format("All created services should be returned - Missing: %s.",
                                        expectedService,
                                        retrievedServices)));
        expectedServices.stream().forEach(
                expectedService -> assertEquals(expectedService,
                        retrievedMap.get(expectedService.getId()),
                        "The correct services should be retrieved."));

        // get with pagination
        Integer limit = 5;
        HttpResponse<List<ServiceVO>> firstPartResponse = callAndCatch(
                () -> serviceApiTestClient.listService(null, 0, limit));
        assertEquals(limit, firstPartResponse.body().size(),
                "Only the requested number of entries should be returend.");
        HttpResponse<List<ServiceVO>> secondPartResponse = callAndCatch(
                () -> serviceApiTestClient.listService(null, 0 + limit, limit));
        assertEquals(limit, secondPartResponse.body().size(),
                "Only the requested number of entries should be returend.");

        retrievedServices.clear();
        retrievedServices.addAll(firstPartResponse.body());
        retrievedServices.addAll(secondPartResponse.body());
        expectedServices.stream()
                .forEach(
                        expectedService -> assertTrue(
                                retrievedMap.containsKey(expectedService.getId()),
                                String.format("All created services should be returned - Missing: %s.",
                                        expectedService)));
        expectedServices.stream().forEach(
                expectedService -> assertEquals(expectedService,
                        retrievedMap.get(expectedService.getId()),
                        "The correct services should be retrieved."));
    }

    @Test
    @Override
    public void listService400() throws Exception {
        HttpResponse<List<ServiceVO>> badRequestResponse = callAndCatch(
                () -> serviceApiTestClient.listService(null, -1, null));
        assertEquals(HttpStatus.BAD_REQUEST,
                badRequestResponse.getStatus(),
                "Negative offsets are impossible.");

        Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

        badRequestResponse = callAndCatch(() -> serviceApiTestClient.listService(null, null, -1));
        assertEquals(HttpStatus.BAD_REQUEST,
                badRequestResponse.getStatus(),
                "Negative limits are impossible.");
        optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void listService401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void listService403() throws Exception {

    }

    @Disabled("Not found is not possible here, will be answered with an empty list instead.")
    @Test
    @Override
    public void listService404() throws Exception {

    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void listService405() throws Exception {

    }

    @Disabled("Impossible status.")
    @Test
    @Override
    public void listService409() throws Exception {

    }

    @Override
    public void listService500() throws Exception {

    }

    @ParameterizedTest
    @MethodSource("provideServiceUpdates")
    public void patchService200(String message, ServiceUpdateVO serviceUpdateVO, ServiceVO expectedService)
            throws Exception {
        this.message = message;
        this.serviceUpdateVO = serviceUpdateVO;
        this.expectedService = expectedService;
        patchService200();
    }

    @Override
    public void patchService200() throws Exception {
        //first create
        ServiceCreateVO serviceCreateVO = ServiceCreateVOTestExample.build()
                .serviceSpecification(null);

        HttpResponse<ServiceVO> createResponse = callAndCatch(
                () -> serviceApiTestClient.createService(serviceCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
                "The service function should have been created first.");

        String serviceId = createResponse.body().getId();

        HttpResponse<ServiceVO> updateResponse = callAndCatch(
                () -> serviceApiTestClient.patchService(serviceId, serviceUpdateVO));
        assertEquals(HttpStatus.OK, updateResponse.getStatus(), message);

        ServiceVO updatedService = updateResponse.body();
        expectedService.href(serviceId).id(serviceId);

        assertEquals(expectedService, updatedService, message);
    }

    private static Stream<Arguments> provideServiceUpdates() {
        List<Arguments> testEntries = new ArrayList<>();

        testEntries.add(Arguments.of("The description should have been updated.",
                ServiceUpdateVOTestExample.build()
                        .serviceSpecification(null)
                        .description("new-description"),
                ServiceVOTestExample.build()
                        .serviceSpecification(null)
                        .place(null)
                        .relatedParty(null)
                        .serviceOrderItem(null)
                        .supportingResource(null)
                        .supportingService(null)
                        .description("new-description")));

        testEntries.add(Arguments.of("The name should have been updated.",
                ServiceUpdateVOTestExample.build()
                        .serviceSpecification(null)
                        .name("new-name"),
                ServiceVOTestExample.build()
                        .serviceSpecification(null)
                        .place(null)
                        .relatedParty(null)
                        .serviceOrderItem(null)
                        .supportingResource(null)
                        .supportingService(null)
                        .name("new-name")));

        testEntries.add(Arguments.of("The isBundle should have been updated.",
                ServiceUpdateVOTestExample.build()
                        .serviceSpecification(null)
                        .isBundle(false),
                ServiceVOTestExample.build()
                        .serviceSpecification(null)
                        .place(null)
                        .relatedParty(null)
                        .serviceOrderItem(null)
                        .supportingResource(null)
                        .supportingService(null)
                        .isBundle(false)));

        Instant date = Instant.now();
        testEntries.add(Arguments.of("The startDate should have been updated.",
                ServiceUpdateVOTestExample.build()
                        .serviceSpecification(null)
                        .startDate(date),
                ServiceVOTestExample.build()
                        .serviceSpecification(null)
                        .place(null)
                        .relatedParty(null)
                        .serviceOrderItem(null)
                        .supportingResource(null)
                        .supportingService(null)
                        .startDate(date)));

        testEntries.add(Arguments.of("The endDate should have been updated.",
                ServiceUpdateVOTestExample.build()
                        .serviceSpecification(null)
                        .endDate(date),
                ServiceVOTestExample.build()
                        .serviceSpecification(null)
                        .place(null)
                        .relatedParty(null)
                        .serviceOrderItem(null)
                        .supportingResource(null)
                        .supportingService(null)
                        .endDate(date)));

        testEntries.add(Arguments.of("The characteristic should have been updated.",
                ServiceUpdateVOTestExample.build()
                        .serviceSpecification(null)
                        .serviceCharacteristic(List.of(CharacteristicVOTestExample.build().name("new"))),
                ServiceVOTestExample.build()
                        .serviceSpecification(null)
                        .place(null)
                        .relatedParty(null)
                        .serviceOrderItem(null)
                        .supportingResource(null)
                        .supportingService(null)
                        .serviceCharacteristic(List.of(CharacteristicVOTestExample.build().name("new")))));

        return testEntries.stream();
    }

    @ParameterizedTest
    @MethodSource("provideInvalidUpdates")
    public void patchService400(String message, ServiceUpdateVO invalidUpdateVO) throws Exception {
        this.message = message;
        this.serviceUpdateVO = invalidUpdateVO;
        patchService400();
    }

    @Override
    public void patchService400() throws Exception {
        //first create
        ServiceCreateVO serviceCreateVO = ServiceCreateVOTestExample.build()
                .serviceSpecification(null);

        HttpResponse<ServiceVO> createResponse = callAndCatch(
                () -> serviceApiTestClient.createService(serviceCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
                "The service function should have been created first.");

        String serviceId = createResponse.body().getId();

        HttpResponse<ServiceVO> updateResponse = callAndCatch(
                () -> serviceApiTestClient.patchService(serviceId, serviceUpdateVO));
        assertEquals(HttpStatus.BAD_REQUEST, updateResponse.getStatus(), message);

        Optional<ErrorDetails> optionalErrorDetails = updateResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Some error details should be present.");
    }

    private static Stream<Arguments> provideInvalidUpdates() {
        List<Arguments> testEntries = new ArrayList<>();
        testEntries.add(Arguments.of("A service with invalid related parties should not be created.",
                ServiceUpdateVOTestExample.build()
                        .serviceSpecification(null)
                        .relatedParty(List.of(RelatedPartyVOTestExample.build()))));
        testEntries.add(Arguments.of("A service with non-existent related parties should not be created.",
                ServiceUpdateVOTestExample.build()
                        .serviceSpecification(null)
                        .relatedParty(
                                List.of((RelatedPartyVOTestExample.build()
                                        .id("urn:ngsi-ld:organisation:non-existent"))))));

        testEntries.add(Arguments.of("A service with an invalid service sepecification ref should not be created.",
                ServiceUpdateVOTestExample.build()
                        .serviceSpecification(ServiceSpecificationRefVOTestExample.build())));
        testEntries.add(Arguments.of("A service with non-existent service specification ref should not be created.",
                ServiceUpdateVOTestExample.build()
                        .serviceSpecification(
                                ServiceSpecificationRefVOTestExample.build()
                                        .id("urn:ngsi-ld:service-specification:non-existent"))));

        testEntries.add(Arguments.of("A service with an invalid service sepecification ref should not be created.",
                ServiceUpdateVOTestExample.build()
                        .supportingResource(List.of(ResourceRefVOTestExample.build()))));
        testEntries.add(Arguments.of("A service with non-existent service specification ref should not be created.",
                ServiceUpdateVOTestExample.build()
                        .supportingResource(
                                List.of(ResourceRefVOTestExample.build()
                                        .id("urn:ngsi-ld:service-specification:non-existent")))));

        return testEntries.stream();
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void patchService401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void patchService403() throws Exception {

    }

    @Test
    @Override
    public void patchService404() throws Exception {
        ServiceUpdateVO serviceUpdateVO = ServiceUpdateVOTestExample.build();
        assertEquals(
                HttpStatus.NOT_FOUND,
                callAndCatch(() -> serviceApiTestClient.patchService("urn:ngsi-ld:service-catalog:not-existent",
                        serviceUpdateVO)).getStatus(),
                "Non existent service should not be updated.");
    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void patchService405() throws Exception {

    }

    @Override
    public void patchService409() throws Exception {
        // TODO: can this happen?
    }

    @Override
    public void patchService500() throws Exception {

    }

    @ParameterizedTest
    @MethodSource("provideFieldParameters")
    public void retrieveService200(String message, String fields, ServiceVO expectedService) throws Exception {
        this.fieldsParameter = fields;
        this.message = message;
        this.expectedService = expectedService;
        retrieveService200();
    }

    @Override
    public void retrieveService200() throws Exception {

        ServiceCreateVO serviceCreateVO = ServiceCreateVOTestExample.build()
                .serviceSpecification(null);
        HttpResponse<ServiceVO> createResponse = callAndCatch(
                () -> serviceApiTestClient.createService(serviceCreateVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), message);
        String id = createResponse.body().getId();

        expectedService
                .id(id)
                .href(id);

        //then retrieve
        HttpResponse<ServiceVO> retrievedRF = callAndCatch(
                () -> serviceApiTestClient.retrieveService(id, fieldsParameter));
        assertEquals(HttpStatus.OK, retrievedRF.getStatus(), message);
        assertEquals(expectedService, retrievedRF.body(), message);
    }

    private static Stream<Arguments> provideFieldParameters() {
        return Stream.of(
                Arguments.of("Without a fields parameter everything should be returned.", null,
                        ServiceVOTestExample.build()
                                .serviceSpecification(null)
                                .place(null)
                                .serviceOrderItem(null)
                                .relatedParty(null)),
                Arguments.of("Only description and the mandatory parameters should have been included.", "description",
                        ServiceVOTestExample.build()
                                .isBundle(null)
                                .name(null)
                                .startDate(null)
                                .place(null)
                                .serviceCharacteristic(null)
                                .serviceOrderItem(null)
                                .serviceRelationship(null)
                                .serviceSpecification(null)
                                .relatedParty(null)),
                Arguments.of(
                        "Only the mandatory parameters should have been included when a non-existent field was requested.",
                        "nothingToSeeHere", ServiceVOTestExample.build()
                                .description(null)
                                .isBundle(null)
                                .name(null)
                                .startDate(null)
                                .place(null)
                                .serviceCharacteristic(null)
                                .serviceOrderItem(null)
                                .serviceRelationship(null)
                                .serviceSpecification(null)
                                .relatedParty(null)),
                Arguments.of("Only description, isBundle and the mandatory parameters should have been included.",
                        "description,isBundle", ServiceVOTestExample.build()
                                .name(null)
                                .startDate(null)
                                .place(null)
                                .serviceCharacteristic(null)
                                .serviceOrderItem(null)
                                .serviceRelationship(null)
                                .serviceSpecification(null)
                                .relatedParty(null)));
    }

    @Disabled("400 cannot happen, only 404")
    @Test
    @Override
    public void retrieveService400() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void retrieveService401() throws Exception {

    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void retrieveService403() throws Exception {

    }

    @Test
    @Override
    public void retrieveService404() throws Exception {
        HttpResponse<ServiceVO> response = callAndCatch(
                () -> serviceApiTestClient.retrieveService("urn:ngsi-ld:service-function:non-existent", null));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such service-catalog should exist.");

        Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void retrieveService405() throws Exception {

    }

    @Disabled("Conflict not possible on retrieval")
    @Test
    @Override
    public void retrieveService409() throws Exception {

    }

    @Override
    public void retrieveService500() throws Exception {

    }
}
