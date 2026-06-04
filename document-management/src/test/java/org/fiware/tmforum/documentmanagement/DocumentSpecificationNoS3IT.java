package org.fiware.tmforum.documentmanagement;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.context.annotation.Property;
import org.fiware.document.api.DocumentSpecificationApiTestClient;
import org.fiware.document.model.AttachmentRefOrValueVO;
import org.fiware.document.model.DocumentSpecificationCreateVO;
import org.fiware.document.model.DocumentSpecificationUpdateVO;
import org.fiware.document.model.DocumentSpecificationVO;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.documentmanagement.domain.DocumentSpecification;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest(packages = {"org.fiware.tmforum.documentmanagement"})
@Property(name = "s3.enabled", value = "false")
public class DocumentSpecificationNoS3IT extends AbstractApiIT {

    private final DocumentSpecificationApiTestClient testClient;

    public DocumentSpecificationNoS3IT(
            DocumentSpecificationApiTestClient testClient,
            EntitiesApiClient entitiesApiClient,
            GeneralProperties generalProperties,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        super(entitiesApiClient, objectMapper, generalProperties);
        this.testClient = testClient;
    }

    @Override
    protected String getEntityType() {
        return DocumentSpecification.TYPE_DOCUMENT_SPECIFICATION;
    }

    @MockBean(TMForumEventHandler.class)
    public TMForumEventHandler eventHandler() {
        TMForumEventHandler eventHandler = mock(TMForumEventHandler.class);
        when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
        when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());
        return eventHandler;
    }

    @Test
    public void createWithInlineContent_rejected() throws Exception {
        DocumentSpecificationCreateVO createVO = new DocumentSpecificationCreateVO();
        createVO.setName("Document with Inline Content");
        AttachmentRefOrValueVO attachment = new AttachmentRefOrValueVO();
        attachment.setContent(Base64.getEncoder().encodeToString("Hello World".getBytes()));
        createVO.setAttachment(List.of(attachment));

        HttpResponse<DocumentSpecificationVO> response = callAndCatch(
                () -> testClient.createDocumentSpecification(null, createVO));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus(),
                "Inline content must be rejected when no AttachmentService is configured.");
    }

    @Test
    public void patchWithInlineContent_rejected() throws Exception {
        DocumentSpecificationCreateVO createVO = new DocumentSpecificationCreateVO();
        createVO.setName("Document for Patch");
        HttpResponse<DocumentSpecificationVO> created = callAndCatch(
                () -> testClient.createDocumentSpecification(null, createVO));
        assertEquals(HttpStatus.CREATED, created.getStatus());

        AttachmentRefOrValueVO attachment = new AttachmentRefOrValueVO();
        attachment.setContent(Base64.getEncoder().encodeToString("data".getBytes()));
        DocumentSpecificationUpdateVO updateVO = new DocumentSpecificationUpdateVO();
        updateVO.setAttachment(List.of(attachment));

        HttpResponse<DocumentSpecificationVO> response = callAndCatch(
                () -> testClient.patchDocumentSpecification(null, created.body().getId(), updateVO));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus(),
                "Inline content must be rejected when no AttachmentService is configured.");
    }
}
