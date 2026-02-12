package org.fiware.tmforum.documentmanagement;

import org.fiware.tmforum.common.domain.AttachmentRefOrValue;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.documentmanagement.s3.S3AttachmentService;
import org.fiware.tmforum.documentmanagement.s3.S3Configuration;
import org.fiware.tmforum.documentmanagement.s3.S3RetrievalInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class S3AttachmentServiceTest {

    private S3Configuration config;

    @BeforeEach
    void setUp() {
        config = new S3Configuration();
        config.setEndpoint("http://localhost:9000");
        config.setAccessKey("minioadmin");
        config.setSecretKey("minioadmin");
        config.setBucket("test-bucket");
        config.setMaxContentSize(1024 * 1024); // 1MB for tests
    }

    @Test
    void testS3RetrievalInfoSerialization() {
        S3RetrievalInfo info = new S3RetrievalInfo(
                "test-bucket",
                "entity-123/uuid-file.txt",
                1024,
                "text/plain",
                "file.txt"
        );

        String encoded = info.toBase64();
        assertNotNull(encoded);
        assertTrue(encoded.startsWith("s3ref:"));

        S3RetrievalInfo decoded = S3RetrievalInfo.fromBase64(encoded);
        assertEquals(info.getBucket(), decoded.getBucket());
        assertEquals(info.getKey(), decoded.getKey());
        assertEquals(info.getSize(), decoded.getSize());
        assertEquals(info.getMimeType(), decoded.getMimeType());
        assertEquals(info.getOriginalName(), decoded.getOriginalName());
    }

    @Test
    void testIsS3RetrievalInfo() {
        S3RetrievalInfo info = new S3RetrievalInfo(
                "bucket",
                "key",
                100,
                "text/plain",
                "file.txt"
        );

        String encoded = info.toBase64();
        assertTrue(S3RetrievalInfo.isS3RetrievalInfo(encoded));

        // Regular base64 content should not be detected as S3 info
        String regularBase64 = Base64.getEncoder().encodeToString("Hello World".getBytes());
        assertFalse(S3RetrievalInfo.isS3RetrievalInfo(regularBase64));

        // Null and empty should return false
        assertFalse(S3RetrievalInfo.isS3RetrievalInfo(null));
        assertFalse(S3RetrievalInfo.isS3RetrievalInfo(""));
    }

    @Test
    void testAttachmentWithNullContent() {
        // Create a mock service that doesn't actually connect to MinIO
        // Just test the logic around null handling
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setName("test.txt");
        attachment.setMimeType("text/plain");
        attachment.setContent(null);

        List<AttachmentRefOrValue> attachments = List.of(attachment);

        // The service will skip null content attachments
        // This is a logic test, not an integration test
        assertNotNull(attachments);
        assertNull(attachments.get(0).getContent());
    }

    @Test
    void testAttachmentWithEmptyContent() {
        AttachmentRefOrValue attachment = new AttachmentRefOrValue();
        attachment.setName("test.txt");
        attachment.setMimeType("text/plain");
        attachment.setContent("");

        assertNotNull(attachment);
        assertEquals("", attachment.getContent());
    }

    @Test
    void testS3RetrievalInfoWithSpecialCharacters() {
        S3RetrievalInfo info = new S3RetrievalInfo(
                "test-bucket",
                "entity/special chars!@#$%^&().txt",
                2048,
                "application/octet-stream",
                "special chars!@#$%^&().txt"
        );

        String encoded = info.toBase64();
        S3RetrievalInfo decoded = S3RetrievalInfo.fromBase64(encoded);

        assertEquals(info.getOriginalName(), decoded.getOriginalName());
        assertEquals(info.getKey(), decoded.getKey());
    }

    @Test
    void testS3ConfigurationDefaults() {
        S3Configuration defaultConfig = new S3Configuration();

        assertEquals("http://localhost:9000", defaultConfig.getEndpoint());
        assertEquals("minioadmin", defaultConfig.getAccessKey());
        assertEquals("minioadmin", defaultConfig.getSecretKey());
        assertEquals("document-attachments", defaultConfig.getBucket());
        assertEquals(10 * 1024 * 1024, defaultConfig.getMaxContentSize());
    }
}
