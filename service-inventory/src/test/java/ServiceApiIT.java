import org.fiware.tmforum.common.test.AbstractApiIT;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.serviceinventory.api.ServiceApiTestClient;
import org.fiware.serviceinventory.api.ServiceApiTestSpec;
import org.fiware.serviceinventory.model.CharacteristicVOTestExample;
import org.fiware.serviceinventory.model.ServiceCreateVO;
import org.fiware.serviceinventory.model.ServiceCreateVOTestExample;
import org.fiware.serviceinventory.model.ServiceRefOrValueVOTestExample;
import org.fiware.serviceinventory.model.ServiceRelationshipVOTestExample;
import org.fiware.serviceinventory.model.ServiceSpecificationRefVOTestExample;
import org.fiware.serviceinventory.model.ServiceUpdateVO;
import org.fiware.serviceinventory.model.ServiceUpdateVOTestExample;
import org.fiware.serviceinventory.model.ServiceVO;
import org.fiware.serviceinventory.model.ServiceVOTestExample;
import org.fiware.serviceinventory.model.RelatedPartyVOTestExample;
import org.fiware.serviceinventory.model.RelatedPlaceRefOrValueVOTestExample;
import org.fiware.serviceinventory.model.RelatedServiceOrderItemVOTestExample;
import org.fiware.serviceinventory.model.ResourceRefVOTestExample;
import org.fiware.serviceinventory.model.ServiceRefVOTestExample;
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
        return Service.TYPE_SERVICE_INVENTORY;
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
                                .serviceSpecification(null)
                                .supportingResource(null)
                                .startDate(start).endDate(end),
                        ServiceVOTestExample.build().serviceSpecification(null)
                                .serviceSpecification(null)
                                .supportingResource(null)
                                .startDate(start).endDate(end)));

        testEntries.add(
                Arguments.of("A service with characteristic should have been created.",
                        ServiceCreateVOTestExample.build()
                                .serviceSpecification(null)
                                .serviceSpecification(null)
                                .supportingResource(null)
                                .serviceCharacteristic(List.of(CharacteristicVOTestExample.build())),
                        ServiceVOTestExample.build().serviceSpecification(null)
                                .serviceSpecification(null)
                                .supportingResource(null)
                                .serviceCharacteristic(List.of(CharacteristicVOTestExample.build()))));
        return testEntries.stream();
    }

    @Override
    public void createService400() throws Exception {

    }

    @Override
    public void createService401() throws Exception {

    }

    @Override
    public void createService403() throws Exception {

    }

    @Override
    public void createService405() throws Exception {

    }

    @Override
    public void createService409() throws Exception {

    }

    @Override
    public void createService500() throws Exception {

    }

    @Override
    public void deleteService204() throws Exception {

    }

    @Override
    public void deleteService400() throws Exception {

    }

    @Override
    public void deleteService401() throws Exception {

    }

    @Override
    public void deleteService403() throws Exception {

    }

    @Override
    public void deleteService404() throws Exception {

    }

    @Override
    public void deleteService405() throws Exception {

    }

    @Override
    public void deleteService409() throws Exception {

    }

    @Override
    public void deleteService500() throws Exception {

    }

    @Override
    public void listService200() throws Exception {

    }

    @Override
    public void listService400() throws Exception {

    }

    @Override
    public void listService401() throws Exception {

    }

    @Override
    public void listService403() throws Exception {

    }

    @Override
    public void listService404() throws Exception {

    }

    @Override
    public void listService405() throws Exception {

    }

    @Override
    public void listService409() throws Exception {

    }

    @Override
    public void listService500() throws Exception {

    }

    @Override
    public void patchService200() throws Exception {

    }

    @Override
    public void patchService400() throws Exception {

    }

    @Override
    public void patchService401() throws Exception {

    }

    @Override
    public void patchService403() throws Exception {

    }

    @Override
    public void patchService404() throws Exception {

    }

    @Override
    public void patchService405() throws Exception {

    }

    @Override
    public void patchService409() throws Exception {

    }

    @Override
    public void patchService500() throws Exception {

    }

    @Override
    public void retrieveService200() throws Exception {

    }

    @Override
    public void retrieveService400() throws Exception {

    }

    @Override
    public void retrieveService401() throws Exception {

    }

    @Override
    public void retrieveService403() throws Exception {

    }

    @Override
    public void retrieveService404() throws Exception {

    }

    @Override
    public void retrieveService405() throws Exception {

    }

    @Override
    public void retrieveService409() throws Exception {

    }

    @Override
    public void retrieveService500() throws Exception {

    }
}
