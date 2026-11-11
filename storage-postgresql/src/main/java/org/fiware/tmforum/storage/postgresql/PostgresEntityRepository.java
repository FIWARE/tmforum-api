package org.fiware.tmforum.storage.postgresql;

import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import io.r2dbc.postgresql.codec.Json;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Readable;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import jakarta.inject.Singleton;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.storage.EntityRepository;
import org.fiware.tmforum.storage.PagedResult;
import org.fiware.tmforum.storage.StorageErrorReason;
import org.fiware.tmforum.storage.StorageException;
import org.fiware.tmforum.storage.StorageOperation;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.fiware.tmforum.storage.postgresql.PostgresStorage.QUALIFIED_ENTITY_TABLE;
import static org.fiware.tmforum.storage.postgresql.PostgresStorage.QUALIFIED_OUTBOX_TABLE;

/**
 * PostgreSQL adapter of the entity port: one row per entity in {@code tmforum.entity}, the domain object as jsonb.
 * Lists support ids, types and paging only; a query or an order fails until ticket 7.
 */
@Singleton
@Primary
@Requires(env = PostgresStorage.ENVIRONMENT)
public class PostgresEntityRepository implements EntityRepository {

    static final String LIST_NOT_SUPPORTED =
            "Filtering or ordering a list is not supported by the PostgreSQL backend before ticket 7.";
    static final String TYPES_REQUIRED = "Listing entities requires at least one type.";
    private static final String LIST_FAILED = "Was not able to list entities.";
    private static final String LIST_SEPARATOR = ",";

    private static final String ID_COLUMN = "id";
    private static final String TYPE_COLUMN = "type";
    private static final String DATA_COLUMN = "data";
    private static final String TOTAL_COUNT_COLUMN = "total_count";

    private static final String SELECT_CURRENT =
            "SELECT data::text AS data FROM " + QUALIFIED_ENTITY_TABLE + " WHERE id = $1 AND is_current";
    // created_at and updated_at fall back to their column default (now()) when bound null
    private static final String INSERT = "INSERT INTO " + QUALIFIED_ENTITY_TABLE
            + " (id, type, data, created_at, updated_at)"
            + " VALUES ($1, $2, $3::jsonb, COALESCE($4::timestamptz, now()), COALESCE($5::timestamptz, now()))";
    // || replaces each carried top-level key, the patch never carries id or type; an emptied declared list is null in
    // the patch and removes its key, as the NGSI-LD adapter deletes the attribute
    private static final String MERGED_DATA = "(data || $2::jsonb)"
            + " - ARRAY(SELECT key FROM jsonb_each($2::jsonb) WHERE value = 'null'::jsonb)";
    private static final String UPDATE_CURRENT = "UPDATE " + QUALIFIED_ENTITY_TABLE
            + " SET data = " + MERGED_DATA + ", updated_at = now() WHERE id = $1 AND is_current RETURNING id";
    private static final String DELETE_CURRENT =
            "DELETE FROM " + QUALIFIED_ENTITY_TABLE + " WHERE id = $1 AND is_current";
    private static final String SELECT_CURRENT_FOR_UPDATE = "SELECT type, data::text AS data FROM "
            + QUALIFIED_ENTITY_TABLE + " WHERE id = $1 AND is_current FOR UPDATE";
    private static final String UPDATE_CURRENT_RETURNING_DATA = "UPDATE " + QUALIFIED_ENTITY_TABLE
            + " SET data = " + MERGED_DATA + ", updated_at = now() WHERE id = $1 AND is_current"
            + " RETURNING data::text AS data";
    private static final String DELETE_CURRENT_RETURNING = "DELETE FROM " + QUALIFIED_ENTITY_TABLE
            + " WHERE id = $1 AND is_current RETURNING type, data::text AS data";
    private static final String INSERT_OUTBOX_ROW = "INSERT INTO " + QUALIFIED_OUTBOX_TABLE
            + " (event_kind, entity_id, entity_type, new_state, old_state) VALUES ($1, $2, $3, $4::jsonb, $5::jsonb)";
    private static final String TYPE_PREDICATE = " WHERE is_current AND type = ANY($1)";
    private static final String ID_PREDICATE = " AND id = ANY($2)";
    private static final String LIST_COLUMNS = "SELECT type, data::text AS data, count(*) OVER () AS total_count FROM ";
    private static final String LIST_ORDER = " ORDER BY created_at, id";
    private static final String COUNT_COLUMNS = "SELECT count(*) AS total_count FROM ";
    private static final int TYPES_INDEX = 0;
    private static final int IDS_INDEX = 1;

