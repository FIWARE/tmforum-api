package org.fiware.tmforum.documentmanagement.s3;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties("s3")
@Getter
@Setter
public class S3Configuration {

    private boolean enabled = false;
    private String endpoint = "http://localhost:9000";
    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin";
    private String bucket = "document-attachments";
    private long maxContentSize = 10 * 1024 * 1024; // 10MB default
    // Required for AWS S3 (e.g. "us-east-1"). Leave unset for MinIO, IONOS, or other providers.
    private String region;
}
