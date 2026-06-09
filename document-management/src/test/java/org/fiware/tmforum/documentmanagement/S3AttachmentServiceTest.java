package org.fiware.tmforum.documentmanagement;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.fiware.tmforum.common.domain.AttachmentRefOrValue;
import org.fiware.tmforum.common.domain.Quantity;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.documentmanagement.s3.S3AttachmentService;
import org.fiware.tmforum.documentmanagement.s3.S3Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.net.URL;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3AttachmentServiceTest {

    private static final String ENDPOINT = "http://localhost:9000";
    private static final String BUCKET = "test-bucket";
    private static final String PRESIGNED_URL = "http://localhost:9000/test-bucket/entity-1/uuid-file.txt?presigned=true";

    /**
     * Subclass that skips the MinIO @PostConstruct initialisation so the service
     * can be unit-tested without a running S3 endpoint.
     */
    private static class TestableS3AttachmentService extends S3AttachmentService {
        TestableS3AttachmentService(S3Configuration config) {
            super(config);
        }

        @Override
        public void init() {
            // skip MinIO connection for unit tests
        }
    }

    @Mock
    private MinioClient s3Client;

    private S3Configuration config;
    private S3AttachmentService service;

    @BeforeEach
    void setUp() throws Exception {
        config = new S3Configuration();
        config.setEndpoint(ENDPOINT);
        config.setAccessKey("minioadmin");
        config.setSecretKey("minioadmin");
        config.setBucket(BUCKET);
        config.setMaxContentSize(1024 * 1024);

        service = new TestableS3AttachmentService(config);

        Field minioClientField = S3AttachmentService.class.getDeclaredField("s3Client");
        minioClientField.setAccessible(true);
        minioClientField.set(service, s3Client);
    }

    // --- validateAttachmentContent ---

    @Test
    void validateAttachmentContent_nullContent_doesNotThrow() {
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setContent(null);
        assertDoesNotThrow(() -> service.validateAttachmentContent(attachment),
                "null content should be accepted without validation");
    }

    @Test
    void validateAttachmentContent_emptyContent_doesNotThrow() {
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setContent("");
        assertDoesNotThrow(() -> service.validateAttachmentContent(attachment),
                "empty content should be accepted without validation");
    }

    @Test
    void validateAttachmentContent_validBase64WithinLimit_doesNotThrow() {
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setContent(Base64.getEncoder().encodeToString("Hello World".getBytes()));
        assertDoesNotThrow(() -> service.validateAttachmentContent(attachment),
                "valid base64 content within size limit should pass validation");
    }

    @Test
    void validateAttachmentContent_invalidBase64_throwsTmForumException() {
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setContent("not-valid-base64!!!");
        assertThrows(TmForumException.class, () -> service.validateAttachmentContent(attachment),
                "invalid base64 content should throw TmForumException");
    }

    @Test
    void validateAttachmentContent_contentExceedsMaxSize_throwsTmForumException() {
        config.setMaxContentSize(4);
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setContent(Base64.getEncoder().encodeToString(new byte[10]));
        assertThrows(TmForumException.class, () -> service.validateAttachmentContent(attachment),
                "content exceeding max size should throw TmForumException");
    }

    // --- offloadAttachments ---

    @Test
    void offloadAttachments_nullList_returnsNull() {
        assertNull(service.offloadAttachments(null, "entity-1").block(),
                "null attachment list should return null Mono");
    }

    @Test
    void offloadAttachments_emptyList_returnsEmptyList() {
        List<AttachmentRefOrValue> result = service.offloadAttachments(List.of(), "entity-1").block();
        assertNotNull(result, "empty list should return an empty list, not null");
        assertTrue(result.isEmpty(), "result should be empty");
    }

    @Test
    void offloadAttachments_nullContent_attachmentUnchanged() {
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setContent(null);

        List<AttachmentRefOrValue> result = service.offloadAttachments(List.of(attachment), "entity-1").block();

        assertNotNull(result, "result should not be null");
        assertNull(result.get(0).getContent(), "content should remain null");
        assertNull(result.get(0).getUrl(), "url should remain null when no content to offload");
        verifyNoInteractions(s3Client);
    }

    @Test
    void offloadAttachments_withContent_uploadsToS3ClearsContentAndSetsUrl() throws Exception {
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setName("file.txt");
        attachment.setMimeType("text/plain");
        attachment.setContent(Base64.getEncoder().encodeToString("file content".getBytes()));

        List<AttachmentRefOrValue> result = service.offloadAttachments(List.of(attachment), "entity-1").block();

        assertNotNull(result, "result should not be null");
        AttachmentRefOrValue processed = result.get(0);
        assertNull(processed.getContent(), "inline content should be cleared after offload");
        assertNotNull(processed.getUrl(), "url should be set to the internal S3 path");
        assertTrue(processed.getUrl().toString().startsWith(ENDPOINT + "/" + BUCKET + "/"),
                "url should be prefixed with endpoint/bucket/");
        verify(s3Client).putObject(any(PutObjectArgs.class));
    }

    @Test
    void offloadAttachments_alreadyManagedUrl_skipsUpload() throws Exception {
        URL managedUrl = new URL(ENDPOINT + "/" + BUCKET + "/entity-1/existing-file.txt");
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setUrl(managedUrl);
        attachment.setContent(Base64.getEncoder().encodeToString("data".getBytes()));

        List<AttachmentRefOrValue> result = service.offloadAttachments(List.of(attachment), "entity-1").block();

        assertNotNull(result, "result should not be null");
        assertEquals(managedUrl, result.get(0).getUrl(), "managed url should remain unchanged");
        verifyNoInteractions(s3Client);
    }

    // --- resolveAttachments ---

    @Test
    void resolveAttachments_nullList_returnsNull() {
        assertNull(service.resolveAttachments(null).block(),
                "null attachment list should return null Mono");
    }

    @Test
    void resolveAttachments_nullUrl_attachmentUnchanged() {
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setUrl(null);

        List<AttachmentRefOrValue> result = service.resolveAttachments(List.of(attachment)).block();

        assertNotNull(result, "result should not be null");
        assertNull(result.get(0).getUrl(), "url should remain null when not managed");
        verifyNoInteractions(s3Client);
    }

    @Test
    void resolveAttachments_nonManagedUrl_attachmentUnchanged() throws Exception {
        URL externalUrl = new URL("https://example.com/file.pdf");
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setUrl(externalUrl);

        List<AttachmentRefOrValue> result = service.resolveAttachments(List.of(attachment)).block();

        assertNotNull(result, "result should not be null");
        assertEquals(externalUrl, result.get(0).getUrl(), "non-managed url should be returned unchanged");
        verifyNoInteractions(s3Client);
    }

    @Test
    void resolveAttachments_managedUrl_replacesWithPresignedUrl() throws Exception {
        URL managedUrl = new URL(ENDPOINT + "/" + BUCKET + "/entity-1/uuid-file.txt");
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setUrl(managedUrl);

        when(s3Client.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn(PRESIGNED_URL);

        List<AttachmentRefOrValue> result = service.resolveAttachments(List.of(attachment)).block();

        assertNotNull(result, "result should not be null");
        assertEquals(new URL(PRESIGNED_URL), result.get(0).getUrl(), "managed url should be replaced with presigned url");
        verify(s3Client).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
    }

    // --- deleteAttachments ---

    @Test
    void deleteAttachments_nullList_noS3Calls() {
        service.deleteAttachments(null).block();
        verifyNoInteractions(s3Client);
    }

    @Test
    void deleteAttachments_nonManagedUrl_noS3Calls() throws Exception {
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setUrl(new URL("https://example.com/file.pdf"));

        service.deleteAttachments(List.of(attachment)).block();

        verifyNoInteractions(s3Client);
    }

    @Test
    void deleteAttachments_managedUrl_deletesFromS3() throws Exception {
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setUrl(new URL(ENDPOINT + "/" + BUCKET + "/entity-1/uuid-file.txt"));

        service.deleteAttachments(List.of(attachment)).block();

        verify(s3Client).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    void deleteAttachments_s3DeleteFails_doesNotThrow() throws Exception {
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setUrl(new URL(ENDPOINT + "/" + BUCKET + "/entity-1/uuid-file.txt"));

        doThrow(new RuntimeException("S3 unavailable")).when(s3Client).removeObject(any(RemoveObjectArgs.class));

        assertDoesNotThrow(() -> service.deleteAttachments(List.of(attachment)).block(),
                "S3 delete failure should be swallowed and not propagate to the caller");
    }

    // --- deleteOrphanedAttachments ---

    @Test
    void deleteOrphanedAttachments_nullExisting_noS3Calls() {
        service.deleteOrphanedAttachments(null, List.of()).block();
        verifyNoInteractions(s3Client);
    }

    @Test
    void deleteOrphanedAttachments_emptyExisting_noS3Calls() {
        service.deleteOrphanedAttachments(List.of(), List.of()).block();
        verifyNoInteractions(s3Client);
    }

    @Test
    void deleteOrphanedAttachments_managedUrlNotInUpdated_deletesFromS3() throws Exception {
        AttachmentRefOrValue existing = new AttachmentRefOrValue();
        existing.setUrl(new URL(ENDPOINT + "/" + BUCKET + "/entity-1/old-file.txt"));

        service.deleteOrphanedAttachments(List.of(existing), List.of()).block();

        verify(s3Client).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    void deleteOrphanedAttachments_managedUrlStillInUpdated_doesNotDelete() throws Exception {
        URL managedUrl = new URL(ENDPOINT + "/" + BUCKET + "/entity-1/kept-file.txt");
        AttachmentRefOrValue existing = new AttachmentRefOrValue();
        existing.setUrl(managedUrl);
        AttachmentRefOrValue updated = new AttachmentRefOrValue();
        updated.setUrl(managedUrl);

        service.deleteOrphanedAttachments(List.of(existing), List.of(updated)).block();

        verifyNoInteractions(s3Client);
    }

    @Test
    void deleteOrphanedAttachments_nonManagedUrl_doesNotDelete() throws Exception {
        AttachmentRefOrValue existing = new AttachmentRefOrValue();
        existing.setUrl(new URL("https://example.com/file.pdf"));

        service.deleteOrphanedAttachments(List.of(existing), List.of()).block();

        verifyNoInteractions(s3Client);
    }

    // --- deep copy ---

    @Test
    void offloadAttachments_deepCopiesValidForAndSize() throws Exception {
        TimePeriod validFor = new TimePeriod();
        validFor.setStartDateTime(Instant.parse("2024-01-01T00:00:00Z"));
        validFor.setEndDateTime(Instant.parse("2025-01-01T00:00:00Z"));
        Quantity size = new Quantity();
        size.setAmount(1.5f);
        size.setUnits("MB");

        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setContent(Base64.getEncoder().encodeToString("data".getBytes()));
        attachment.setName("file.txt");
        attachment.setValidFor(validFor);
        attachment.setSize(size);

        List<AttachmentRefOrValue> result = service.offloadAttachments(List.of(attachment), "entity-1").block();

        assertNotNull(result, "result should not be null");
        AttachmentRefOrValue copy = result.get(0);

        assertNotSame(validFor, copy.getValidFor(), "validFor should be a new instance, not the same reference");
        assertEquals(validFor.getStartDateTime(), copy.getValidFor().getStartDateTime(), "validFor startDateTime should be preserved");
        assertEquals(validFor.getEndDateTime(), copy.getValidFor().getEndDateTime(), "validFor endDateTime should be preserved");

        assertNotSame(size, copy.getSize(), "size should be a new instance, not the same reference");
        assertEquals(size.getAmount(), copy.getSize().getAmount(), "size amount should be preserved");
        assertEquals(size.getUnits(), copy.getSize().getUnits(), "size units should be preserved");
    }
}
