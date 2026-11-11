package org.fiware.tmforum.common.test;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.http.HttpResponse;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityListVO;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.CommonConstants;
import org.fiware.tmforum.common.configuration.GeneralProperties;

import java.util.Objects;

/**
 * Cleans the NGSI-LD broker: queries the entities of the type and deletes them one by one.
 */
@Singleton
@Requires(env = Environment.TEST)
@Requires(notEnv = CommonConstants.POSTGRESQL_ENVIRONMENT)
@RequiredArgsConstructor
public class NgsiLdTestEntityCleaner implements TestEntityCleaner {

	private static final int QUERY_LIMIT = 1000;
	private static final String LINK_HEADER_FORMAT =
			"<%s>; rel=\"http://www.w3.org/ns/json-ld#context\"; type=\"application/ld+json";

	private final EntitiesApiClient entitiesApiClient;
	private final GeneralProperties generalProperties;

	@Override
	public void deleteAll(String entityTypes) {
		EntityListVO entityVOS = entitiesApiClient.queryEntities(null,
						null,
						null,
						entityTypes,
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						QUERY_LIMIT,
						0,
						false,
						null,
						String.format(LINK_HEADER_FORMAT, generalProperties.getContextUrl()))
				.map(HttpResponse::body)
				.block();
		entityVOS.stream()
				.filter(Objects::nonNull)
				.map(EntityVO::getId)
				.filter(Objects::nonNull)
				.forEach(eId -> entitiesApiClient.removeEntityById(eId, null, null).block());
	}
}
