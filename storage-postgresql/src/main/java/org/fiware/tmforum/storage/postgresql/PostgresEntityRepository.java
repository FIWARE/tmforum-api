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
import org.fiware.tmforum.common.exception.QueryException;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.storage.EntityRepository;
import org.fiware.tmforum.storage.PagedResult;
import org.fiware.tmforum.storage.StorageErrorReason;
import org.fiware.tmforum.storage.StorageException;
import org.fiware.tmforum.storage.StorageOperation;
import org.fiware.tmforum.storage.query.EntityQuery;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.fiware.tmforum.storage.postgresql.PostgresStorage.QUALIFIED_ENTITY_TABLE;
import static org.fiware.tmforum.storage.postgresql.PostgresStorage.QUALIFIED_OUTBOX_TABLE;

/**
 * PostgreSQL adapter of the entity port: one row per entity in {@code tmforum.entity}, the domain object as jsonb.
 * Lists and counts only see current rows; their filter and sort are rendered by {@link JsonbQueryRenderer}.
 */
@Singleton
@Primary
@Requires(env = PostgresStorage.ENVIRONMENT)
public class PostgresEntityRepository implements EntityRepository {

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
    private static final String ORDER_SEPARATOR = " ";
    private static final String FILTER_SEPARATOR = " AND ";
    private static final String PAGE = " LIMIT $%d OFFSET $%d";
    private static final String COUNT_COLUMNS = "SELECT count(*) AS total_count FROM ";
    // compiles the pattern whether or not any row would reach it
    private static final String COMPILE_PATTERN = "SELECT '' ~ $1";
    private static final String INVALID_PATTERN = "'%s' is not a valid regular expression: %s";

    private final ConnectionFactory connectionFactory;
    private final DocumentMapper documents;
    private final DocumentMapper eventDocuments;
    private final JsonbQueryRenderer renderer;

    public PostgresEntityRepository(ConnectionFactory connectionFactory, PostgresDocumentMapper documentMapper) {
        this.connectionFactory = connectionFactory;
        this.documents = documentMapper.storage();
        this.eventDocuments = documentMapper.event();
        this.renderer = new JsonbQueryRenderer(new DocumentPathResolver(documents));
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
                                                 EntityQuery query, String ids, String types) {
        return list(offset, limit, types, query, ids, type -> entityClass);
    }

