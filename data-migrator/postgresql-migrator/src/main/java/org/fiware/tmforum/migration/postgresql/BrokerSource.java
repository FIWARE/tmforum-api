package org.fiware.tmforum.migration.postgresql;

import io.github.wistefan.mapping.EntityVOMapper;
import io.micronaut.context.ApplicationContext;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityListVO;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.CommonConstants;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Reads the broker of the source context through the application's own client and mapper, so the data maps exactly
 * as the NGSI-LD adapter reads it (reserved-key and query unescaping included). The broker backfill also deletes
 * attributes through it.
 */
class BrokerSource implements EntitySource {

	static final String SYS_ATTRS = "sysAttrs";
	static final String TYPES_PATH = "/ngsi-ld/v1/types";
	// deleteAll removes every instance, whatever its datasetId; the generated client cannot pass it nor a Link header
	static final String DELETE_ATTRIBUTE_PATH = "/ngsi-ld/v1/entities/%s/attrs/%s?deleteAll=true";
	static final String TYPE_LIST_FIELD = "typeList";
	private static final String LINK_HEADER = "Link";
	private static final String TENANT_HEADER = "NGSILD-Tenant";
	private static final String ACCEPT_HEADER = "Accept";
	private static final String JSON = "application/json";
	private static final int HTTP_OK = 200;
	private static final int HTTP_NO_CONTENT = 204;
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

	BrokerSource(ApplicationContext source) {
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
		List<EntityVO> entities = listed.stream().map(EntityVO::getId).map(this::entity).flatMap(Optional::stream)
				.toList();
		return new Page(entities, totalCount(response, type));
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
					"The broker reported no %s for %s, so the import cannot check it read everything.",
					generalProperties.getCountHeader(), type));
		}
		return Long.parseLong(count);
	}

	private String linkHeader() {
		return String.format(CommonConstants.JSON_LD_CONTEXT_LINK_TEMPLATE, generalProperties.getContextUrl());
	}
}
