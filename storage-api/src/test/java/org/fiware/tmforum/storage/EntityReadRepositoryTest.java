package org.fiware.tmforum.storage;

import org.fiware.tmforum.storage.query.AttributePath;
import org.fiware.tmforum.storage.query.ComparisonOperator;
import org.fiware.tmforum.storage.query.EntityQuery;
import org.fiware.tmforum.storage.query.QueryFilter;
import org.fiware.tmforum.storage.query.ValueType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class EntityReadRepositoryTest {

    private static final EntityQuery QUERY = new EntityQuery(String.class, new QueryFilter.Comparison(
            new AttributePath(List.of("name"), true), ComparisonOperator.EQUALS, ValueType.STRING, List.of("test")),
            List.of());
    private static final String ENTITY_TYPE = "product-offering";

    static Stream<Arguments> typedQueries() {
        return Stream.of(
                Arguments.of(0, 10, ENTITY_TYPE, QUERY),
                Arguments.of(null, null, ENTITY_TYPE, null),
                Arguments.of(5, 1, "category", QUERY));
    }

    @ParameterizedTest
    @MethodSource("typedQueries")
    void typedFindEntitiesDelegatesWithoutIds(Integer offset, Integer limit, String entityType,
                                                      EntityQuery query) {
        RecordingRepository repository = new RecordingRepository();

        repository.findEntities(offset, limit, entityType, String.class, query).block();

        assertEquals(offset, repository.offset);
        assertEquals(limit, repository.limit);
        assertSame(String.class, repository.entityClass);
        assertEquals(query, repository.query);
        assertNull(repository.ids);
        assertEquals(entityType, repository.types);
    }

    /** Captures the arguments of the abstract {@code findEntities}. */
    private static class RecordingRepository implements EntityReadRepository {
        private Integer offset;
        private Integer limit;
        private Class<?> entityClass;
        private EntityQuery query;
        private String ids;
        private String types;

        @Override
        public <T> Mono<T> get(URI id, Class<T> entityClass) {
            return Mono.empty();
        }

        @Override
        public <T> Mono<PagedResult<T>> findEntities(Integer offset, Integer limit, Class<T> entityClass,
                                                     EntityQuery query, String ids, String types) {
            this.offset = offset;
            this.limit = limit;
            this.entityClass = entityClass;
            this.query = query;
            this.ids = ids;
            this.types = types;
            return Mono.just(new PagedResult<>(List.of(), offset, limit, null));
        }

        @Override
        public <T> Mono<PagedResult<T>> findEntitiesPolymorphic(Integer offset, Integer limit, String types,
                                                                EntityQuery query, String ids,
                                                                Function<String, Class<? extends T>> typeToClass) {
            return Mono.empty();
        }
    }
}