    @Override
    public <T> Mono<PagedResult<T>> findEntitiesPolymorphic(Integer offset, Integer limit, String types,
                                                            EntityQuery query, String ids,
                                                            Function<String, Class<? extends T>> typeToClass) {
        return list(offset, limit, types, query, ids, typeToClass);
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

    private <T> Mono<PagedResult<T>> list(Integer offset, Integer limit, String types, EntityQuery query,
                                          String ids, Function<String, Class<? extends T>> typeToClass) {
        String[] typeList = split(types);
        if (typeList == null || typeList.length == 0) {
            return Mono.error(new StorageException(StorageOperation.LIST, StorageErrorReason.UNKNOWN,
                    TYPES_REQUIRED));
        }
        // deferred so a rendering failure is an error signal, mapped like any other
        return Mono.defer(() -> listPage(offset, limit, listStatements(typeList, split(ids), query), query,
                        typeToClass))
                .onErrorMap(failure -> PostgresFailures.queryFailure(StorageOperation.LIST, LIST_FAILED, failure));
    }

    private <T> Mono<PagedResult<T>> listPage(Integer offset, Integer limit, ListStatements statements,
                                              EntityQuery query, Function<String, Class<? extends T>> typeToClass) {
        // one connection for the pattern check, the page and the count
        return Mono.usingWhen(connectionFactory.create(),
                connection -> compilePatterns(connection, query)
                        .then(PostgresStatements.execute(connection, statements.page(),
                                        statement -> bindPage(statement, statements, offset, limit),
                                        result -> result.map(ListedRow::of))
                                .collectList())
                        .flatMap(rows -> totalCount(connection, statements, rows, offset, limit)
                                .map(totalCount -> new PagedResult<T>(
                                        rows.stream().map(row -> this.<T>toEntity(row, typeToClass)).toList(),
                                        offset, limit, totalCount))),
                Connection::close);
    }

    // $1 holds the types, $2 the ids if any, then the filter's and the order's parameters, then LIMIT and OFFSET
    private ListStatements listStatements(String[] typeList, String[] idList, EntityQuery query) {
        List<Object> bindings = new ArrayList<>();
        bindings.add(typeList);
        String predicate = TYPE_PREDICATE;
        if (idList != null) {
            bindings.add(idList);
            predicate += ID_PREDICATE;
        }
        if (query != null && query.filter() != null) {
            SqlFragment filter = renderer.filter(query, bindings.size() + 1);
            bindings.addAll(filter.bindings());
            predicate += FILTER_SEPARATOR + filter.sql();
        }
        List<Object> predicateBindings = List.copyOf(bindings);
        SqlFragment orderBy = renderer.orderBy(query, bindings.size() + 1);
        bindings.addAll(orderBy.bindings());
        String page = LIST_COLUMNS + QUALIFIED_ENTITY_TABLE + predicate + ORDER_SEPARATOR + orderBy.sql()
                + String.format(PAGE, bindings.size() + 1, bindings.size() + 2);
        return new ListStatements(page, bindings, COUNT_COLUMNS + QUALIFIED_ENTITY_TABLE + predicate,
                predicateBindings);
    }

    // an invalid pattern is the client's error, and must be one even when no row reaches it
    private static Mono<Void> compilePatterns(Connection connection, EntityQuery query) {
        return Flux.fromIterable(RegexPatterns.of(query))
                .concatMap(pattern -> PostgresStatements.execute(connection, COMPILE_PATTERN,
                                statement -> statement.bind(0, pattern), Result::getRowsUpdated)
                        .onErrorMap(PostgresFailures::isInvalidPattern,
                                failure -> new QueryException(String.format(INVALID_PATTERN, pattern,
                                        failure.getMessage()))))
                .then();
    }

    // an empty page has no window count; past the last match or with limit 0 the real count is still owed
    private Mono<Integer> totalCount(Connection connection, ListStatements statements, List<ListedRow> rows,
                                     Integer offset, Integer limit) {
        if (!rows.isEmpty()) {
            return Mono.just(Math.toIntExact(rows.getFirst().totalCount()));
        }
        if ((offset == null || offset <= 0) && (limit == null || limit > 0)) {
            return Mono.just(0);
        }
        return PostgresStatements.execute(connection, statements.count(),
                        statement -> bindAll(statement, statements.countBindings()),
                        result -> result.map(row -> row.get(TOTAL_COUNT_COLUMN, Long.class)))
                .next()
                .map(Math::toIntExact);
    }

    // a null LIMIT or OFFSET means no limit or no offset
    private static Statement bindPage(Statement statement, ListStatements statements, Integer offset, Integer limit) {
        int limitIndex = statements.pageBindings().size();
        bindAll(statement, statements.pageBindings());
        bindNullable(statement, limitIndex, limit, Integer.class);
        return bindNullable(statement, limitIndex + 1, offset, Integer.class);
    }

    private static Statement bindAll(Statement statement, List<Object> bindings) {
        for (int i = 0; i < bindings.size(); i++) {
            statement.bind(i, bindings.get(i));
        }
        return statement;
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

    // the count shares the page's predicate and its parameters, without the order and the paging
    private record ListStatements(String page, List<Object> pageBindings, String count, List<Object> countBindings) {
    }

    private record ListedRow(String type, String data, long totalCount) {

        static ListedRow of(Readable row) {
            return new ListedRow(row.get(TYPE_COLUMN, String.class), row.get(DATA_COLUMN, String.class),
                    row.get(TOTAL_COUNT_COLUMN, Long.class));
        }
    }
}
