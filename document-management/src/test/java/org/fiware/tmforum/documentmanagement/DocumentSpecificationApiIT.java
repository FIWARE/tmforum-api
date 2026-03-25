package org.fiware.tmforum.documentmanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.document.api.DocumentSpecificationApiTestClient;
import org.fiware.document.api.DocumentSpecificationApiTestSpec;
import org.fiware.document.model.AttachmentRefOrValueVO;
import org.fiware.document.model.DocumentSpecificationCreateVO;
import org.fiware.document.model.DocumentSpecificationStatusTypeVO;
import org.fiware.document.model.DocumentSpecificationVO;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.documentmanagement.domain.DocumentSpecification;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MicronautTest(packages = {"org.fiware.tmforum.documentmanagement"})
public class DocumentSpecificationApiIT extends AbstractApiIT implements DocumentSpecificationApiTestSpec {

    public final DocumentSpecificationApiTestClient documentSpecificationApiTestClient;

    private String message;
    private DocumentSpecificationCreateVO documentSpecificationCreateVO;
    private DocumentSpecificationVO expectedDocSpec;

    public DocumentSpecificationApiIT(
            DocumentSpecificationApiTestClient documentSpecificationApiTestClient,
            EntitiesApiClient entitiesApiClient,
            ObjectMapper objectMapper,
            GeneralProperties generalProperties) {
        super(entitiesApiClient, objectMapper, generalProperties);
        this.documentSpecificationApiTestClient = documentSpecificationApiTestClient;
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

    @ParameterizedTest
    @MethodSource("provideValidDocumentSpecifications")
    public void createDocumentSpecification201(String message, DocumentSpecificationCreateVO createVO,
                                                DocumentSpecificationVO expectedDocSpec) throws Exception {
        this.message = message;
        this.documentSpecificationCreateVO = createVO;
        this.expectedDocSpec = expectedDocSpec;
        createDocumentSpecification201();
    }

    @Override
    public void createDocumentSpecification201() throws Exception {
        HttpResponse<DocumentSpecificationVO> response = callAndCatch(
                () -> documentSpecificationApiTestClient.createDocumentSpecification(null, documentSpecificationCreateVO));

        assertEquals(HttpStatus.CREATED, response.getStatus(), message);
        assertNotNull(response.body(), message);
        assertNotNull(response.body().getId(), message);
        assertEquals(expectedDocSpec.getName(), response.body().getName(), message);
    }

    private static Stream<Arguments> provideValidDocumentSpecifications() {
        List<Arguments> testEntries = new ArrayList<>();

        DocumentSpecificationCreateVO simpleCreateVO = new DocumentSpecificationCreateVO();
        simpleCreateVO.setName("Test Document Specification");
        simpleCreateVO.setDescription("A test document specification");
        simpleCreateVO.setVersion("1.0.0");
        DocumentSpecificationVO simpleExpected = new DocumentSpecificationVO();
        simpleExpected.setName("Test Document Specification");
        testEntries.add(Arguments.of("A simple document specification should be created.", simpleCreateVO, simpleExpected));

        DocumentSpecificationCreateVO withLifecycleVO = new DocumentSpecificationCreateVO();
        withLifecycleVO.setName("Document with Lifecycle");
        withLifecycleVO.setLifecycleStatus(DocumentSpecificationStatusTypeVO.APPROVED);
        DocumentSpecificationVO lifecycleExpected = new DocumentSpecificationVO();
        lifecycleExpected.setName("Document with Lifecycle");
        testEntries.add(Arguments.of("A document specification with lifecycle status should be created.", withLifecycleVO, lifecycleExpected));

        return testEntries.stream();
    }

    @Test
    public void createDocumentSpecificationWithAttachment201() throws Exception {
        DocumentSpecificationCreateVO createVO = new DocumentSpecificationCreateVO();
        createVO.setName("Document with Attachment");
        createVO.setVersion("1.0.0");

        AttachmentRefOrValueVO attachment = new AttachmentRefOrValueVO();
        attachment.setName("test-file.txt");
        attachment.setMimeType("text/plain");
        attachment.setContent(Base64.getEncoder().encodeToString("Hello World".getBytes()));
        createVO.setAttachment(List.of(attachment));

        HttpResponse<DocumentSpecificationVO> response = callAndCatch(
                () -> documentSpecificationApiTestClient.createDocumentSpecification(null, createVO));

        assertEquals(HttpStatus.CREATED, response.getStatus(), "Document specification with attachment should be created.");
        assertNotNull(response.body());
        assertNotNull(response.body().getAttachment());
        assertFalse(response.body().getAttachment().isEmpty());
        // The attachment content should be S3 retrieval info, not the original content
        String content = response.body().getAttachment().get(0).getContent();
        assertTrue(content.startsWith("s3ref:"), "Content should be S3 retrieval info");
    }

    @Test
    @Override
    public void createDocumentSpecification400() throws Exception {
        // Test creation with missing required field (name)
        DocumentSpecificationCreateVO createVO = new DocumentSpecificationCreateVO();
        createVO.setDescription("Missing name field");

        HttpResponse<DocumentSpecificationVO> response = callAndCatch(
                () -> documentSpecificationApiTestClient.createDocumentSpecification(null, createVO));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus(), "Creation without name should fail.");

        Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void createDocumentSpecification401() throws Exception {
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void createDocumentSpecification403() throws Exception {
    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void createDocumentSpecification405() throws Exception {
    }

    @Disabled("DocumentSpecification doesn't have 'implicit' entities and id is generated, no conflict possible.")
    @Test
    @Override
    public void createDocumentSpecification409() throws Exception {
    }

    @Override
    public void createDocumentSpecification500() throws Exception {
    }

    @Test
    @Override
    public void deleteDocumentSpecification204() throws Exception {
        // First create a document specification
        DocumentSpecificationCreateVO createVO = new DocumentSpecificationCreateVO();
        createVO.setName("Document to Delete");
        createVO.setVersion("1.0.0");

        HttpResponse<DocumentSpecificationVO> createResponse = callAndCatch(
                () -> documentSpecificationApiTestClient.createDocumentSpecification(null, createVO));

        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The document specification should have been created first.");
        String id = createResponse.body().getId();

        // Then delete it
        HttpResponse<?> deleteResponse = callAndCatch(
                () -> documentSpecificationApiTestClient.deleteDocumentSpecification(null, id));

        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatus(), "The document specification should have been deleted.");

        // Verify it no longer exists
        assertEquals(HttpStatus.NOT_FOUND,
                callAndCatch(() -> documentSpecificationApiTestClient.retrieveDocumentSpecification(null, id, null)).getStatus(),
                "The document specification should not exist anymore.");
    }

    @Disabled("400 is impossible to happen on deletion with the current implementation.")
    @Test
    @Override
    public void deleteDocumentSpecification400() throws Exception {
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void deleteDocumentSpecification401() throws Exception {
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void deleteDocumentSpecification403() throws Exception {
    }

    @Test
    @Override
    public void deleteDocumentSpecification404() throws Exception {
        HttpResponse<?> notFoundResponse = callAndCatch(
                () -> documentSpecificationApiTestClient.deleteDocumentSpecification(null,
                        "urn:ngsi-ld:document-specification:non-existent"));

        assertEquals(HttpStatus.NOT_FOUND, notFoundResponse.getStatus(), "No such document specification should exist.");

        Optional<ErrorDetails> optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

        // Also test with invalid id format
        notFoundResponse = callAndCatch(
                () -> documentSpecificationApiTestClient.deleteDocumentSpecification(null, "invalid-id"));
        assertEquals(HttpStatus.NOT_FOUND, notFoundResponse.getStatus(), "Invalid ID should return not found.");

        optionalErrorDetails = notFoundResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void deleteDocumentSpecification405() throws Exception {
    }

    @Disabled("Impossible status.")
    @Test
    @Override
    public void deleteDocumentSpecification409() throws Exception {
    }

    @Override
    public void deleteDocumentSpecification500() throws Exception {
    }

    @Test
    @Override
    public void listDocumentSpecification200() throws Exception {
        List<DocumentSpecificationVO> expectedSpecs = new ArrayList<>();

        // Create document specifications
        for (int i = 0; i < 10; i++) {
            DocumentSpecificationCreateVO createVO = new DocumentSpecificationCreateVO();
            createVO.setName("Document " + i);
            createVO.setVersion("1.0.0");

            HttpResponse<DocumentSpecificationVO> createResponse = callAndCatch(
                    () -> documentSpecificationApiTestClient.createDocumentSpecification(null, createVO));
            assertEquals(HttpStatus.CREATED, createResponse.getStatus());
            expectedSpecs.add(createResponse.body());
        }

        // List all
        HttpResponse<List<DocumentSpecificationVO>> listResponse = callAndCatch(
                () -> documentSpecificationApiTestClient.listDocumentSpecification(null, null, null, null));

        assertEquals(HttpStatus.OK, listResponse.getStatus(), "The list should be accessible.");
        assertEquals(expectedSpecs.size(), listResponse.body().size(), "All document specifications should have been returned.");

        // Test pagination
        Integer limit = 5;
        HttpResponse<List<DocumentSpecificationVO>> firstPartResponse = callAndCatch(
                () -> documentSpecificationApiTestClient.listDocumentSpecification(null, null, 0, limit));
        assertEquals(limit, firstPartResponse.body().size(), "Only the requested number of entries should be returned.");

        HttpResponse<List<DocumentSpecificationVO>> secondPartResponse = callAndCatch(
                () -> documentSpecificationApiTestClient.listDocumentSpecification(null, null, limit, limit));
        assertEquals(limit, secondPartResponse.body().size(), "Only the requested number of entries should be returned.");
    }

    @Test
    @Override
    public void listDocumentSpecification400() throws Exception {
        HttpResponse<List<DocumentSpecificationVO>> badRequestResponse = callAndCatch(
                () -> documentSpecificationApiTestClient.listDocumentSpecification(null, null, -1, null));
        assertEquals(HttpStatus.BAD_REQUEST, badRequestResponse.getStatus(), "Negative offsets are impossible.");

        Optional<ErrorDetails> optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");

        badRequestResponse = callAndCatch(
                () -> documentSpecificationApiTestClient.listDocumentSpecification(null, null, null, -1));
        assertEquals(HttpStatus.BAD_REQUEST, badRequestResponse.getStatus(), "Negative limits are impossible.");

        optionalErrorDetails = badRequestResponse.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should be provided.");
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void listDocumentSpecification401() throws Exception {
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void listDocumentSpecification403() throws Exception {
    }

    @Disabled("Not found is not possible here, will be answered with an empty list instead.")
    @Test
    @Override
    public void listDocumentSpecification404() throws Exception {
    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void listDocumentSpecification405() throws Exception {
    }

    @Disabled("Impossible status.")
    @Test
    @Override
    public void listDocumentSpecification409() throws Exception {
    }

    @Override
    public void listDocumentSpecification500() throws Exception {
    }

    @Disabled("PATCH is not supported for DocumentSpecification per design requirements.")
    @Test
    @Override
    public void patchDocumentSpecification200() throws Exception {
    }

    @Disabled("PATCH is not supported for DocumentSpecification per design requirements.")
    @Test
    @Override
    public void patchDocumentSpecification400() throws Exception {
    }

    @Disabled("PATCH is not supported for DocumentSpecification per design requirements.")
    @Test
    @Override
    public void patchDocumentSpecification401() throws Exception {
    }

    @Disabled("PATCH is not supported for DocumentSpecification per design requirements.")
    @Test
    @Override
    public void patchDocumentSpecification403() throws Exception {
    }

    @Disabled("PATCH is not supported for DocumentSpecification per design requirements.")
    @Test
    @Override
    public void patchDocumentSpecification404() throws Exception {
    }

    @Disabled("PATCH is not supported for DocumentSpecification per design requirements.")
    @Test
    @Override
    public void patchDocumentSpecification405() throws Exception {
    }

    @Disabled("PATCH is not supported for DocumentSpecification per design requirements.")
    @Test
    @Override
    public void patchDocumentSpecification409() throws Exception {
    }

    @Disabled("PATCH is not supported for DocumentSpecification per design requirements.")
    @Test
    @Override
    public void patchDocumentSpecification500() throws Exception {
    }

    @Test
    @Override
    public void retrieveDocumentSpecification200() throws Exception {
        // Create a document specification
        DocumentSpecificationCreateVO createVO = new DocumentSpecificationCreateVO();
        createVO.setName("Document to Retrieve");
        createVO.setVersion("1.0.0");

        HttpResponse<DocumentSpecificationVO> createResponse = callAndCatch(
                () -> documentSpecificationApiTestClient.createDocumentSpecification(null, createVO));

        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The document specification should have been created first.");
        String id = createResponse.body().getId();

        // Retrieve it
        HttpResponse<DocumentSpecificationVO> retrieveResponse = callAndCatch(
                () -> documentSpecificationApiTestClient.retrieveDocumentSpecification(null, id, null));

        assertEquals(HttpStatus.OK, retrieveResponse.getStatus(), "The retrieval should be ok.");
        assertNotNull(retrieveResponse.body());
        assertEquals(id, retrieveResponse.body().getId());
        assertEquals("Document to Retrieve", retrieveResponse.body().getName());
    }

    @Test
    public void retrieveDocumentSpecificationWithHydratedAttachment() throws Exception {
        // Create a document specification with attachment
        DocumentSpecificationCreateVO createVO = new DocumentSpecificationCreateVO();
        createVO.setName("Document with Hydrated Attachment");
        createVO.setVersion("1.0.0");

        String originalContent = "Hello World from S3!";
        AttachmentRefOrValueVO attachment = new AttachmentRefOrValueVO();
        attachment.setName("test-file.txt");
        attachment.setMimeType("text/plain");
        attachment.setContent(Base64.getEncoder().encodeToString(originalContent.getBytes()));
        createVO.setAttachment(List.of(attachment));

        HttpResponse<DocumentSpecificationVO> createResponse = callAndCatch(
                () -> documentSpecificationApiTestClient.createDocumentSpecification(null, createVO));

        assertEquals(HttpStatus.CREATED, createResponse.getStatus());
        String id = createResponse.body().getId();

        // Retrieve it - the attachment should be hydrated
        HttpResponse<DocumentSpecificationVO> retrieveResponse = callAndCatch(
                () -> documentSpecificationApiTestClient.retrieveDocumentSpecification(null, id, null));

        assertEquals(HttpStatus.OK, retrieveResponse.getStatus());
        assertNotNull(retrieveResponse.body().getAttachment());
        assertFalse(retrieveResponse.body().getAttachment().isEmpty());

        // The content should be the original content (hydrated from S3)
        String retrievedContent = retrieveResponse.body().getAttachment().get(0).getContent();
        String decodedContent = new String(Base64.getDecoder().decode(retrievedContent));
        assertEquals(originalContent, decodedContent);
    }

    @Disabled("400 cannot happen, only 404")
    @Test
    @Override
    public void retrieveDocumentSpecification400() throws Exception {
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void retrieveDocumentSpecification401() throws Exception {
    }

    @Disabled("Security is handled externally, thus 401 and 403 cannot happen.")
    @Test
    @Override
    public void retrieveDocumentSpecification403() throws Exception {
    }

    @Test
    @Override
    public void retrieveDocumentSpecification404() throws Exception {
        HttpResponse<DocumentSpecificationVO> response = callAndCatch(
                () -> documentSpecificationApiTestClient.retrieveDocumentSpecification(null,
                        "urn:ngsi-ld:document-specification:non-existent", null));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatus(), "No such document specification should exist.");

        Optional<ErrorDetails> optionalErrorDetails = response.getBody(ErrorDetails.class);
        assertTrue(optionalErrorDetails.isPresent(), "Error details should have been provided.");
    }

    @Disabled("Prohibited by the framework.")
    @Test
    @Override
    public void retrieveDocumentSpecification405() throws Exception {
    }

    @Disabled("Conflict not possible on retrieval")
    @Test
    @Override
    public void retrieveDocumentSpecification409() throws Exception {
    }

    @Override
    public void retrieveDocumentSpecification500() throws Exception {
    }
}
