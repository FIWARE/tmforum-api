package org.fiware.tmforum.documentmanagement.s3;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class S3RetrievalInfo {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String MARKER = "s3ref:";

    private String bucket;
    private String key;
    private long size;
    private String mimeType;
    private String originalName;

    public String toBase64() {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(this);
            return MARKER + Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize S3RetrievalInfo", e);
        }
    }

    public static S3RetrievalInfo fromBase64(String encoded) {
        try {
            if (encoded.startsWith(MARKER)) {
                encoded = encoded.substring(MARKER.length());
            }
            String json = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            return OBJECT_MAPPER.readValue(json, S3RetrievalInfo.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize S3RetrievalInfo", e);
        }
    }

    public static boolean isS3RetrievalInfo(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }

        // Check for our marker prefix
        if (content.startsWith(MARKER)) {
            return true;
        }

        // Try to decode and check for expected fields
        try {
            String decoded;
            if (content.startsWith(MARKER)) {
                decoded = new String(Base64.getDecoder().decode(content.substring(MARKER.length())), StandardCharsets.UTF_8);
            } else {
                decoded = new String(Base64.getDecoder().decode(content), StandardCharsets.UTF_8);
            }
            return decoded.contains("\"bucket\"") && decoded.contains("\"key\"");
        } catch (Exception e) {
            return false;
        }
    }
}