    private final ConnectionFactory connectionFactory;
    private final DocumentMapper documents;
    private final DocumentMapper eventDocuments;

    public PostgresEntityRepository(ConnectionFactory connectionFactory, PostgresDocumentMapper documentMapper) {
        this.connectionFactory = connectionFactory;
        this.documents = documentMapper.storage();
        this.eventDocuments = documentMapper.event();
    }

    @Override
    public <T> Mono<T> get(URI id, Class<T> entityClass) {
        if (!IdHelper.hasTypeSegmentOf(id, entityClass)) {
            return Mono.empty();
        }
        return execute(SELECT_CURRENT, statement -> statement.bind(0, id.toString()),
                result -> result.map(row -> row.get(DATA_COLUMN, String.class)))
                .next()
                .map(document -> documents.fromDocument(document, entityClass, StorageOperation.READ))
                .onErrorMap(failure -> PostgresFailures.failure(StorageOperation.READ,
                        String.format("Was not able to read %s.", id), failure));
    }

    @Override
    public <T> Mono<PagedResult<T>> findEntities(Integer offset, Integer limit, Class<T> entityClass,
                                                 String query, String ids, String types, String orderBy) {
        return list(offset, limit, types, query, ids, orderBy, type -> entityClass);
    }

    @Override
    public <T> Mono<PagedResult<T>> findEntitiesPolymorphic(Integer offset, Integer limit, String types,
                                                            String query, String ids, String orderBy,
                                                            Function<String, Class<? extends T>> typeToClass) {
        return list(offset, limit, types, query, ids, orderBy, typeToClass);
    }

    @Override
    public <T> Mono<Void> createDomainEntity(T domainEntity) {
        return insert(domainEntity, null, null);
    }

    /**
     * Creates the entity with the given timestamps instead of now, for a migration that keeps the broker's creation
     * order. A null timestamp falls back to now. Fails like {@link #createDomainEntity}.
     */
    public <T> Mono<Void> importDomainEntity(T domainEntity, Instant createdAt, Instant updatedAt) {
        return insert(domainEntity, createdAt, updatedAt);
    }

    /**
     * One statement merging the carried attributes into the stored document, so concurrent updates never interleave
     * a read and a write.
     */
    @Override
    public <T> Mono<Void> updateDomainEntity(String id, T domainEntity) {
        return Mono.fromCallable(() -> documents.toDocument(domainEntity, DocumentMode.CARRIED_ATTRIBUTES,
                        StorageOperation.UPDATE))
                .flatMapMany(patch -> execute(UPDATE_CURRENT,
                        statement -> statement.bind(0, id).bind(1, Json.of(patch)),
                        result -> result.map(row -> row.get(ID_COLUMN, String.class))))
                .count()
                .onErrorMap(failure -> PostgresFailures.failure(StorageOperation.UPDATE,
                        String.format("Was not able to update %s.", id), failure))
                .flatMap(updated -> updated == 0
                        ? Mono.error(new StorageException(StorageOperation.UPDATE, StorageErrorReason.NOT_FOUND,
                        String.format("Was not able to update %s, since it does not exist.", id)))
                        : Mono.empty());
    }

