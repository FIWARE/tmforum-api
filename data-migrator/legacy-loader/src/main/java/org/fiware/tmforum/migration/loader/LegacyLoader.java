package org.fiware.tmforum.migration.loader;

import com.fasterxml.jackson.core.JsonProcessingException;
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
public class LegacyLoader {

	private volatile ApplicationContext applicationContext;

	private String brokerAddress;

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
		ObjectMapper objectMapper = applicationContext.getBean(ObjectMapper.class);
		objectMapper.setPropertyNamingStrategy(new PropertyRenamingStrategy());
		return Mono.zip(
				entityVOStream.map(entityVO -> {
					try {
						String originalString = objectMapper.writeValueAsString(entityVO);
						return objectMapper.readValue(originalString, EntityVO.class);
					} catch (JsonProcessingException e) {
						throw new RuntimeException(e);
					}
				}).map(renamendEntity -> {
					try {
						return entityVOMapper.fromEntityVO(renamendEntity, targetClass)
								.map(Optional::of)
								.onErrorResume(t -> {
									System.out.println("Was not able to translate entity: " + renamendEntity);
									return Mono.just(Optional.empty());
								});
					} catch (Exception e) {
						throw e;
					}
				}).toList(),
				oList -> Arrays.stream(oList).map(Optional.class::cast).filter(Optional::isPresent).map(Optional::get).map(targetClass::cast).toList()
		);
	}

	private void fixEntityVo(EntityVO entityVO) {
//		if (entityVO.getType().equals("usage")) {
//			Optional.ofNullable((PropertyVO) entityVO.getAdditionalProperties()
//							.get("usageCharacteristic"))
//					.map(PropertyVO::getValue)
//					.map(List.class::cast)
//					.orElse(List.of())
//					.stream()
//					.forEach(entry -> {
//						((Map) entry).put("charValue", ((Map) entry).get("value"));
//						((Map) entry).put("charId", ((Map) entry).get("id"));
//					});
//
//		}
//		if (entityVO.getType().equals("usageSpecification")) {
//			Optional.ofNullable((PropertyVO) entityVO.getAdditionalProperties()
//							.get("attachment"))
//					.map(PropertyVO::getValue)
//					.map(List.class::cast)
//					.orElse(List.of())
//					.stream()
//					.forEach(entry -> {
//						((Map) entry).put("attachementId", ((Map) entry).get("id"));
//					});
//			Optional.ofNullable((PropertyVO) entityVO.getAdditionalProperties()
//							.get("specCharacteristic"))
//					.map(PropertyVO::getValue)
//					.map(List.class::cast)
//					.orElse(List.of())
//					.stream()
//					.forEach(entry -> {
//						((Map) entry).put("specId", ((Map) entry).get("id"));
//					});
//		}

	}

	private String getLinkHeader(GeneralProperties generalProperties) {
		return String.format("<%s>; rel=\"http://www.w3.org/ns/json-ld#context\"; type=\"application/ld+json", generalProperties.getContextUrl());
	}

	private <T> Mono<List<T>> findEntities(Integer offset, Integer limit, Class<T> entityClass,
										   String query, String ids, String types) {

		if (applicationContext == null) {
			applicationContext = ApplicationContext.builder()
					.classLoader(Thread.currentThread().getContextClassLoader())
					.properties(Collections.singletonMap("micronaut.http.services.ngsi.url", brokerAddress))
					.build()
					.start();
		} else {
			if (!applicationContext.isRunning()) {
				applicationContext = ApplicationContext.builder()
						.classLoader(Thread.currentThread().getContextClassLoader())
						.properties(Collections.singletonMap("micronaut.http.services.ngsi.url", brokerAddress))
						.build()
						.start();
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
					throw new TmForumException("Was not able to list entities.", t, TmForumExceptionReason.UNKNOWN);
				});

	}

	public void setBrokerAddress(String brokerAddress) {
		this.brokerAddress = brokerAddress;
	}
}
