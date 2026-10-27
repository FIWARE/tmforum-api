package org.fiware.tmforum.common.caching;

import io.micronaut.configuration.lettuce.cache.RedisCache;
import io.micronaut.context.ApplicationContext;
import io.micronaut.inject.BeanDefinition;
import org.fiware.tmforum.common.CommonConstants;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Guards that {@link ClientTrackingRedisCache} still replaces lettuce's {@link RedisCache} on Micronaut 5. Checks the
 * bean definitions only, so no Redis server is needed.
 */
class ClientTrackingRedisCacheWiringTest {

	// never connected to, the definitions resolve without a connection
	private static final String REDIS_URI = "redis://localhost:6379";
	private static final String EXPIRY = "1s";

	@Test
	void replacesTheDefaultRedisCache() {
		Map<String, Object> properties = Map.of(
				"redis.uri", REDIS_URI,
				"redis.caches." + CommonConstants.ENTITIES_CACHE_NAME + ".expire-after-write", EXPIRY);
		try (ApplicationContext context = ApplicationContext.run(properties)) {
			Collection<BeanDefinition<RedisCache>> definitions = context.getBeanDefinitions(RedisCache.class);

			assertFalse(definitions.isEmpty(), "A Redis cache should be defined for the configured cache.");
			definitions.forEach(definition -> assertEquals(ClientTrackingRedisCache.class, definition.getBeanType(),
					"Only the client-tracking cache should be defined."));
		}
	}
}
