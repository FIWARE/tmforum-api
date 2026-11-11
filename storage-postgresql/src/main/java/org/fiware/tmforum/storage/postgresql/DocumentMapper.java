package org.fiware.tmforum.storage.postgresql;

import org.fiware.tmforum.storage.StorageErrorReason;
import org.fiware.tmforum.storage.StorageException;
import org.fiware.tmforum.storage.StorageOperation;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;

/**
 * Converts domain objects to jsonb documents and back with one derived mapper of {@link PostgresDocumentMapper}.
 */
public final class DocumentMapper {

    static final String ID_PROPERTY = DocumentAnnotationIntrospector.ID_PROPERTY;
    static final String TYPE_PROPERTY = PostgresMappingModule.TYPE_PROPERTY;

    private final ObjectMapper mapper;
    private final boolean dropEmptyAttributes;

    DocumentMapper(ObjectMapper mapper, boolean dropEmptyAttributes) {
        this.mapper = mapper;
        this.dropEmptyAttributes = dropEmptyAttributes;
    }

    /**
     * The document text to bind as jsonb. Throws {@link StorageException} with {@code operation}: INVALID_DATA for
     * a {@code \u0000} in a key or string, UNKNOWN if the object cannot be serialized.
     */
    public String toDocument(Object entity, DocumentMode mode, StorageOperation operation) {
        JsonNode document;
        try {
            document = mapper.valueToTree(entity);
        } catch (JacksonException e) {
            throw new StorageException(operation, StorageErrorReason.UNKNOWN,
                    String.format("Was not able to serialize %s.", entity.getClass().getSimpleName()), e);
        }
        if (!(document instanceof ObjectNode objectNode)) {
            throw new StorageException(operation, StorageErrorReason.UNKNOWN,
                    String.format("%s does not serialize to a JSON object.", entity.getClass().getSimpleName()));
        }
        if (mode == DocumentMode.CARRIED_ATTRIBUTES) {
            objectNode.remove(ID_PROPERTY);
            objectNode.remove(TYPE_PROPERTY);
        }
        if (dropEmptyAttributes) {
            // an update keeps an emptied list, the merge removes the stored one
            removeEmptyAttributes(objectNode, mode == DocumentMode.FULL);
        }
        NullCharacterValidator.validate(objectNode, operation);
        return mapper.writeValueAsString(objectNode);
    }

    // the NGSI-LD adapter creates no attribute for an empty list or an object without values, so neither reads back
    private static void removeEmptyAttributes(ObjectNode document, boolean includeLists) {
        List<String> emptyAttributes = document.properties().stream()
                .filter(attribute -> (attribute.getValue().isObject()
                        || (includeLists && attribute.getValue().isArray()))
                        && attribute.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .toList();
        document.remove(emptyAttributes);
    }

    /**
     * Throws {@link StorageException} (operation, UNKNOWN) if the document does not map to {@code entityClass}.
     */
    public <T> T fromDocument(String document, Class<T> entityClass, StorageOperation operation) {
        try {
            return mapper.readValue(document, entityClass);
        } catch (JacksonException e) {
            throw new StorageException(operation, StorageErrorReason.UNKNOWN,
                    String.format("Was not able to read a stored %s.", entityClass.getSimpleName()), e);
        }
    }
}
