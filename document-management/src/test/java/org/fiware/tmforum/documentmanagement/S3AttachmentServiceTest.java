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
    private MinioClient minioClient;

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

        Field minioClientField = S3AttachmentService.class.getDeclaredField("minioClient");
        minioClientField.setAccessible(true);
        minioClientField.set(service, minioClient);
    }

    // --- validateAttachmentContent ---

    @Test
    void validateAttachmentContent_nullContent_doesNotThrow() {
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setContent(null);
        assertDoesNotThrow(() -> service.validateAttachmentContent(attachment));
    }

    @Test
    void validateAttachmentContent_emptyContent_doesNotThrow() {
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setContent("");
        assertDoesNotThrow(() -> service.validateAttachmentContent(attachment));
    }

    @Test
    void validateAttachmentContent_validBase64WithinLimit_doesNotThrow() {
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setContent(Base64.getEncoder().encodeToString("Hello World".getBytes()));
        assertDoesNotThrow(() -> service.validateAttachmentContent(attachment));
    }

    @Test
    void validateAttachmentContent_invalidBase64_throwsTmForumException() {
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setContent("not-valid-base64!!!");
        assertThrows(TmForumException.class, () -> service.validateAttachmentContent(attachment));
    }

    @Test
    void validateAttachmentContent_contentExceedsMaxSize_throwsTmForumException() {
        config.setMaxContentSize(4);
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setContent(Base64.getEncoder().encodeToString(new byte[10]));
        assertThrows(TmForumException.class, () -> service.validateAttachmentContent(attachment));
    }

    // --- offloadAttachments ---

    @Test
    void offloadAttachments_nullList_returnsNull() {
        assertNull(service.offloadAttachments(null, "entity-1").block());
    }

    @Test
    void offloadAttachments_emptyList_returnsEmptyList() {
        List<AttachmentRefOrValue> result = service.offloadAttachments(List.of(), "entity-1").block();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void offloadAttachments_nullContent_attachmentUnchanged() {
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setContent(null);

        List<AttachmentRefOrValue> result = service.offloadAttachments(List.of(attachment), "entity-1").block();

        assertNotNull(result);
        assertNull(result.get(0).getContent());
        assertNull(result.get(0).getUrl());
        verifyNoInteractions(minioClient);
    }

    @Test
    void offloadAttachments_withContent_uploadsToS3ClearsContentAndSetsUrl() throws Exception {
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setName("file.txt");
        attachment.setMimeType("text/plain");
        attachment.setContent(Base64.getEncoder().encodeToString("file content".getBytes()));

        List<AttachmentRefOrValue> result = service.offloadAttachments(List.of(attachment), "entity-1").block();

        assertNotNull(result);
        AttachmentRefOrValue processed = result.get(0);
        assertNull(processed.getContent(), "inline content should be cleared after offload");
        assertNotNull(processed.getUrl(), "url should be set to the internal S3 path");
        assertTrue(processed.getUrl().toString().startsWith(ENDPOINT + "/" + BUCKET + "/"),
                "url should be prefixed with endpoint/bucket/");
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void offloadAttachments_alreadyManagedUrl_skipsUpload() throws Exception {
        URL managedUrl = new URL(ENDPOINT + "/" + BUCKET + "/entity-1/existing-file.txt");
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setUrl(managedUrl);
        attachment.setContent(Base64.getEncoder().encodeToString("data".getBytes()));

        List<AttachmentRefOrValue> result = service.offloadAttachments(List.of(attachment), "entity-1").block();

        assertNotNull(result);
        assertEquals(managedUrl, result.get(0).getUrl(), "managed url should remain unchanged");
        verifyNoInteractions(minioClient);
    }

    // --- resolveAttachments ---

    @Test
    void resolveAttachments_nullList_returnsNull() {
        assertNull(service.resolveAttachments(null).block());
    }

    @Test
    void resolveAttachments_nullUrl_attachmentUnchanged() {
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setUrl(null);

        List<AttachmentRefOrValue> result = service.resolveAttachments(List.of(attachment)).block();

        assertNotNull(result);
        assertNull(result.get(0).getUrl());
        verifyNoInteractions(minioClient);
    }

    @Test
    void resolveAttachments_nonManagedUrl_attachmentUnchanged() throws Exception {
        URL externalUrl = new URL("https://example.com/file.pdf");
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setUrl(externalUrl);

        List<AttachmentRefOrValue> result = service.resolveAttachments(List.of(attachment)).block();

        assertNotNull(result);
        assertEquals(externalUrl, result.get(0).getUrl());
        verifyNoInteractions(minioClient);
    }

    @Test
    void resolveAttachments_managedUrl_replacesWithPresignedUrl() throws Exception {
        URL managedUrl = new URL(ENDPOINT + "/" + BUCKET + "/entity-1/uuid-file.txt");
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setUrl(managedUrl);

        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn(PRESIGNED_URL);

        List<AttachmentRefOrValue> result = service.resolveAttachments(List.of(attachment)).block();

        assertNotNull(result);
        assertEquals(new URL(PRESIGNED_URL), result.get(0).getUrl());
        verify(minioClient).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
    }

    // --- deleteAttachments ---

    @Test
    void deleteAttachments_nullList_noS3Calls() {
        service.deleteAttachments(null).block();
        verifyNoInteractions(minioClient);
    }

    @Test
    void deleteAttachments_nonManagedUrl_noS3Calls() throws Exception {
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setUrl(new URL("https://example.com/file.pdf"));

        service.deleteAttachments(List.of(attachment)).block();

        verifyNoInteractions(minioClient);
    }

    @Test
    void deleteAttachments_managedUrl_deletesFromS3() throws Exception {
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setUrl(new URL(ENDPOINT + "/" + BUCKET + "/entity-1/uuid-file.txt"));

        service.deleteAttachments(List.of(attachment)).block();

        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    void deleteAttachments_s3DeleteFails_doesNotThrow() throws Exception {
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setUrl(new URL(ENDPOINT + "/" + BUCKET + "/entity-1/uuid-file.txt"));

        doThrow(new RuntimeException("S3 unavailable")).when(minioClient).removeObject(any(RemoveObjectArgs.class));

        assertDoesNotThrow(() -> service.deleteAttachments(List.of(attachment)).block());
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

        assertNotNull(result);
        AttachmentRefOrValue copy = result.get(0);

        assertNotSame(validFor, copy.getValidFor(), "validFor should be a new instance, not the same reference");
        assertEquals(validFor.getStartDateTime(), copy.getValidFor().getStartDateTime());
        assertEquals(validFor.getEndDateTime(), copy.getValidFor().getEndDateTime());

        assertNotSame(size, copy.getSize(), "size should be a new instance, not the same reference");
        assertEquals(size.getAmount(), copy.getSize().getAmount());
        assertEquals(size.getUnits(), copy.getSize().getUnits());
    }
}
