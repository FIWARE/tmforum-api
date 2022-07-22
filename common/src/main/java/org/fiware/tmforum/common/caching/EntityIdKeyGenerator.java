package org.fiware.tmforum.common.caching;

import io.micronaut.cache.interceptor.CacheKeyGenerator;
import io.micronaut.core.annotation.AnnotationMetadata;
import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.exception.CachingException;

/**
 * Generator for cache keys to be used for entity caches. It will use the entity-id as a cache-key.
 */
@Slf4j
public class EntityIdKeyGenerator implements CacheKeyGenerator {

	@Override
	public Object generateKey(AnnotationMetadata annotationMetadata, Object... params) {
		if (params.length < 1) {
			throw new CachingException("No entity provided");
		}
		if (params[0] instanceof EntityVO entityVO) {
			return entityVO.getId();
		}
		throw new CachingException(String.format("Key generator not supported for parameter type %s.", params[0].getClass()));
	}
}
