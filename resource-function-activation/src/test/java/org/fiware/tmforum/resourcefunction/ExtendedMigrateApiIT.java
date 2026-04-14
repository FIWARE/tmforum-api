package org.fiware.tmforum.resourcefunction;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.resourcefunction.api.MigrateApiTestClient;
import org.fiware.resourcefunction.api.ext.MigrateExtensionApiTestClient;
import org.fiware.resourcefunction.api.ext.MigrateExtensionApiTestSpec;
import org.fiware.resourcefunction.model.MigrateCreateVO;
import org.fiware.resourcefunction.model.MigrateCreateVOTestExample;
import org.fiware.resourcefunction.model.MigrateVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.resourcefunction.domain.Migrate;
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
public class ExtendedMigrateApiIT extends AbstractApiIT implements MigrateExtensionApiTestSpec {

    private final MigrateExtensionApiTestClient extensionTestClient;
    private final MigrateApiTestClient baseTestClient;

    public ExtendedMigrateApiIT(EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper,
            GeneralProperties generalProperties,
            MigrateExtensionApiTestClient extensionTestClient,
            MigrateApiTestClient baseTestClient) {
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
        return Migrate.TYPE_MIGRATE;
    }

    @Test
    @Override
    public void deleteMigrate204() throws Exception {
        MigrateCreateVO createVO = MigrateCreateVOTestExample.build().atSchemaLocation(null).resourceFunction(null).place(null);

        HttpResponse<MigrateVO> createResponse = callAndCatch(
                () -> baseTestClient.createMigrate(null, createVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "Migrate should have been created.");
        String id = createResponse.body().getId();

        assertEquals(HttpStatus.NO_CONTENT,
                callAndCatch(() -> extensionTestClient.deleteMigrate(null, id)).getStatus(),
                "Migrate should have been deleted.");

        assertEquals(HttpStatus.NOT_FOUND,
                callAndCatch(() -> baseTestClient.retrieveMigrate(null, id, null)).getStatus(),
                "Migrate should not exist anymore.");
    }

    @Disabled("400 is impossible to happen on deletion with the current implementation.")
    @Test
    @Override
    public void deleteMigrate400() throws Exception {
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void deleteMigrate401() throws Exception {
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void deleteMigrate403() throws Exception {
    }

    @Test
    @Override
    public void deleteMigrate404() throws Exception {
        HttpResponse<?> notFoundResponse = callAndCatch(
                () -> extensionTestClient.deleteMigrate(null, "urn:ngsi-ld:migrate:no-such-entity"));
        assertEquals(HttpStatus.NOT_FOUND, notFoundResponse.getStatus(), "No such entity should exist.");
        Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

        notFoundResponse = callAndCatch(() -> extensionTestClient.deleteMigrate(null, "invalid-id"));
        assertEquals(HttpStatus.NOT_FOUND, notFoundResponse.getStatus(), "No such entity should exist.");
        optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
    }

    @Override
    public void deleteMigrate500() throws Exception {
    }
}
