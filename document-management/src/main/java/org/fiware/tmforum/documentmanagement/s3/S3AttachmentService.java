package org.fiware.tmforum.documentmanagement.s3;

import io.micronaut.context.annotation.Requires;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.domain.AttachmentRefOrValue;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
@Requires(property = "s3.enabled", value = "true")
@Slf4j
public class S3AttachmentService {

    private MinioClient minioClient;
    private final S3Configuration config;

    public S3AttachmentService(S3Configuration config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing S3AttachmentService:");
        log.info("  endpoint: {}", config.getEndpoint());
        log.info("  accessKey: {}", config.getAccessKey());
        log.info("  bucket: {}", config.getBucket());
        log.info("  maxContentSize: {}", config.getMaxContentSize());

        try {
            MinioClient.Builder builder = MinioClient.builder()
                    .endpoint(config.getEndpoint())
                    .credentials(config.getAccessKey(), config.getSecretKey());
            if (config.getRegion() != null && !config.getRegion().isBlank()) {
                builder.region(config.getRegion());
            }
            this.minioClient = builder.build();
            log.info("S3 client created successfully");
        } catch (Exception e) {
            log.error("Failed to create S3 client: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize S3 client", e);
        }
        ensureBucketExists();
    }

    private void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(config.getBucket()).build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(config.getBucket()).build());
                log.info("Created S3 bucket: {}", config.getBucket());
            }
        } catch (Exception e) {
            log.warn("Could not ensure bucket exists: {}. Will retry on first upload.", e.getMessage());
        }
    }

    public void validateAttachmentContent(AttachmentRefOrValue attachment) {
        String content = attachment.getContent();
        if (content == null || content.isEmpty() || S3RetrievalInfo.isS3RetrievalInfo(content)) {
            return;
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(content);
        } catch (IllegalArgumentException e) {
            throw new TmForumException("Attachment content is not valid base64.", TmForumExceptionReason.INVALID_DATA);
        }
        if (decoded.length > config.getMaxContentSize()) {
            throw new TmForumException(
                    String.format("Attachment content exceeds maximum size of %d bytes (got %d bytes)",
                            config.getMaxContentSize(), decoded.length),
                    TmForumExceptionReason.INVALID_DATA);
        }
    }

    public Mono<List<AttachmentRefOrValue>> offloadAttachments(List<AttachmentRefOrValue> attachments, String entityId) {
        if (attachments == null || attachments.isEmpty()) {
            return Mono.justOrEmpty(attachments);
        }

        return Mono.fromCallable(() -> attachments.stream()
                        .map(att -> processAttachmentForOffload(att, entityId))
                        .collect(Collectors.toList()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<List<AttachmentRefOrValue>> hydrateAttachments(List<AttachmentRefOrValue> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return Mono.justOrEmpty(attachments);
        }

        return Mono.fromCallable(() -> attachments.stream()
                        .map(this::hydrateAttachment)
                        .collect(Collectors.toList()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> deleteAttachments(List<AttachmentRefOrValue> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return Mono.empty();
        }

        return Mono.fromRunnable(() -> attachments.stream()
                        .map(AttachmentRefOrValue::getContent)
                        .filter(S3RetrievalInfo::isS3RetrievalInfo)
                        .map(S3RetrievalInfo::fromBase64)
                        .forEach(info -> {
                            try {
                                deleteFromS3(info.getKey());
                            } catch (Exception e) {
                                log.warn("Failed to delete S3 object {}: {}", info.getKey(), e.getMessage());
                            }
                        }))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private AttachmentRefOrValue processAttachmentForOffload(AttachmentRefOrValue attachment, String entityId) {
        String content = attachment.getContent();
        if (content == null || content.isEmpty()) {
            return attachment;
        }

        // Skip if already S3 reference
        if (S3RetrievalInfo.isS3RetrievalInfo(content)) {
            return attachment;
        }

        // Decode base64 content
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(content);
        } catch (IllegalArgumentException e) {
            log.warn("Content is not valid base64, storing as-is");
            return attachment;
        }

        // Check size limit
        if (decoded.length > config.getMaxContentSize()) {
            throw new TmForumException(
                    String.format("Attachment content exceeds maximum size of %d bytes (got %d bytes)",
                            config.getMaxContentSize(), decoded.length),
                    TmForumExceptionReason.INVALID_DATA);
        }

        // Generate key
        String key = generateKey(entityId, attachment);

        // Upload to S3
        uploadToS3(key, decoded, attachment.getMimeType());

        // Create retrieval info
        S3RetrievalInfo info = new S3RetrievalInfo(
                config.getBucket(),
                key,
                decoded.length,
                attachment.getMimeType(),
                attachment.getName()
        );

        // Return modified attachment with retrieval info
        AttachmentRefOrValue modified = copyAttachment(attachment);
        modified.setContent(info.toBase64());
        return modified;
    }

    private AttachmentRefOrValue hydrateAttachment(AttachmentRefOrValue attachment) {
        String content = attachment.getContent();
        if (content == null || !S3RetrievalInfo.isS3RetrievalInfo(content)) {
            return attachment;
        }

        try {
            S3RetrievalInfo info = S3RetrievalInfo.fromBase64(content);
            byte[] fileContent = downloadFromS3(info.getKey());

            AttachmentRefOrValue hydrated = copyAttachment(attachment);
            hydrated.setContent(Base64.getEncoder().encodeToString(fileContent));
            return hydrated;
        } catch (Exception e) {
            log.error("Failed to hydrate attachment from S3: {}", e.getMessage());
            return attachment;
        }
    }

    private String generateKey(String entityId, AttachmentRefOrValue attachment) {
        String name = attachment.getName();
        if (name == null || name.isEmpty()) {
            name = "attachment";
        }
        // Clean the name for S3 key
        name = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        return String.format("%s/%s-%s", entityId, UUID.randomUUID().toString(), name);
    }

    private void uploadToS3(String key, byte[] content, String mimeType) {
        try {
            ensureBucketExists();

            String contentType = mimeType != null ? mimeType : "application/octet-stream";

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(config.getBucket())
                            .object(key)
                            .stream(new ByteArrayInputStream(content), content.length, -1)
                            .contentType(contentType)
                            .build()
            );
            log.debug("Uploaded to S3: {}/{}", config.getBucket(), key);
        } catch (Exception e) {
            throw new TmForumException(
                    "Failed to upload attachment to S3: " + e.getMessage(),
                    TmForumExceptionReason.INVALID_DATA);
        }
    }

    private byte[] downloadFromS3(String key) {
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(config.getBucket())
                        .object(key)
                        .build())) {
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new TmForumException(
                    "Failed to download attachment from S3: " + e.getMessage(),
                    TmForumExceptionReason.NOT_FOUND);
        }
    }

    private void deleteFromS3(String key) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(config.getBucket())
                            .object(key)
                            .build()
            );
            log.debug("Deleted from S3: {}/{}", config.getBucket(), key);
        } catch (Exception e) {
            log.warn("Failed to delete S3 object {}: {}", key, e.getMessage());
        }
    }

    private AttachmentRefOrValue copyAttachment(AttachmentRefOrValue source) {
        AttachmentRefOrValue copy = new AttachmentRefOrValue();
        copy.setTmfId(source.getTmfId());
        copy.setHref(source.getHref());
        copy.setAttachmentType(source.getAttachmentType());
        copy.setContent(source.getContent());
        copy.setDescription(source.getDescription());
        copy.setMimeType(source.getMimeType());
        copy.setUrl(source.getUrl());
        copy.setSize(source.getSize());
        copy.setValidFor(source.getValidFor());
        copy.setName(source.getName());
        copy.setAtReferredType(source.getAtReferredType());
        return copy;
    }
}
