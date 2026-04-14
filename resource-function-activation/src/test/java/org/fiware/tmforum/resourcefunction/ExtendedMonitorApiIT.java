package org.fiware.tmforum.resourcefunction;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.resourcefunction.api.MonitorApiTestClient;
import org.fiware.resourcefunction.api.ext.MonitorExtensionApiTestClient;
import org.fiware.resourcefunction.api.ext.MonitorExtensionApiTestSpec;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.resourcefunction.domain.Monitor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest(packages = {"org.fiware.tmforum.resourcefunction"})
@Property(name = "apiExtension.enabled", value = "true")
public class ExtendedMonitorApiIT extends AbstractApiIT implements MonitorExtensionApiTestSpec {

    private final MonitorExtensionApiTestClient extensionTestClient;
    private final MonitorApiTestClient baseTestClient;
    private final EntitiesApiClient entitiesApiClient;

    public ExtendedMonitorApiIT(EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper,
            GeneralProperties generalProperties,
            MonitorExtensionApiTestClient extensionTestClient,
            MonitorApiTestClient baseTestClient) {
        super(entitiesApiClient, objectMapper, generalProperties);
        this.entitiesApiClient = entitiesApiClient;
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
        return Monitor.TYPE_MONITOR;
    }

    @Test
    @Override
    public void deleteMonitor204() throws Exception {
        // Monitor has no POST in the standard API — create directly via NGSI-LD
        String monitorId = "urn:ngsi-ld:monitor:" + UUID.randomUUID();
        EntityVO monitorEntity = new EntityVO()
                .atContext("https://uri.etsi.org/ngsi-ld/v1/ngsi-ld-core-context.jsonld")
                .id(URI.create(monitorId))
                .type(Monitor.TYPE_MONITOR);
        entitiesApiClient.createEntity(monitorEntity, null).block();

        assertEquals(HttpStatus.NO_CONTENT,
                callAndCatch(() -> extensionTestClient.deleteMonitor(null, monitorId)).getStatus(),
                "Monitor should have been deleted.");

        assertEquals(HttpStatus.NOT_FOUND,
                callAndCatch(() -> baseTestClient.retrieveMonitor(null, monitorId, null)).getStatus(),
                "Monitor should not exist anymore.");
    }

    @Disabled("400 is impossible to happen on deletion with the current implementation.")
    @Test
    @Override
    public void deleteMonitor400() throws Exception {
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void deleteMonitor401() throws Exception {
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void deleteMonitor403() throws Exception {
    }

    @Test
    @Override
    public void deleteMonitor404() throws Exception {
        HttpResponse<?> notFoundResponse = callAndCatch(
                () -> extensionTestClient.deleteMonitor(null, "urn:ngsi-ld:monitor:no-such-entity"));
        assertEquals(HttpStatus.NOT_FOUND, notFoundResponse.getStatus(), "No such entity should exist.");
        Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

        notFoundResponse = callAndCatch(() -> extensionTestClient.deleteMonitor(null, "invalid-id"));
        assertEquals(HttpStatus.NOT_FOUND, notFoundResponse.getStatus(), "No such entity should exist.");
        optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
    }

    @Override
    public void deleteMonitor500() throws Exception {
    }
}
