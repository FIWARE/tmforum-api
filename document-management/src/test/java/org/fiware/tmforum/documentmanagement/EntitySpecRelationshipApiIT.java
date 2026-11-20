package org.fiware.tmforum.documentmanagement;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.document.api.DocumentSpecificationApiTestClient;
import org.fiware.document.model.AssociationSpecificationRefVO;
import org.fiware.document.model.DocumentSpecificationCreateVO;
import org.fiware.document.model.DocumentSpecificationUpdateVO;
import org.fiware.document.model.DocumentSpecificationVO;
import org.fiware.document.model.EntitySpecificationRelationshipVO;
import org.fiware.document.model.TimePeriodVO;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.domain.AttachmentRefOrValue;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.fiware.tmforum.documentmanagement.domain.DocumentSpecification;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Round-trip of a document specification's entitySpecRelationship to stored specifications through the API. Before
 * ticket 88 the read resolved the stored target and failed, since EntitySpecificationRelationship has no
 * {@code @MappingEnabled}.
 */
@MicronautTest(packages = {"org.fiware.tmforum.documentmanagement"}, contextBuilder = FirstMatchConfigContextBuilder.class)
@Property(name = "references.external-hosts", value = EntitySpecRelationshipApiIT.EXTERNAL_HOST)
public class EntitySpecRelationshipApiIT extends AbstractApiIT {

    // association specifications are not served by this API, so the associationSpec is external
    static final String EXTERNAL_HOST = "external-tmf.example.org";
    private static final String ASSOCIATION_HREF_PREFIX =
            "https://" + EXTERNAL_HOST + "/tmf-api/serviceCatalog/v4/associationSpecification/";
    private static final String ASSOCIATION_ID = "urn:ngsi-ld:association-specification:remote-1";
    private static final String ASSOCIATION_NAME = "Held by another deployment";
    private static final String REFERRED_TYPE_ASSOCIATION = "AssociationSpecification";
    // ReferenceValidationService checks a local target's type against the relationship's @referredType
    private static final String REFERRED_TYPE_SPECIFICATION = DocumentSpecification.TYPE_DOCUMENT_SPECIFICATION;
    private static final String SPECIFICATION_NAME = "Target document specification";
    private static final String RELATIONSHIP_TYPE = "dependency";
    private static final String ROLE = "requires";
    private static final Instant VALID_FROM = Instant.parse("2026-01-01T00:00:00Z");
    // written into each relationship's name, so a read-back tells the patched payload from the created one
    private static final String CREATE = "create";
    private static final String PATCH = "patch";

    /**
     * How much of a relationship a test case posts.
     */
    enum Payload {
        // id, @referredType and the required relationshipType
        MINIMAL,
        // every field, with an external associationSpec
        FULL
    }

    private final DocumentSpecificationApiTestClient documentSpecificationApiTestClient;

    public EntitySpecRelationshipApiIT(EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper,
            GeneralProperties generalProperties,
            DocumentSpecificationApiTestClient documentSpecificationApiTestClient) {
        super(entitiesApiClient, objectMapper, generalProperties);
        this.documentSpecificationApiTestClient = documentSpecificationApiTestClient;
    }

