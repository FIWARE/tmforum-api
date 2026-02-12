package org.fiware.tmforum.documentmanagement.s3;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties("s3")
@Getter
@Setter
public class S3Configuration {

    private String endpoint = "http://localhost:9000";
    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin";
    private String bucket = "document-attachments";
    private long maxContentSize = 10 * 1024 * 1024; // 10MB default
}
