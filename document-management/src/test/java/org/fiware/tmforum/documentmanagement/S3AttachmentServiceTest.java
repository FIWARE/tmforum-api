package org.fiware.tmforum.documentmanagement;

import org.fiware.tmforum.common.domain.AttachmentRefOrValue;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.documentmanagement.s3.S3AttachmentService;
import org.fiware.tmforum.documentmanagement.s3.S3Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class S3AttachmentServiceTest {

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

    private S3Configuration config;
    private S3AttachmentService service;

    @BeforeEach
    void setUp() {
        config = new S3Configuration();
        config.setEndpoint("http://localhost:9000");
        config.setAccessKey("minioadmin");
        config.setSecretKey("minioadmin");
        config.setBucket("test-bucket");
        config.setMaxContentSize(1024 * 1024); // 1 MB for tests

        service = new TestableS3AttachmentService(config);
    }

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
        String content = Base64.getEncoder().encodeToString("Hello World".getBytes());
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setContent(content);
        assertDoesNotThrow(() -> service.validateAttachmentContent(attachment));
    }

    @Test
    void validateAttachmentContent_invalidBase64_throwsTmForumException() {
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setContent("this is not valid base64!!!");
        assertThrows(TmForumException.class, () -> service.validateAttachmentContent(attachment));
    }

    @Test
    void validateAttachmentContent_contentExceedsMaxSize_throwsTmForumException() {
        config.setMaxContentSize(4); // 4 bytes max
        byte[] oversized = new byte[10];
        String content = Base64.getEncoder().encodeToString(oversized);
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setContent(content);
        assertThrows(TmForumException.class, () -> service.validateAttachmentContent(attachment));
    }

    @Test
    void testS3ConfigurationDefaults() {
        S3Configuration defaultConfig = new S3Configuration();
        assertFalse(defaultConfig.isEnabled());
        assertEquals("http://localhost:9000", defaultConfig.getEndpoint());
        assertEquals("minioadmin", defaultConfig.getAccessKey());
        assertEquals("minioadmin", defaultConfig.getSecretKey());
        assertEquals("document-attachments", defaultConfig.getBucket());
        assertEquals(10 * 1024 * 1024, defaultConfig.getMaxContentSize());
    }
}