    @Override
    public Mono<Void> deleteDomainEntity(URI id) {
        return execute(DELETE_CURRENT, statement -> statement.bind(0, id.toString()), Result::getRowsUpdated)
                .reduce(0L, Long::sum)
                .onErrorMap(failure -> PostgresFailures.failure(StorageOperation.DELETE,
                        String.format("Was not able to delete %s.", id), failure))
                .flatMap(deleted -> deleted == 0
                        ? Mono.error(new StorageException(StorageOperation.DELETE, StorageErrorReason.NOT_FOUND,
                        String.format("Was not able to delete %s, since it does not exist.", id)))
                        : Mono.empty());
    }

    /**
     * Creates the entity and, if {@code recordsEvents} accepts its type, its CREATE outbox row, in one transaction.
     * The row's new_state is the request object with its {@link org.fiware.tmforum.storage.NotStored} fields.
     */
    public Mono<Void> createWithEvent(Object domainEntity, Predicate<String> recordsEvents) {
        EntityWithId entity = withId(domainEntity);
        String id = entity.getId().toString();
        return Mono.fromCallable(() -> new CreatedDocuments(
                        documents.toDocument(entity, DocumentMode.FULL, StorageOperation.CREATE),
                        recordsEvents.test(entity.getType())
                                ? eventDocuments.toDocument(entity, DocumentMode.FULL, StorageOperation.CREATE)
                                : null))
                .flatMap(created -> PostgresStatements.inTransaction(connectionFactory, connection ->
                        PostgresStatements.execute(connection, INSERT,
                                        statement -> bindInsert(statement, entity, created.stored(), null, null),
                                        Result::getRowsUpdated)
                                .then(created.event() == null ? Mono.empty() : insertOutboxRow(connection,
                                        EventKind.CREATE, id, entity.getType(), created.event(), null))))
                .onErrorMap(failure -> PostgresFailures.failure(StorageOperation.CREATE,
                        String.format("Was not able to create %s.", id), failure));
    }

    /**
     * Merges the patch like {@link #updateDomainEntity} and, if {@code recordsEvents} accepts the stored type, inserts
     * the UPDATE outbox row with the locked old and the merged new document, in one transaction. Returns the new
     * state.
     */
    public <T> Mono<T> updateWithEvent(String id, T patch, Class<T> entityClass, Predicate<String> recordsEvents) {
        return Mono.fromCallable(() -> documents.toDocument(patch, DocumentMode.CARRIED_ATTRIBUTES,
                        StorageOperation.UPDATE))
                .flatMap(patchDocument -> PostgresStatements.inTransaction(connectionFactory, connection ->
                        PostgresStatements.execute(connection, SELECT_CURRENT_FOR_UPDATE,
                                        statement -> statement.bind(0, id),
                                        result -> result.map(StoredRow::of))
                                .next()
                                .switchIfEmpty(Mono.error(() -> notFound(StorageOperation.UPDATE, id)))
                                .flatMap(oldRow -> PostgresStatements.execute(connection,
                                                UPDATE_CURRENT_RETURNING_DATA,
                                                statement -> statement.bind(0, id).bind(1, Json.of(patchDocument)),
                                                result -> result.map(row -> row.get(DATA_COLUMN, String.class)))
                                        .next()
                                        .flatMap(newData -> (recordsEvents.test(oldRow.type())
                                                ? insertOutboxRow(connection, EventKind.UPDATE, id, oldRow.type(),
                                                newData, oldRow.data())
                                                : Mono.<Void>empty())
                                                .thenReturn(newData)))))
                .map(newData -> documents.fromDocument(newData, entityClass, StorageOperation.UPDATE))
                .onErrorMap(failure -> PostgresFailures.failure(StorageOperation.UPDATE,
                        String.format("Was not able to update %s.", id), failure));
    }

