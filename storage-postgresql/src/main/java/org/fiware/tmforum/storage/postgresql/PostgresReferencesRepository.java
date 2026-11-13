package org.fiware.tmforum.storage.postgresql;

import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import io.r2dbc.spi.ConnectionFactory;
import jakarta.inject.Singleton;
import org.fiware.tmforum.storage.ReferenceReadRepository;
import org.fiware.tmforum.storage.StorageOperation;
import org.fiware.tmforum.storage.StoredReference;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

import static org.fiware.tmforum.storage.postgresql.PostgresStorage.QUALIFIED_ENTITY_TABLE;

/**
 * PostgreSQL adapter of the reference port; every lookup, single or batched, is one query on the current rows.
 */
@Singleton
@Primary
@Requires(env = PostgresStorage.ENVIRONMENT)
public class PostgresReferencesRepository implements ReferenceReadRepository {

    private static final String LOOKUP_FAILED = "Was not able to look up the referenced entities %s.";

    private static final String SELECT_REFERENCES =
            "SELECT id, type FROM " + QUALIFIED_ENTITY_TABLE + " WHERE id = ANY($1) AND is_current";
    private static final String ID_COLUMN = "id";
    private static final String TYPE_COLUMN = "type";

    private final ConnectionFactory connectionFactory;

    public PostgresReferencesRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Mono<StoredReference> referenceExists(String id, List<String> acceptedTypes) {
        return findReference(id).filter(reference -> acceptedTypes.contains(reference.type()));
    }

    @Override
    public Mono<StoredReference> findReference(String id) {
        return findReferences(List.of(id)).next();
    }

    @Override
    public Flux<StoredReference> findReferences(Collection<String> ids) {
        if (ids.isEmpty()) {
            return Flux.empty();
        }
        // the current-row index makes the ids unique, so duplicates in the input need no DISTINCT
        String[] idArray = ids.toArray(String[]::new);
        return PostgresStatements.execute(connectionFactory, SELECT_REFERENCES,
                        statement -> statement.bind(0, idArray),
                        result -> result.map(row -> new StoredReference(row.get(ID_COLUMN, String.class),
                                row.get(TYPE_COLUMN, String.class))))
                .onErrorMap(failure -> PostgresFailures.failure(StorageOperation.READ,
                        String.format(LOOKUP_FAILED, ids), failure));
    }
}
