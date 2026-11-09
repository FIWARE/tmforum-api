package org.fiware.tmforum.storage.postgresql;

import org.fiware.tmforum.storage.StorageErrorReason;
import org.fiware.tmforum.storage.StorageException;
import org.fiware.tmforum.storage.StorageOperation;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Optional;

/**
 * Rejects documents PostgreSQL cannot store as jsonb because a key or string value contains {@code \u0000}, naming
 * the first such field instead of the database's generic error.
 */
public final class NullCharacterValidator {

    static final char NUL = '\u0000';
    static final String NUL_ESCAPED = "\\u0000";
    static final String MESSAGE_FORMAT =
            "Field '%s' contains the NUL character (" + NUL_ESCAPED + "), which cannot be stored.";

    private static final String PROPERTY_SEPARATOR = ".";
    private static final String INDEX_FORMAT = "%s[%d]";

    private NullCharacterValidator() {
    }

    /**
     * @throws StorageException with INVALID_DATA and {@code operation}, the message naming the field's path
     */
    public static void validate(JsonNode document, StorageOperation operation) {
        findNul(document, "").ifPresent(path -> {
            throw new StorageException(operation, StorageErrorReason.INVALID_DATA, String.format(MESSAGE_FORMAT, path));
        });
    }

    private static Optional<String> findNul(JsonNode node, String path) {
        if (node.isString()) {
            return node.stringValue().indexOf(NUL) >= 0 ? Optional.of(path) : Optional.empty();
        }
        if (node.isArray()) {
            int index = 0;
            for (JsonNode element : node.values()) {
                Optional<String> found = findNul(element, String.format(INDEX_FORMAT, path, index++));
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        if (node.isObject()) {
            for (Map.Entry<String, JsonNode> property : node.properties()) {
                String key = property.getKey();
                String propertyPath = path.isEmpty() ? escape(key) : path + PROPERTY_SEPARATOR + escape(key);
                if (key.indexOf(NUL) >= 0) {
                    return Optional.of(propertyPath);
                }
                Optional<String> found = findNul(property.getValue(), propertyPath);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    // the path ends up in an HTTP response, so the character itself is spelled out
    private static String escape(String key) {
        return key.replace(String.valueOf(NUL), NUL_ESCAPED);
    }
}
