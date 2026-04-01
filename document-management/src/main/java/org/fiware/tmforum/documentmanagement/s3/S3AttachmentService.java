package org.fiware.tmforum.documentmanagement.s3;

import io.micronaut.context.annotation.Requires;
import org.fiware.tmforum.documentmanagement.AttachmentService;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.domain.AttachmentRefOrValue;
import org.fiware.tmforum.common.domain.Quantity;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * {@link AttachmentService} implementation backed by an S3-compatible object store (e.g. MinIO,
 * AWS S3, IONOS Object Storage).
 *
 * <p>Inline base64 attachment content is decoded, validated against the configured size limit,
 * and uploaded to the configured bucket. The {@code content} field is cleared and the {@code url}
 * field is set to an internal S3 path ({@code endpoint/bucket/key}). On retrieval the internal path
 * is resolved to a pre-signed download URL so the raw bytes are never persisted in the context broker.
 *
 * <p>This bean is only instantiated when {@code s3.enabled=true} is set. When it is absent the
 * API falls back to accepting pure URL/href references only.
 */
@Singleton
@Requires(property = "s3.enabled", value = "true")
@Slf4j
public class S3AttachmentService implements AttachmentService {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final int PRESIGNED_URL_EXPIRY_SECONDS = 3600;

    private MinioClient minioClient;
    private final S3Configuration config;

    public S3AttachmentService(S3Configuration config) {
        this.config = config;
    }

    /**
     * Initialises the MinIO client from configuration and ensures the target bucket exists.
     * Called automatically by the Micronaut container after dependency injection.
     *
     * @throws RuntimeException if the MinIO client cannot be created
     */
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

    /** {@inheritDoc} */
    @Override
    public void validateAttachmentContent(AttachmentRefOrValue attachment) {
        String content = attachment.getContent();
        if (content == null || content.isEmpty()) {
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

    /** {@inheritDoc} */
    @Override
    public Mono<List<AttachmentRefOrValue>> offloadAttachments(List<AttachmentRefOrValue> attachments, String entityId) {
        if (attachments == null || attachments.isEmpty()) {
            return Mono.justOrEmpty(attachments);
        }

        return Mono.fromCallable(() -> attachments.stream()
                        .map(att -> processAttachmentForOffload(att, entityId))
                        .collect(Collectors.toList()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /** {@inheritDoc} */
    @Override
    public Mono<List<AttachmentRefOrValue>> resolveAttachments(List<AttachmentRefOrValue> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return Mono.justOrEmpty(attachments);
        }

        return Mono.fromCallable(() -> attachments.stream()
                        .map(this::resolveAttachment)
                        .collect(Collectors.toList()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /** {@inheritDoc} */
    @Override
    public Mono<Void> deleteAttachments(List<AttachmentRefOrValue> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return Mono.empty();
        }

        return Mono.fromRunnable(() -> attachments.stream()
                        .map(AttachmentRefOrValue::getUrl)
                        .filter(this::isManagedUrl)
                        .map(this::extractKey)
                        .forEach(key -> {
                            try {
                                deleteFromS3(key);
                            } catch (Exception e) {
                                log.warn("Failed to delete S3 object {}", key, e);
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

        // Skip if this attachment already has a managed URL
        if (isManagedUrl(attachment.getUrl())) {
            return attachment;
        }

        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(content);
        } catch (IllegalArgumentException e) {
            log.warn("Content is not valid base64, skipping offload");
            return attachment;
        }

        String key = generateKey(entityId, attachment);
        uploadToS3(key, decoded, attachment.getMimeType());

        AttachmentRefOrValue modified = copyAttachment(attachment);
        modified.setContent(null);
        try {
            modified.setUrl(new URL(config.getEndpoint() + "/" + config.getBucket() + "/" + key));
        } catch (Exception e) {
            throw new TmForumException("Failed to construct S3 URL for attachment: " + e.getMessage(),
                    TmForumExceptionReason.INVALID_DATA);
        }
        return modified;
    }

    private AttachmentRefOrValue resolveAttachment(AttachmentRefOrValue attachment) {
        if (!isManagedUrl(attachment.getUrl())) {
            return attachment;
        }

        try {
            String key = extractKey(attachment.getUrl());
            String presignedUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(config.getBucket())
                            .object(key)
                            .expiry(PRESIGNED_URL_EXPIRY_SECONDS)
                            .build());
            AttachmentRefOrValue resolved = copyAttachment(attachment);
            resolved.setUrl(new URL(presignedUrl));
            return resolved;
        } catch (Exception e) {
            log.error("Failed to resolve attachment URL from S3: {}", e.getMessage());
            return attachment;
        }
    }

    private boolean isManagedUrl(URL url) {
        if (url == null) {
            return false;
        }
        String prefix = config.getEndpoint() + "/" + config.getBucket() + "/";
        return url.toString().startsWith(prefix);
    }

    private String extractKey(URL url) {
        String prefix = config.getEndpoint() + "/" + config.getBucket() + "/";
        return url.toString().substring(prefix.length());
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

            String contentType = mimeType != null ? mimeType : DEFAULT_CONTENT_TYPE;

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
            log.warn("Failed to delete S3 object {}", key, e);
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
        if (source.getSize() != null) {
            Quantity sizeCopy = new Quantity();
            sizeCopy.setAmount(source.getSize().getAmount());
            sizeCopy.setUnits(source.getSize().getUnits());
            copy.setSize(sizeCopy);
        }
        if (source.getValidFor() != null) {
            TimePeriod validForCopy = new TimePeriod();
            validForCopy.setStartDateTime(source.getValidFor().getStartDateTime());
            validForCopy.setEndDateTime(source.getValidFor().getEndDateTime());
            copy.setValidFor(validForCopy);
        }
        copy.setName(source.getName());
        copy.setAtReferredType(source.getAtReferredType());
        copy.setAtSchemaLocation(source.getAtSchemaLocation());
        return copy;
    }
}
