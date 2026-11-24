package org.fiware.tmforum.migration.postgresql;

import io.github.wistefan.mapping.EntityVOMapper;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.micronaut.context.ApplicationContext;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityListVO;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.CommonConstants;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Reads the broker of the source context through the application's own client and mapper, so the data maps exactly
 * as the NGSI-LD adapter reads it (reserved-key and query unescaping included). The broker backfill also deletes
 * attributes through it, and {@code cleanup-subscriptions} the NGSI-LD subscriptions.
 */
class BrokerSource implements EntitySource, SubscriptionStore {

	static final String SYS_ATTRS = "sysAttrs";
	static final String TYPES_PATH = "/ngsi-ld/v1/types";
	// deleteAll removes every instance, whatever its datasetId; the generated client cannot pass it nor a Link header
	static final String DELETE_ATTRIBUTE_PATH = "/ngsi-ld/v1/entities/%s/attrs/%s?deleteAll=true";
	static final String TYPE_LIST_FIELD = "typeList";
	// the generated subscription client sends no tenant on delete/list and cannot page
	static final String SUBSCRIPTIONS_PATH = "/ngsi-ld/v1/subscriptions?limit=%d&offset=%d";
	static final String SUBSCRIPTION_PATH = "/ngsi-ld/v1/subscriptions/%s";
	private static final String ID_FIELD = "id";
	private static final String NOTIFICATION_FIELD = "notification";
	private static final String ENDPOINT_FIELD = "endpoint";
	private static final String URI_FIELD = "uri";
	private static final String LIST_TYPE_SUFFIX = "_LIST";
	private static final String LINK_HEADER = "Link";
	private static final String TENANT_HEADER = "NGSILD-Tenant";
	private static final String ACCEPT_HEADER = "Accept";
	private static final String JSON = "application/json";
	private static final int HTTP_OK = 200;
	private static final int HTTP_NO_CONTENT = 204;
	private static final int HTTP_NOT_FOUND = 404;
	private static final String ID_SEPARATOR = ",";
	static final String READ_TIMEOUT_PROPERTY = "micronaut.http.services.ngsi.read-timeout";
	static final String CONNECT_TIMEOUT_PROPERTY = "micronaut.http.services.ngsi.connect-timeout";
	// used when the ngsi service configures none; the read timeout is Micronaut's client default
	static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(10);
	static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);

	private final EntitiesApiClient entitiesApi;
	private final EntityVOMapper entityVOMapper;
	private final GeneralProperties generalProperties;
	private final String brokerUrl;
	private final Duration readTimeout;
	private final HttpClient httpClient;
	private final JsonMapper jsonMapper = JsonMapper.builder().build();
	private final EntityTypeRegistry registry;
	private final Map<String, Boolean> multiInstanceTypes = new HashMap<>();

	BrokerSource(ApplicationContext source, EntityTypeRegistry registry) {
		this.registry = registry;
		this.entitiesApi = source.getBean(EntitiesApiClient.class);
		this.entityVOMapper = source.getBean(EntityVOMapper.class);
		this.generalProperties = source.getBean(GeneralProperties.class);
		this.brokerUrl = source.getRequiredProperty(MigrationContexts.NGSI_URL_PROPERTY, String.class);
		this.readTimeout = source.getProperty(READ_TIMEOUT_PROPERTY, Duration.class).orElse(DEFAULT_READ_TIMEOUT);
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(source.getProperty(CONNECT_TIMEOUT_PROPERTY, Duration.class)
						.orElse(DEFAULT_CONNECT_TIMEOUT))
				.build();
	}

	@Override
	public Page page(String type, int offset, int limit) {
		HttpResponse<EntityListVO> response = entitiesApi.queryEntities(generalProperties.getTenant(), null, null,
				type, null, null, null, null, null, null, null, null, limit, offset, true, SYS_ATTRS, linkHeader())
				.block();
		if (response == null) {
			throw new IllegalStateException(String.format("The broker returned no response for %s.", type));
		}
		List<EntityVO> listed = response.body() == null ? List.of() : List.copyOf(response.body());
		// Orion-LD's query omits multi-instance attributes (list references, productOrderItem); the by-id read has them
		List<EntityVO> entities = canHoldMultiInstanceAttributes(type)
				? listed.stream().map(EntityVO::getId).map(this::entity).flatMap(Optional::stream).toList()
				: listed;
		return new Page(entities, listed.size(), totalCount(response, type));
	}

	/**
	 * The broker's count of the type, from a one-entity page.
	 */
	long count(String type) {
		HttpResponse<EntityListVO> response = entitiesApi.queryEntities(generalProperties.getTenant(), null, null,
				type, null, null, null, null, null, null, null, null, 1, 0, true, null, linkHeader()).block();
		if (response == null) {
			throw new IllegalStateException(String.format("The broker returned no response for %s.", type));
		}
		return totalCount(response, type);
	}

	// a type no domain class maps is re-read too, nothing tells what it holds
	private boolean canHoldMultiInstanceAttributes(String type) {
		return multiInstanceTypes.computeIfAbsent(type, t -> registry.domainClass(t)
				.map(domainClass -> !listAttributes(domainClass).isEmpty())
				.orElse(true));
	}

	/**
	 * The target names of the class's list-valued attributes, which the broker stores as one instance per element.
	 */
	static Set<String> listAttributes(Class<?> domainClass) {
		return Arrays.stream(domainClass.getMethods())
				.map(method -> method.getAnnotation(AttributeSetter.class))
				.filter(Objects::nonNull)
				.filter(setter -> setter.value().name().endsWith(LIST_TYPE_SUFFIX))
				.map(AttributeSetter::targetName)
				.collect(Collectors.toSet());
	}

	/**
	 * The entity read by id, with every instance of its multi-instance attributes; empty when it is not stored.
	 */
	Optional<EntityVO> entity(URI id) {
		try {
			return Optional.ofNullable(entitiesApi.retrieveEntityById(id, generalProperties.getTenant(),
					null, null, SYS_ATTRS, linkHeader()).block()).map(HttpResponse::body);
		} catch (HttpClientResponseException e) {
			if (HttpStatus.NOT_FOUND.equals(e.getStatus())) {
				return Optional.empty();
			}
			throw e;
		}
	}

	/**
	 * The stored entities among {@code ids}, in one request; an unknown id is simply absent. Like any entity query
	 * on Orion-LD, it leaves out attributes with several instances; {@link #entity(URI)} has them.
	 */
	List<EntityVO> entities(Collection<URI> ids) {
		if (ids.isEmpty()) {
			return List.of();
		}
		String idList = ids.stream().map(URI::toString).collect(Collectors.joining(ID_SEPARATOR));
		HttpResponse<EntityListVO> response = entitiesApi.queryEntities(generalProperties.getTenant(), idList, null,
				null, null, null, null, null, null, null, null, null, ids.size(), 0, null, null, linkHeader()).block();
		if (response == null) {
			throw new IllegalStateException("The broker returned no response for an id query.");
		}
		return response.body() == null ? List.of() : List.copyOf(response.body());
	}

	/**
	 * Deletes every instance of the attribute. Throws when the broker answers anything but 204.
	 */
	void deleteAttribute(URI entityId, String attribute) {
		String path = String.format(DELETE_ATTRIBUTE_PATH, encode(entityId.toString()), encode(attribute));
		java.net.http.HttpResponse<String> response = send(request(path).DELETE(), "deleting " + attribute + " of " + entityId);
		if (response.statusCode() != HTTP_NO_CONTENT) {
			throw new IllegalStateException(String.format("Deleting %s of %s returned %d: %s", attribute, entityId,
					response.statusCode(), response.body()));
		}
	}

	@Override
	public List<BrokerSubscription> subscriptions(int pageSize) {
		Map<URI, BrokerSubscription> subscriptions = new LinkedHashMap<>();
		int offset = 0;
		int listed;
		do {
			java.net.http.HttpResponse<String> response = send(
					request(String.format(SUBSCRIPTIONS_PATH, pageSize, offset)).GET(), "listing the subscriptions");
			if (response.statusCode() != HTTP_OK) {
				throw new IllegalStateException(String.format("Listing the subscriptions returned %d: %s",
						response.statusCode(), response.body()));
			}
			JsonNode page = jsonMapper.readTree(response.body());
			int known = subscriptions.size();
			page.forEach(subscription -> {
				URI id = URI.create(subscription.path(ID_FIELD).asString());
				subscriptions.put(id, new BrokerSubscription(id,
						subscription.path(NOTIFICATION_FIELD).path(ENDPOINT_FIELD).path(URI_FIELD).asString(null)));
			});
			listed = page.size();
			// a broker that ignores the offset would repeat the first page forever
			if (listed > 0 && subscriptions.size() == known) {
				throw new IllegalStateException(String.format(
						"The broker repeated the subscriptions at offset %d; it does not page them.", offset));
			}
			offset += listed;
		} while (listed == pageSize);
		return List.copyOf(subscriptions.values());
	}

	@Override
	public boolean deleteSubscription(URI subscriptionId) {
		java.net.http.HttpResponse<String> response = send(
				request(String.format(SUBSCRIPTION_PATH, encode(subscriptionId.toString()))).DELETE(),
				"deleting subscription " + subscriptionId);
		if (response.statusCode() == HTTP_NOT_FOUND) {
			return false;
		}
		if (response.statusCode() != HTTP_NO_CONTENT) {
			throw new IllegalStateException(String.format("Deleting subscription %s returned %d: %s", subscriptionId,
					response.statusCode(), response.body()));
		}
		return true;
	}

	@Override
	public <T> T toDomain(EntityVO entity, Class<T> domainClass) {
		T domainEntity = entityVOMapper.fromEntityVO(entity, domainClass).block();
		if (domainEntity == null) {
			throw new IllegalStateException(String.format("%s mapped to nothing.", entity.getId()));
		}
		return domainEntity;
	}

	@Override
	public Set<String> types() {
		java.net.http.HttpResponse<String> response = send(request(TYPES_PATH).GET(), "listing the broker's types");
		if (response.statusCode() != HTTP_OK) {
			throw new IllegalStateException(String.format("Listing the broker's types returned %d: %s",
					response.statusCode(), response.body()));
		}
		Set<String> types = new TreeSet<>();
		jsonMapper.readTree(response.body()).path(TYPE_LIST_FIELD).forEach(node -> types.add(node.asString()));
		return types;
	}

	// these calls bypass the ngsi client, so they take over its timeouts to not hang on a silent broker
	private HttpRequest.Builder request(String path) {
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(brokerUrl + path))
				.header(ACCEPT_HEADER, JSON)
				.header(LINK_HEADER, linkHeader())
				.timeout(readTimeout);
		if (generalProperties.getTenant() != null) {
			request.header(TENANT_HEADER, generalProperties.getTenant());
		}
		return request;
	}

	private java.net.http.HttpResponse<String> send(HttpRequest.Builder request, String action) {
		try {
			return httpClient.send(request.build(), BodyHandlers.ofString());
		} catch (IOException e) {
			throw new IllegalStateException("Failed " + action + ".", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted " + action + ".", e);
		}
	}

	private static String encode(String pathSegment) {
		return URLEncoder.encode(pathSegment, StandardCharsets.UTF_8);
	}

	private long totalCount(HttpResponse<?> response, String type) {
		String count = response.header(generalProperties.getCountHeader());
		if (count == null) {
			throw new IllegalStateException(String.format(
					"The broker reported no %s for %s, so the run cannot check it read everything.",
					generalProperties.getCountHeader(), type));
		}
		return Long.parseLong(count);
	}

	private String linkHeader() {
		return String.format(CommonConstants.JSON_LD_CONTEXT_LINK_TEMPLATE, generalProperties.getContextUrl());
	}
}