    /**
     * Deletes the entity and, if {@code recordsEvents} accepts its type, inserts the DELETE outbox row with the deleted
     * document, in one transaction. A missing entity fails with NOT_FOUND and leaves no row.
     */
    public Mono<Void> deleteWithEvent(URI id, Predicate<String> recordsEvents) {
        String entityId = id.toString();
        return PostgresStatements.inTransaction(connectionFactory, connection ->
                        PostgresStatements.execute(connection, DELETE_CURRENT_RETURNING,
                                        statement -> statement.bind(0, entityId),
                                        result -> result.map(StoredRow::of))
                                .next()
                                .switchIfEmpty(Mono.error(() -> notFound(StorageOperation.DELETE, entityId)))
                                .flatMap(deleted -> recordsEvents.test(deleted.type())
                                        ? insertOutboxRow(connection, EventKind.DELETE, entityId, deleted.type(),
                                        null, deleted.data())
                                        : Mono.empty()))
                .onErrorMap(failure -> PostgresFailures.failure(StorageOperation.DELETE,
                        String.format("Was not able to delete %s.", id), failure));
    }

    private static Mono<Void> insertOutboxRow(Connection connection, EventKind kind, String id, String type,
                                              String newState, String oldState) {
        return PostgresStatements.execute(connection, INSERT_OUTBOX_ROW, statement -> {
            statement.bind(0, kind.name()).bind(1, id).bind(2, type);
            bindNullable(statement, 3, newState == null ? null : Json.of(newState), Json.class);
            return bindNullable(statement, 4, oldState == null ? null : Json.of(oldState), Json.class);
        }, Result::getRowsUpdated).then();
    }

    private static StorageException notFound(StorageOperation operation, String id) {
        return new StorageException(operation, StorageErrorReason.NOT_FOUND,
                String.format("Was not able to %s %s, since it does not exist.",
                        operation.name().toLowerCase(Locale.ROOT), id));
    }

    private <T> Mono<Void> insert(T domainEntity, Instant createdAt, Instant updatedAt) {
        EntityWithId entity = withId(domainEntity);
        String id = entity.getId().toString();
        return Mono.fromCallable(() -> documents.toDocument(entity, DocumentMode.FULL, StorageOperation.CREATE))
                .flatMapMany(document -> execute(INSERT,
                        statement -> bindInsert(statement, entity, document, createdAt, updatedAt),
                        Result::getRowsUpdated))
                .then()
                .onErrorMap(failure -> PostgresFailures.failure(StorageOperation.CREATE,
                        String.format("Was not able to create %s.", id), failure));
    }

    private static EntityWithId withId(Object domainEntity) {
        if (!(domainEntity instanceof EntityWithId entity)) {
            throw new IllegalArgumentException(String.format("%s has no id and type to store it by.",
                    domainEntity.getClass().getName()));
        }
        return entity;
    }

    private static Statement bindInsert(Statement statement, EntityWithId entity, String document, Instant createdAt,
                                        Instant updatedAt) {
        statement.bind(0, entity.getId().toString()).bind(1, entity.getType()).bind(2, Json.of(document));
        bindNullable(statement, 3, createdAt, Instant.class);
        return bindNullable(statement, 4, updatedAt, Instant.class);
    }

