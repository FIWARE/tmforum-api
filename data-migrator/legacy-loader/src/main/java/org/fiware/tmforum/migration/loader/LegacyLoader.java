package org.fiware.tmforum.migration.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.wistefan.mapping.EntityVOMapper;
import io.micronaut.context.ApplicationContext;
import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.*;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.ListVOMixin;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.util.*;
import java.util.stream.Stream;

/**
 * Load the objects from the broker and map them to JavaObjects, using the old version of the mapping library
 */
@Slf4j
public class LegacyLoader {

	private volatile ApplicationContext applicationContext;

	/**
	 * Returns a list of all objects with the given entity type. Calls the broker synchronously and collects all results.
	 * This is bad practice in general, but acceptable for data migration, having it more robust and understandable.
	 */
	public <T> List<T> getAll(EntityType entityType) throws InstantiationException, IllegalAccessException, NoSuchFieldException, MalformedURLException {
		System.out.println("get all " + entityType);
		List<T> entities = new ArrayList<>();
		int currentOffset = 0;
		boolean all = false;
		do {
			List<T> currentEntities = findEntities(currentOffset, 100, (Class<T>) entityType.entityClass(), null, null, entityType.entityType()).block();
			if (currentEntities != null && currentEntities.size() > 0) {
				entities.addAll(currentEntities);
			} else {
				all = true;
			}
			currentOffset += 100;
			if (currentEntities != null && currentEntities.size() < 100) {
				all = true;
			}
		}
		while (!all);

		return entities;
	}

	private <T> Mono<List<T>> zipToList(EntityVOMapper entityVOMapper, Stream<EntityVO> entityVOStream, Class<T> targetClass) {
		return Mono.zip(
				entityVOStream.map(entityVO -> {

					try {
						return entityVOMapper.fromEntityVO(entityVO, targetClass)
								.map(Optional::of)
								.onErrorResume(t -> {
									System.out.println("Was not able to translate entity: " + entityVO);
									return Mono.just(Optional.empty());
								});
					} catch (Exception e) {
						throw e;
					}
				}).toList(),
				oList -> Arrays.stream(oList).map(Optional.class::cast).filter(Optional::isPresent).map(Optional::get).map(targetClass::cast).toList()
		);
	}

	private EntityVO cleanEntity(EntityVO entityVO) {
		Map<String, AdditionalPropertyVO> additionalPropertyVOMap = entityVO.getAdditionalProperties();
		additionalPropertyVOMap.entrySet()
				.forEach(entry -> {
					if (entry.getValue() instanceof PropertyListVO eL && eL.size() == 0) {
						PropertyVO propertyVO = new PropertyVO();
						propertyVO.setValue(List.of());
						entityVO.setAdditionalProperties(entry.getKey(), propertyVO);
					} else {
						entityVO.setAdditionalProperties(entry.getKey(), entry.getValue());
					}
				});
		return entityVO;

	}

	private String getLinkHeader(GeneralProperties generalProperties) {
		return String.format("<%s>; rel=\"http://www.w3.org/ns/json-ld#context\"; type=\"application/ld+json", generalProperties.getContextUrl());
	}

	private <T> Mono<List<T>> findEntities(Integer offset, Integer limit, Class<T> entityClass,
										  String query, String ids, String types) {

		if (applicationContext == null) {
			applicationContext = ApplicationContext.run(Thread.currentThread().getContextClassLoader());
		} else {
			if (!applicationContext.isRunning()) {
				applicationContext = ApplicationContext.run(Thread.currentThread().getContextClassLoader());
			}
		}

		EntityVOMapper entityVOMapper = applicationContext.getBean(EntityVOMapper.class);
		GeneralProperties generalProperties = applicationContext.getBean(GeneralProperties.class);
		EntitiesApiClient entitiesApiClient = applicationContext.getBean(EntitiesApiClient.class);
		ObjectMapper objectMapper = applicationContext.getBean(ObjectMapper.class);
		objectMapper.addMixIn(PropertyListVO.class, ListVOMixin.class);
		objectMapper.addMixIn(RelationshipListVO.class, ListVOMixin.class);
		objectMapper.addMixIn(GeoPropertyListVO.class, ListVOMixin.class);

		return entitiesApiClient.queryEntities(generalProperties.getTenant(),
						ids,
						null,
						types,
						null,
						query,
						null,
						null,
						null,
						null,
						null,
						limit,
						offset,
						null,
						getLinkHeader(generalProperties))
				.map(List::stream)
				.flatMap(entityVOStream -> zipToList(entityVOMapper, entityVOStream, entityClass))
				.onErrorResume(t -> {
					log.warn("Was not able to list entities.", t);
					throw new TmForumException("Was not able to list entities.", t, TmForumExceptionReason.UNKNOWN);
				});

	}
}
