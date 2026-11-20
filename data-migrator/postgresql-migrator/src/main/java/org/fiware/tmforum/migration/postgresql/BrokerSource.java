package org.fiware.tmforum.migration.postgresql;

import io.github.wistefan.mapping.EntityVOMapper;
import io.micronaut.context.ApplicationContext;
import io.micronaut.http.HttpResponse;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityListVO;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.CommonConstants;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Reads the broker of the source context through the application's own client and mapper, so the data maps exactly
 * as the NGSI-LD adapter reads it (reserved-key and query unescaping included).
 */
class BrokerSource implements EntitySource {

	static final String SYS_ATTRS = "sysAttrs";
	static final String TYPES_PATH = "/ngsi-ld/v1/types";
	static final String TYPE_LIST_FIELD = "typeList";
	private static final String LINK_HEADER = "Link";
	private static final String TENANT_HEADER = "NGSILD-Tenant";
	private static final String ACCEPT_HEADER = "Accept";
	private static final String JSON = "application/json";
	private static final int HTTP_OK = 200;

	private final EntitiesApiClient entitiesApi;
	private final EntityVOMapper entityVOMapper;
	private final GeneralProperties generalProperties;
	private final String brokerUrl;
	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	BrokerSource(ApplicationContext source) {
		this.entitiesApi = source.getBean(EntitiesApiClient.class);
		this.entityVOMapper = source.getBean(EntityVOMapper.class);
		this.generalProperties = source.getBean(GeneralProperties.class);
		this.brokerUrl = source.getRequiredProperty(MigrationContexts.NGSI_URL_PROPERTY, String.class);
	}

	@Override
	public Page page(String type, int offset, int limit) {
		HttpResponse<EntityListVO> response = entitiesApi.queryEntities(generalProperties.getTenant(), null, null,
				type, null, null, null, null, null, null, null, null, limit, offset, true, SYS_ATTRS, linkHeader())
				.block();
		if (response == null) {
			throw new IllegalStateException(String.format("The broker returned no response for %s.", type));
		}
		List<EntityVO> entities = response.body() == null ? List.of() : List.copyOf(response.body());
		return new Page(entities, totalCount(response, type));
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
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(brokerUrl + TYPES_PATH))
				.header(ACCEPT_HEADER, JSON)
				.header(LINK_HEADER, linkHeader())
				.GET();
		if (generalProperties.getTenant() != null) {
			request.header(TENANT_HEADER, generalProperties.getTenant());
		}
		try {
			java.net.http.HttpResponse<String> response = httpClient.send(request.build(), BodyHandlers.ofString());
			if (response.statusCode() != HTTP_OK) {
				throw new IllegalStateException(String.format("Listing the broker's types returned %d: %s",
						response.statusCode(), response.body()));
			}
			Set<String> types = new TreeSet<>();
			jsonMapper.readTree(response.body()).path(TYPE_LIST_FIELD).forEach(node -> types.add(node.asString()));
			return types;
		} catch (IOException e) {
			throw new IllegalStateException("Was not able to list the broker's types.", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while listing the broker's types.", e);
		}
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
