package org.fiware.tmforum.productordering;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.productordering.api.CancelProductOrderApiTestClient;
import org.fiware.productordering.api.ProductOrderApiTestClient;
import org.fiware.productordering.api.ext.CancelProductOrderExtensionApiTestClient;
import org.fiware.productordering.api.ext.CancelProductOrderExtensionApiTestSpec;
import org.fiware.productordering.model.*;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.productordering.domain.CancelProductOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest(packages = {"org.fiware.tmforum.productordering"})
@Property(name = "apiExtension.enabled", value = "true")
public class ExtendedCancelProductOrderApiIT extends AbstractApiIT
        implements CancelProductOrderExtensionApiTestSpec {

    private final CancelProductOrderExtensionApiTestClient extensionTestClient;
    private final CancelProductOrderApiTestClient baseTestClient;
    private final ProductOrderApiTestClient productOrderApiTestClient;

    private String productOrderId;

    private Clock clock = mock(Clock.class);

    @MockBean(Clock.class)
    public Clock clock() {
        return clock;
    }

    @MockBean(TMForumEventHandler.class)
    public TMForumEventHandler eventHandler() {
        TMForumEventHandler eventHandler = mock(TMForumEventHandler.class);
        when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
        when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());
        return eventHandler;
    }

    public ExtendedCancelProductOrderApiIT(EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper,
            GeneralProperties generalProperties,
            CancelProductOrderExtensionApiTestClient extensionTestClient,
            CancelProductOrderApiTestClient baseTestClient,
            ProductOrderApiTestClient productOrderApiTestClient) {
        super(entitiesApiClient, objectMapper, generalProperties);
        this.extensionTestClient = extensionTestClient;
        this.baseTestClient = baseTestClient;
        this.productOrderApiTestClient = productOrderApiTestClient;
    }

    @BeforeEach
    public void setupProductOrder() {
        productOrderId = productOrderApiTestClient.createProductOrder(null,
                ProductOrderCreateVOTestExample.build().atSchemaLocation(null).billingAccount(null))
                .body().getId();
    }

    @AfterEach
    public void cleanProductOrder() {
        productOrderApiTestClient.deleteProductOrder(null, productOrderId);
    }

    @Override
    protected String getEntityType() {
        return CancelProductOrder.TYPE_CANCEL_PRODUCT_ORDER;
    }

    @Test
    @Override
    public void deleteCancelProductOrder204() throws Exception {
        CancelProductOrderCreateVO createVO = CancelProductOrderCreateVOTestExample.build()
                .atSchemaLocation(null)
                .productOrder(ProductOrderRefVOTestExample.build().atSchemaLocation(null).id(productOrderId));

        HttpResponse<CancelProductOrderVO> createResponse = callAndCatch(
                () -> baseTestClient.createCancelProductOrder(null, createVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "CancelProductOrder should have been created.");
        String id = createResponse.body().getId();

        assertEquals(HttpStatus.NO_CONTENT,
                callAndCatch(() -> extensionTestClient.deleteCancelProductOrder(null, id)).getStatus(),
                "CancelProductOrder should have been deleted.");

        assertEquals(HttpStatus.NOT_FOUND,
                callAndCatch(() -> baseTestClient.retrieveCancelProductOrder(null, id, null)).getStatus(),
                "CancelProductOrder should not exist anymore.");
    }

    @Disabled("400 is impossible to happen on deletion with the current implementation.")
    @Test
    @Override
    public void deleteCancelProductOrder400() throws Exception {
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void deleteCancelProductOrder401() throws Exception {
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void deleteCancelProductOrder403() throws Exception {
    }

    @Test
    @Override
    public void deleteCancelProductOrder404() throws Exception {
        HttpResponse<?> notFoundResponse = callAndCatch(
                () -> extensionTestClient.deleteCancelProductOrder(null,
                        "urn:ngsi-ld:cancel-product-order:no-such-entity"));
        assertEquals(HttpStatus.NOT_FOUND, notFoundResponse.getStatus(), "No such entity should exist.");
        Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

        notFoundResponse = callAndCatch(
                () -> extensionTestClient.deleteCancelProductOrder(null, "invalid-id"));
        assertEquals(HttpStatus.NOT_FOUND, notFoundResponse.getStatus(), "No such entity should exist.");
        optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
    }

    @Override
    public void deleteCancelProductOrder500() throws Exception {
    }
}
