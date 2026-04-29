package org.fiware.tmforum.customerbillmanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.customerbillmanagement.api.CustomerBillOnDemandApiTestClient;
import org.fiware.customerbillmanagement.api.ext.CustomerBillOnDemandExtensionApiTestClient;
import org.fiware.customerbillmanagement.api.ext.CustomerBillOnDemandExtensionApiTestSpec;
import org.fiware.customerbillmanagement.model.CustomerBillOnDemandCreateVO;
import org.fiware.customerbillmanagement.model.CustomerBillOnDemandCreateVOTestExample;
import org.fiware.customerbillmanagement.model.CustomerBillOnDemandVO;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.customerbillmanagement.domain.CustomerBillOnDemand;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest(packages = {"org.fiware.tmforum.customerbillmanagement"})
@Property(name = "apiExtension.enabled", value = "true")
public class ExtendedCustomerBillOnDemandApiIT extends AbstractApiIT
        implements CustomerBillOnDemandExtensionApiTestSpec {

    private final CustomerBillOnDemandExtensionApiTestClient extensionTestClient;
    private final CustomerBillOnDemandApiTestClient baseTestClient;

    public ExtendedCustomerBillOnDemandApiIT(EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper,
            GeneralProperties generalProperties,
            CustomerBillOnDemandExtensionApiTestClient extensionTestClient,
            CustomerBillOnDemandApiTestClient baseTestClient) {
        super(entitiesApiClient, objectMapper, generalProperties);
        this.extensionTestClient = extensionTestClient;
        this.baseTestClient = baseTestClient;
    }

    @MockBean(TMForumEventHandler.class)
    public TMForumEventHandler eventHandler() {
        TMForumEventHandler eventHandler = mock(TMForumEventHandler.class);
        when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
        when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());
        return eventHandler;
    }

    @Override
    protected String getEntityType() {
        return CustomerBillOnDemand.TYPE_CUSTOMER_BILL_ON_DEMAND;
    }

    @Test
    @Override
    public void deleteCustomerBillOnDemand204() throws Exception {
        CustomerBillOnDemandCreateVO createVO = CustomerBillOnDemandCreateVOTestExample.build()
                .atSchemaLocation(null)
                .lastUpdate(Instant.MAX.toString())
                .billingAccount(null)
                .relatedParty(null)
                .customerBill(null);

        HttpResponse<CustomerBillOnDemandVO> createResponse = callAndCatch(
                () -> baseTestClient.createCustomerBillOnDemand(null, createVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "CustomerBillOnDemand should have been created.");
        String id = createResponse.body().getId();

        assertEquals(HttpStatus.NO_CONTENT,
                callAndCatch(() -> extensionTestClient.deleteCustomerBillOnDemand(null, id)).getStatus(),
                "CustomerBillOnDemand should have been deleted.");

        assertEquals(HttpStatus.NOT_FOUND,
                callAndCatch(() -> baseTestClient.retrieveCustomerBillOnDemand(null, id, null)).getStatus(),
                "CustomerBillOnDemand should not exist anymore.");
    }

    @Disabled("400 is impossible to happen on deletion with the current implementation.")
    @Test
    @Override
    public void deleteCustomerBillOnDemand400() throws Exception {
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void deleteCustomerBillOnDemand401() throws Exception {
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void deleteCustomerBillOnDemand403() throws Exception {
    }

    @Test
    @Override
    public void deleteCustomerBillOnDemand404() throws Exception {
        HttpResponse<?> notFoundResponse = callAndCatch(
                () -> extensionTestClient.deleteCustomerBillOnDemand(null,
                        "urn:ngsi-ld:customer-bill-on-demand:no-such-entity"));
        assertEquals(HttpStatus.NOT_FOUND, notFoundResponse.getStatus(), "No such entity should exist.");
        Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

        notFoundResponse = callAndCatch(
                () -> extensionTestClient.deleteCustomerBillOnDemand(null, "invalid-id"));
        assertEquals(HttpStatus.NOT_FOUND, notFoundResponse.getStatus(), "No such entity should exist.");
        optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
    }

    @Override
    public void deleteCustomerBillOnDemand500() throws Exception {
    }
}
