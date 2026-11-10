package org.fiware.tmforum.storage.postgresql;

import org.fiware.tmforum.storage.StorageErrorReason;
import org.fiware.tmforum.storage.StorageException;
import org.fiware.tmforum.storage.StorageOperation;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Converts domain objects to jsonb documents and back with one derived mapper of {@link PostgresDocumentMapper}.
 */
public final class DocumentMapper {

    static final String ID_PROPERTY = DocumentAnnotationIntrospector.ID_PROPERTY;
    static final String TYPE_PROPERTY = PostgresMappingModule.TYPE_PROPERTY;

    private final ObjectMapper mapper;

    DocumentMapper(ObjectMapper mapper) {
        this.mapper = mapper;
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
        NullCharacterValidator.validate(objectNode, operation);
        return mapper.writeValueAsString(objectNode);
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
