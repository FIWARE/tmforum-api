package org.fiware.tmforum.common.caching;

import io.lettuce.core.TrackingArgs;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.api.sync.RedisKeyCommands;
import io.micronaut.configuration.lettuce.cache.DefaultRedisCacheConfiguration;
import io.micronaut.configuration.lettuce.cache.RedisCache;
import io.micronaut.configuration.lettuce.cache.RedisCacheConfiguration;
import io.micronaut.context.BeanLocator;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.convert.ConversionService;
import jakarta.inject.Singleton;
import org.fiware.tmforum.common.CommonConstants;

@Singleton
@Bean
@EachBean(RedisCacheConfiguration.class)
@Replaces(RedisCache.class)
public class ClientTrackingRedisCache extends RedisCache {
    public ClientTrackingRedisCache(DefaultRedisCacheConfiguration defaultRedisCacheConfiguration,
                                    RedisCacheConfiguration redisCacheConfiguration,
                                    ConversionService<?> conversionService, BeanLocator beanLocator) {
        super(defaultRedisCacheConfiguration, redisCacheConfiguration, conversionService, beanLocator);
    }

    @Override
    protected RedisKeyCommands<byte[], byte[]> getRedisKeyCommands(StatefulConnection<byte[], byte[]> connection) {
        RedisKeyCommands<byte[], byte[]> commands;
        if (connection instanceof StatefulRedisConnection) {
            RedisCommands<byte[], byte[]> sync = ((StatefulRedisConnection<byte[], byte[]>) connection).sync();
            sync.clientTracking(TrackingArgs.Builder.enabled()
                    .bcast()
                    .prefixes(CommonConstants.SUBSCRIPTIONS_CACHE_NAME)
                    .prefixes(CommonConstants.ENTITIES_CACHE_NAME)
                    .noloop());
            commands = sync;
        } else {
            throw new ConfigurationException(INVALID_REDIS_CONNECTION_MESSAGE);
        }
        return commands;
    }
}