    @MockBean(TMForumEventHandler.class)
    public TMForumEventHandler eventHandler() {
        TMForumEventHandler eventHandler = mock(TMForumEventHandler.class);
        when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
        when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());
        return eventHandler;
    }

    // no attachments are posted, so the S3 offloading is passed through
    @MockBean(AttachmentService.class)
    public AttachmentService attachmentService() {
        AttachmentService attachmentService = mock(AttachmentService.class);
        when(attachmentService.offloadAttachments(any(), any())).thenAnswer(i -> {
            List<AttachmentRefOrValue> list = i.getArgument(0);
            return list == null ? Mono.empty() : Mono.just(list);
        });
        when(attachmentService.resolveAttachments(any())).thenAnswer(i -> {
            List<AttachmentRefOrValue> list = i.getArgument(0);
            return list == null ? Mono.empty() : Mono.just(list);
        });
        when(attachmentService.deleteAttachments(any())).thenReturn(Mono.empty());
        when(attachmentService.deleteOrphanedAttachments(any(), any())).thenReturn(Mono.empty());
        return attachmentService;
    }

    private static Stream<Arguments> relationships() {
        return Stream.of(
                Arguments.of("A minimal relationship to a stored specification should round-trip.", Payload.MINIMAL, 1),
                Arguments.of("A full relationship to a stored specification should round-trip.", Payload.FULL, 1),
                Arguments.of("Full relationships to several stored specifications should round-trip.", Payload.FULL,
                        2));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("relationships")
    public void entitySpecRelationshipToStoredSpecificationsRoundTrips(String message, Payload payload, int count)
            throws Exception {
        List<String> targetIds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            targetIds.add(createSpecification(specificationCreate()));
        }

        List<EntitySpecificationRelationshipVO> created = relationships(targetIds, payload, CREATE);
        DocumentSpecificationCreateVO createVO = specificationCreate();
        createVO.setEntitySpecRelationship(created);
        HttpResponse<DocumentSpecificationVO> createResponse = callAndCatch(
                () -> documentSpecificationApiTestClient.createDocumentSpecification(null, createVO));
        assertEquals(HttpStatus.CREATED, createResponse.getStatus(), message);
        String id = createResponse.body().getId();
        assertRoundTrip(created, createResponse.body(), "create response: " + message);
        assertRoundTrip(created, retrieve(id), "retrieval after create: " + message);

        // same target ids, so the patch updates the stored instances on both brokers
        List<EntitySpecificationRelationshipVO> patched = relationships(targetIds, payload, PATCH);
        DocumentSpecificationUpdateVO updateVO = new DocumentSpecificationUpdateVO();
        updateVO.setEntitySpecRelationship(patched);
        HttpResponse<DocumentSpecificationVO> patchResponse = callAndCatch(
                () -> documentSpecificationApiTestClient.patchDocumentSpecification(null, id, updateVO));
        assertEquals(HttpStatus.OK, patchResponse.getStatus(), message);
        assertRoundTrip(patched, patchResponse.body(), "patch response: " + message);
        assertRoundTrip(patched, retrieve(id), "retrieval after patch: " + message);
    }

    private static List<EntitySpecificationRelationshipVO> relationships(List<String> targetIds, Payload payload,
            String stage) {
        return targetIds.stream().map(targetId -> relationship(targetId, payload, stage)).toList();
    }

    private static EntitySpecificationRelationshipVO relationship(String targetId, Payload payload, String stage) {
        EntitySpecificationRelationshipVO relationship = new EntitySpecificationRelationshipVO()
                .id(targetId)
                .relationshipType(RELATIONSHIP_TYPE)
                .atReferredType(REFERRED_TYPE_SPECIFICATION);
        if (payload == Payload.MINIMAL) {
            return relationship;
        }
        return relationship
                .href(URI.create(targetId))
                .name(String.format("%s (%s)", targetId, stage))
                .role(ROLE)
                .validFor(new TimePeriodVO().startDateTime(VALID_FROM))
                .associationSpec(new AssociationSpecificationRefVO()
                        .id(ASSOCIATION_ID)
                        .href(URI.create(ASSOCIATION_HREF_PREFIX + ASSOCIATION_ID))
                        .name(String.format("%s (%s)", ASSOCIATION_NAME, stage))
                        .atReferredType(REFERRED_TYPE_ASSOCIATION));
    }

    private void assertRoundTrip(List<EntitySpecificationRelationshipVO> posted, DocumentSpecificationVO readBack,
            String message) {
        assertEquals(multiset(objectMapper.valueToTree(posted)),
                multiset(objectMapper.valueToTree(readBack.getEntitySpecRelationship())), message);
    }

    private static DocumentSpecificationCreateVO specificationCreate() {
        DocumentSpecificationCreateVO createVO = new DocumentSpecificationCreateVO();
        createVO.setName(SPECIFICATION_NAME);
        return createVO;
    }

    private String createSpecification(DocumentSpecificationCreateVO createVO) throws Exception {
        HttpResponse<DocumentSpecificationVO> response = callAndCatch(
                () -> documentSpecificationApiTestClient.createDocumentSpecification(null, createVO));
        assertEquals(HttpStatus.CREATED, response.getStatus(), "The target specification should have been created.");
        return response.body().getId();
    }

    private DocumentSpecificationVO retrieve(String id) throws Exception {
        HttpResponse<DocumentSpecificationVO> retrieved = callAndCatch(
                () -> documentSpecificationApiTestClient.retrieveDocumentSpecification(null, id, null));
        assertEquals(HttpStatus.OK, retrieved.getStatus(), "The document specification should be retrievable.");
        return retrieved.body();
    }

    @Override
    protected String getEntityType() {
        return DocumentSpecification.TYPE_DOCUMENT_SPECIFICATION;
    }
}
