package org.fiware.tmforum.decoration;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.ApplicationContextBuilder;
import io.micronaut.context.Qualifier;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.r2dbc.spi.ConnectionFactory;
import org.fiware.tmforum.common.repository.BrokerBackedReferencesRepository;
import org.fiware.tmforum.common.repository.NgsiLdEntityRepository;
import org.fiware.tmforum.storage.EntityReadRepository;
import org.fiware.tmforum.storage.EntityRepository;
import org.fiware.tmforum.storage.EntityWriteRepository;
import org.fiware.tmforum.storage.ReferenceReadRepository;
import org.fiware.tmforum.storage.StorageAdapter;
import org.fiware.tmforum.storage.postgresql.OutboxProperties;
import org.fiware.tmforum.storage.postgresql.PostgresEntityRepository;
import org.fiware.tmforum.storage.postgresql.PostgresReferencesRepository;
import org.fiware.tmforum.storage.postgresql.PostgresStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Proves on every storage that a {@code @Primary} decorator takes the unqualified port lookups while
 * {@code @StorageAdapter} still reaches the single adapter, and that without a decorator nothing changes.
 */
class StorageAdapterQualifierTest {

    static final String DECORATE_PROPERTY = "storage-adapter-qualifier-test.decorate";
    static final String DECORATE = "true";

    private static final String ORION_LD_ENVIRONMENT = "orion-ld";
    private static final String SCORPIO_ENVIRONMENT = "scorpio";
    // a location without config files, so the test application-postgresql.yaml (datasource) is not loaded
    private static final String NO_CONFIG_FILES = "classpath:storage-adapter-qualifier-test/";
    private static final String NGSI_URL_PROPERTY = "micronaut.http.services.ngsi.url";
    // never contacted, adapter construction does no I/O
    private static final String DUMMY_BROKER_URL = "http://localhost:1026";
    private static final List<Class<?>> ENTITY_PORTS =
            List.of(EntityRepository.class, EntityReadRepository.class, EntityWriteRepository.class);

    static Stream<Arguments> storages() {
        return Stream.of(
                        Arguments.of(ORION_LD_ENVIRONMENT, NgsiLdEntityRepository.class,
                                BrokerBackedReferencesRepository.class),
                        Arguments.of(SCORPIO_ENVIRONMENT, NgsiLdEntityRepository.class,
                                BrokerBackedReferencesRepository.class),
                        Arguments.of(PostgresStorage.ENVIRONMENT, PostgresEntityRepository.class,
                                PostgresReferencesRepository.class))
                .flatMap(storage -> Stream.of(false, true).map(decorate -> Arguments.of(
                        storage.get()[0], decorate, storage.get()[1], storage.get()[2])));
    }

    @ParameterizedTest(name = "{0}, decorate={1}")
    @MethodSource("storages")
    void portsResolveThroughOptionalDecoratorToSingleAdapter(String environment, boolean decorate,
                                                              Class<?> entityAdapter, Class<?> referenceAdapter) {
        try (ApplicationContext context = startContext(environment, decorate)) {
            EntityRepository qualifiedEntity = context.getBean(EntityRepository.class, qualifier());
            ReferenceReadRepository qualifiedReference = context.getBean(ReferenceReadRepository.class, qualifier());
            assertInstanceOf(entityAdapter, qualifiedEntity, "@StorageAdapter should reach the entity adapter.");
            assertInstanceOf(referenceAdapter, qualifiedReference,
                    "@StorageAdapter should reach the reference adapter.");
            assertEquals(1, context.getBeanDefinitions(EntityRepository.class, qualifier()).size(),
                    "Exactly one entity adapter should be wired.");
            assertEquals(1, context.getBeanDefinitions(ReferenceReadRepository.class, qualifier()).size(),
                    "Exactly one reference adapter should be wired.");

            if (decorate) {
                RecordingEntityRepositoryDecorator entityDecorator =
                        context.getBean(RecordingEntityRepositoryDecorator.class);
                ENTITY_PORTS.forEach(port -> assertSame(entityDecorator, context.getBean(port),
                        port.getSimpleName() + " should resolve to the decorator."));
                assertSame(qualifiedEntity, entityDecorator.delegate(),
                        "The entity decorator should wrap the @StorageAdapter bean.");

                ReferenceReadRepository reference = context.getBean(ReferenceReadRepository.class);
                RecordingReferenceReadRepositoryDecorator referenceDecorator =
                        assertInstanceOf(RecordingReferenceReadRepositoryDecorator.class, reference,
                                "ReferenceReadRepository should resolve to the decorator.");
                assertSame(qualifiedReference, referenceDecorator.delegate(),
                        "The reference decorator should wrap the @StorageAdapter bean.");
            } else {
                ENTITY_PORTS.forEach(port -> assertSame(qualifiedEntity, context.getBean(port),
                        port.getSimpleName() + " should resolve to the adapter."));
                assertSame(qualifiedReference, context.getBean(ReferenceReadRepository.class),
                        "ReferenceReadRepository should resolve to the adapter.");
                assertEquals(1, context.getBeanDefinitions(EntityRepository.class).size(),
                        "Without a decorator the adapter should be the only EntityRepository.");
            }
        }
    }

    @Test
    void brokerAdaptersAbsentOnPostgresql() {
        try (ApplicationContext context = startContext(PostgresStorage.ENVIRONMENT, false)) {
            assertTrue(context.findBeanDefinition(NgsiLdEntityRepository.class).isEmpty(),
                    "The NGSI-LD entity adapter should not be wired on PostgreSQL.");
            assertTrue(context.findBeanDefinition(BrokerBackedReferencesRepository.class).isEmpty(),
                    "The NGSI-LD reference adapter should not be wired on PostgreSQL.");
        }
    }

    private static ApplicationContext startContext(String environment, boolean decorate) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(DECORATE_PROPERTY, String.valueOf(decorate));
        properties.put(NGSI_URL_PROPERTY, DUMMY_BROKER_URL);
        // no scheduled dispatcher may touch the mocked connection factory
        properties.put(OutboxProperties.ENABLED_PROPERTY, false);
        ApplicationContextBuilder builder = ApplicationContext.builder(environment)
                .overrideConfigLocations(NO_CONFIG_FILES)
                .properties(properties);
        // only PostgreSQL needs one; on a broker it would wake the r2dbc pool metrics, which need a named datasource
        if (PostgresStorage.ENVIRONMENT.equals(environment)) {
            builder.singletons(mock(ConnectionFactory.class));
        }
        return builder.start();
    }

    private static <T> Qualifier<T> qualifier() {
        return Qualifiers.byStereotype(StorageAdapter.class);
    }
}
