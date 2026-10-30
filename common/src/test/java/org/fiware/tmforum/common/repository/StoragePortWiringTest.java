package org.fiware.tmforum.common.repository;

import io.micronaut.context.ApplicationContext;
import org.fiware.tmforum.storage.EntityReadRepository;
import org.fiware.tmforum.storage.EntityRepository;
import org.fiware.tmforum.storage.EntityWriteRepository;
import org.fiware.tmforum.storage.ReferenceReadRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Guards that each port interface resolves to exactly one NGSI-LD adapter bean. No broker is contacted.
 */
class StoragePortWiringTest {

	private static ApplicationContext context;

	@BeforeAll
	static void startContext() {
		context = ApplicationContext.run();
	}

	@AfterAll
	static void stopContext() {
		context.close();
	}

	static Stream<Arguments> ports() {
		return Stream.of(
				Arguments.of(EntityRepository.class, NgsiLdEntityRepository.class),
				Arguments.of(EntityReadRepository.class, NgsiLdEntityRepository.class),
				Arguments.of(EntityWriteRepository.class, NgsiLdEntityRepository.class),
				Arguments.of(SubscriptionRepository.class, NgsiLdEntityRepository.class),
				Arguments.of(ReferenceReadRepository.class, BrokerBackedReferencesRepository.class));
	}

	@ParameterizedTest(name = "{0} -> {1}")
	@MethodSource("ports")
	void portResolvesToSingleAdapter(Class<?> port, Class<?> adapter) {
		assertEquals(1, context.getBeanDefinitions(port).size(), "The port should have exactly one bean.");
		assertInstanceOf(adapter, context.getBean(port), "The port should resolve to the NGSI-LD adapter.");
	}
}
