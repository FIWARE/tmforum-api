package org.fiware.tmforum.storage.postgresql;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import tools.jackson.databind.ObjectMapper;

/**
 * Document mappers of the PostgreSQL adapter, derived from the application mapper so they keep its modules and
 * configuration, plus {@link PostgresMappingModule}. Both variants write one format and read each other's documents.
 */
@Singleton
@Requires(env = PostgresStorage.ENVIRONMENT)
public class PostgresDocumentMapper {

    // null and absent are the same, an empty list or string is a value
    private static final JsonInclude.Value DOCUMENT_INCLUSION =
            JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.USE_DEFAULTS);

    private final DocumentMapper storage;
    private final DocumentMapper event;

    public PostgresDocumentMapper(ObjectMapper applicationMapper) {
        this.storage = new DocumentMapper(derive(applicationMapper, true));
        this.event = new DocumentMapper(derive(applicationMapper, false));
    }

    /**
     * For {@code tmforum.entity.data}: drops {@link org.fiware.tmforum.storage.NotStored} fields.
     */
    public DocumentMapper storage() {
        return storage;
    }

    /**
     * For outbox states, which keep {@link org.fiware.tmforum.storage.NotStored} fields for the event payload.
     */
    public DocumentMapper event() {
        return event;
    }

    private static ObjectMapper derive(ObjectMapper applicationMapper, boolean dropNotStored) {
        return applicationMapper.rebuild()
                .addModule(new PostgresMappingModule(dropNotStored))
                .changeDefaultPropertyInclusion(inclusion -> DOCUMENT_INCLUSION)
                .build();
    }
}
