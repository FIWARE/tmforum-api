package org.fiware.tmforum.resourcefunction;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.resourcefunction.api.HealApiTestClient;
import org.fiware.resourcefunction.api.ext.HealExtensionApiTestClient;
import org.fiware.resourcefunction.api.ext.HealExtensionApiTestSpec;
import org.fiware.resourcefunction.model.HealCreateVO;
import org.fiware.resourcefunction.model.HealCreateVOTestExample;
import org.fiware.resourcefunction.model.HealVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.resourcefunction.domain.Heal;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest(packages = {"org.fiware.tmforum.resourcefunction"})
@Property(name = "apiExtension.enabled", value = "true")
public class ExtendedHealApiIT extends AbstractApiIT implements HealExtensionApiTestSpec {

    private final HealExtensionApiTestClient extensionTestClient;
    private final HealApiTestClient baseTestClient;

    public ExtendedHealApiIT(EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper,
            GeneralProperties generalProperties,
            HealExtensionApiTestClient extensionTestClient,
            HealApiTestClient baseTestClient) {
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
        return Heal.TYPE_HEAL;
    }

    @Test
    @Override
    public void deleteHeal204() throws Exception {
        HealCreateVO createVO = HealCreateVOTestExample.build().atSchemaLocation(null).healPolicy(null).resourceFunction(null);

        HttpResponse<HealVO> createResponse = callAndCatch(
                () -> baseTestClient.createHeal(null, createVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "Heal should have been created.");
        String id = createResponse.body().getId();

        assertEquals(HttpStatus.NO_CONTENT,
                callAndCatch(() -> extensionTestClient.deleteHeal(null, id)).getStatus(),
                "Heal should have been deleted.");

        assertEquals(HttpStatus.NOT_FOUND,
                callAndCatch(() -> baseTestClient.retrieveHeal(null, id, null)).getStatus(),
                "Heal should not exist anymore.");
    }

    @Disabled("400 is impossible to happen on deletion with the current implementation.")
    @Test
    @Override
    public void deleteHeal400() throws Exception {
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void deleteHeal401() throws Exception {
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void deleteHeal403() throws Exception {
    }

    @Test
    @Override
    public void deleteHeal404() throws Exception {
        HttpResponse<?> notFoundResponse = callAndCatch(
                () -> extensionTestClient.deleteHeal(null, "urn:ngsi-ld:heal:no-such-entity"));
        assertEquals(HttpStatus.NOT_FOUND, notFoundResponse.getStatus(), "No such entity should exist.");
        Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

        notFoundResponse = callAndCatch(() -> extensionTestClient.deleteHeal(null, "invalid-id"));
        assertEquals(HttpStatus.NOT_FOUND, notFoundResponse.getStatus(), "No such entity should exist.");
        optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
    }

    @Override
    public void deleteHeal500() throws Exception {
    }
}