    private <T> Mono<PagedResult<T>> list(Integer offset, Integer limit, String types, String query, String ids,
                                          String orderBy, Function<String, Class<? extends T>> typeToClass) {
        if (isPresent(query) || isPresent(orderBy)) {
            return Mono.error(new StorageException(StorageOperation.LIST, StorageErrorReason.UNKNOWN,
                    LIST_NOT_SUPPORTED));
        }
        String[] typeList = split(types);
        if (typeList == null || typeList.length == 0) {
            return Mono.error(new StorageException(StorageOperation.LIST, StorageErrorReason.UNKNOWN,
                    TYPES_REQUIRED));
        }
        String[] idList = split(ids);
        int limitIndex = idList == null ? IDS_INDEX : IDS_INDEX + 1;
        // a null LIMIT or OFFSET means no limit or no offset
        String sql = LIST_COLUMNS + QUALIFIED_ENTITY_TABLE + predicate(idList) + LIST_ORDER
                + " LIMIT $" + (limitIndex + 1) + " OFFSET $" + (limitIndex + 2);
        return execute(sql, statement -> {
            bindPredicate(statement, typeList, idList);
            bindNullable(statement, limitIndex, limit, Integer.class);
            return bindNullable(statement, limitIndex + 1, offset, Integer.class);
        }, result -> result.map(ListedRow::of))
                .collectList()
                .flatMap(rows -> totalCount(rows, offset, limit, typeList, idList)
                        .map(totalCount -> new PagedResult<T>(
                                rows.stream().map(row -> this.<T>toEntity(row, typeToClass)).toList(),
                                offset, limit, totalCount)))
                .onErrorMap(failure -> PostgresFailures.failure(StorageOperation.LIST, LIST_FAILED, failure));
    }

    // an empty page has no window count; past the last match or with limit 0 the real count is still owed
    private Mono<Integer> totalCount(List<ListedRow> rows, Integer offset, Integer limit, String[] typeList,
                                     String[] idList) {
        if (!rows.isEmpty()) {
            return Mono.just(Math.toIntExact(rows.getFirst().totalCount()));
        }
        if ((offset == null || offset <= 0) && (limit == null || limit > 0)) {
            return Mono.just(0);
        }
        return execute(COUNT_COLUMNS + QUALIFIED_ENTITY_TABLE + predicate(idList),
                statement -> bindPredicate(statement, typeList, idList),
                result -> result.map(row -> row.get(TOTAL_COUNT_COLUMN, Long.class)))
                .next()
                .map(Math::toIntExact);
    }

    private <T> T toEntity(ListedRow row, Function<String, Class<? extends T>> typeToClass) {
        Class<? extends T> entityClass = typeToClass.apply(row.type());
        if (entityClass == null) {
            throw new StorageException(StorageOperation.LIST, StorageErrorReason.UNKNOWN,
                    String.format("No class is known for the stored type %s.", row.type()));
        }
        return documents.fromDocument(row.data(), entityClass, StorageOperation.LIST);
    }

    private <R> Flux<R> execute(String sql, Function<Statement, Statement> binder,
                                Function<Result, Publisher<R>> resultMapper) {
        return PostgresStatements.execute(connectionFactory, sql, binder, resultMapper);
    }

    // parameter $1 holds the types, $2 the ids if any
    private static String predicate(String[] idList) {
        return idList == null ? TYPE_PREDICATE : TYPE_PREDICATE + ID_PREDICATE;
    }

    private static Statement bindPredicate(Statement statement, String[] typeList, String[] idList) {
        statement.bind(TYPES_INDEX, typeList);
        return idList == null ? statement : statement.bind(IDS_INDEX, idList);
    }

    private static <V> Statement bindNullable(Statement statement, int index, V value, Class<V> type) {
        return value == null ? statement.bindNull(index, type) : statement.bind(index, value);
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    // null when absent, so that "no filter" and "matches nothing" stay distinct
    private static String[] split(String commaSeparated) {
        if (!isPresent(commaSeparated)) {
            return null;
        }
        return Arrays.stream(commaSeparated.split(LIST_SEPARATOR))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toArray(String[]::new);
    }

    // event is null when the type records no events
    private record CreatedDocuments(String stored, String event) {
    }

    private record StoredRow(String type, String data) {

        static StoredRow of(Readable row) {
            return new StoredRow(row.get(TYPE_COLUMN, String.class), row.get(DATA_COLUMN, String.class));
        }
    }

    private record ListedRow(String type, String data, long totalCount) {

        static ListedRow of(Readable row) {
            return new ListedRow(row.get(TYPE_COLUMN, String.class), row.get(DATA_COLUMN, String.class),
                    row.get(TOTAL_COUNT_COLUMN, Long.class));
        }
    }
}
