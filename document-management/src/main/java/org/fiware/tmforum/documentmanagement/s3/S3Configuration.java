package org.fiware.tmforum.documentmanagement.s3;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties for the S3-compatible object storage backend, bound from the
 * {@code s3.*} namespace in {@code application.yaml}.
 *
 * <p>S3 support is opt-in: set {@code s3.enabled=true} together with the connection properties
 * to activate the {@link S3AttachmentService}. When disabled, the DocumentManagement API
 * still operates normally but rejects attachments with inline content.
 *
 * <p>Example configuration for a local MinIO instance:
 * <pre>
 * s3:
 *   enabled: true
 *   endpoint: http://minio:9000
 *   access-key: minioadmin
 *   secret-key: minioadmin
 *   bucket: document-attachments
 * </pre>
 */
@ConfigurationProperties("s3")
@Getter
@Setter
public class S3Configuration {

    /**
     * Whether the S3 storage backend is enabled. When {@code false} (the default) the
     * {@link S3AttachmentService} bean is not instantiated and attachments with inline
     * content are rejected by the API.
     */
    private boolean enabled = false;

    /**
     * URL of the S3-compatible endpoint, e.g. {@code http://minio:9000} for MinIO or
     * {@code https://s3.amazonaws.com} for AWS S3. Defaults to {@code http://localhost:9000}.
     */
    private String endpoint = "http://localhost:9000";

    /**
     * Access key (username) used to authenticate with the S3 endpoint.
     */
    private String accessKey = "minioadmin";

    /**
     * Secret key (password) used to authenticate with the S3 endpoint.
     */
    private String secretKey = "minioadmin";

    /**
     * Name of the S3 bucket where attachment objects are stored. The bucket is created
     * automatically on startup if it does not already exist.
     */
    private String bucket = "document-attachments";

    /**
     * Maximum allowed size in bytes for a single attachment's inline content. Requests
     * that exceed this limit are rejected with a 422. Defaults to 10 MB.
     */
    private long maxContentSize = 10 * 1024 * 1024;

    /**
     * S3 region identifier (e.g. {@code us-east-1} for AWS, {@code eu-central-3} for IONOS).
     * Required for AWS S3 and IONOS Object Storage. Leave unset for local MinIO instances.
     */
    private String region;
}
