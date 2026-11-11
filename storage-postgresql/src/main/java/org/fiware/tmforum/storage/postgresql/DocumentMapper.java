package org.fiware.tmforum.storage.postgresql;

import io.github.wistefan.mapping.UnmappedProperty;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.storage.StorageErrorReason;
import org.fiware.tmforum.storage.StorageException;
import org.fiware.tmforum.storage.StorageOperation;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Converts domain objects to jsonb documents and back with one derived mapper of {@link PostgresDocumentMapper}.
 */
public final class DocumentMapper {

    static final String ID_PROPERTY = DocumentAnnotationIntrospector.ID_PROPERTY;
    static final String TYPE_PROPERTY = PostgresMappingModule.TYPE_PROPERTY;

    private final ObjectMapper mapper;
    private final PostgresMappingModule documentModule;
    private final boolean dropEmptyAttributes;

    /**
     * {@code documentModule} is the module registered on {@code mapper}.
     */
    DocumentMapper(ObjectMapper mapper, PostgresMappingModule documentModule, boolean dropEmptyAttributes) {
        this.mapper = mapper;
        this.documentModule = documentModule;
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
            removeEmptyAttributes(objectNode, extensionNames(entity), mode);
        }
        NullCharacterValidator.validate(objectNode, operation);
        return mapper.writeValueAsString(objectNode);
    }

    // the NGSI-LD adapter creates no attribute for an empty declared list or object, ignores an empty object on
    // update and deletes the attribute of an emptied list; it keeps empty extension values, so those stay as they are
    private static void removeEmptyAttributes(ObjectNode document, Set<String> extensionNames, DocumentMode mode) {
        List<Map.Entry<String, JsonNode>> emptyAttributes = document.properties().stream()
                .filter(attribute -> !extensionNames.contains(attribute.getKey()))
                .filter(attribute -> (attribute.getValue().isObject() || attribute.getValue().isArray())
                        && attribute.getValue().isEmpty())
                .toList();
        emptyAttributes.forEach(attribute -> {
            if (mode == DocumentMode.CARRIED_ATTRIBUTES && attribute.getValue().isArray()) {
                // a stored document never holds null, so the merge reads it as "remove this key"
                document.putNull(attribute.getKey());
            } else {
                document.remove(attribute.getKey());
            }
        });
    }

    // only the entries the serializer wrote: under a declared name the declared value is in the document
    private Set<String> extensionNames(Object entity) {
        if (!(entity instanceof Entity domainEntity) || domainEntity.getAdditionalProperties() == null) {
            return Set.of();
        }
        Set<String> declaredNames = documentModule.declaredNames(entity.getClass());
        return domainEntity.getAdditionalProperties().stream()
                .map(UnmappedProperty::getName)
                .filter(name -> !declaredNames.contains(name))
                .collect(Collectors.toSet());
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
