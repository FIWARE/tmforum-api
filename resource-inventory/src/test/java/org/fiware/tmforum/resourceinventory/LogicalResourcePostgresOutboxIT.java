package org.fiware.tmforum.resourceinventory;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.resourceinventory.api.ResourceApiTestClient;
import org.fiware.resourceinventory.model.ResourceCreateVOTestExample;
import org.fiware.resourceinventory.model.ResourceUpdateVOTestExample;
import org.fiware.resourceinventory.model.ResourceVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.fiware.tmforum.common.test.PostgresqlOnly;
import org.fiware.tmforum.resource.ResourceTypeRegistry;
import org.fiware.tmforum.storage.postgresql.PostgresStorage;
import org.fiware.tmforum.storage.postgresql.PostgresOutboxInspector;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * resource-inventory maps only {@code resource}, so a sub-type create and patch on PostgreSQL enqueue no outbox row.
 */
@PostgresqlOnly
@MicronautTest(packages = {"org.fiware.tmforum.resourceinventory"}, environments = PostgresStorage.ENVIRONMENT,
		contextBuilder = FirstMatchConfigContextBuilder.class)
class LogicalResourcePostgresOutboxIT extends AbstractApiIT {

	private static final String LOGICAL_RESOURCE = "LogicalResource";

	private final ResourceApiTestClient resourceApiTestClient;
	private final PostgresOutboxInspector postgresOutboxInspector;

	LogicalResourcePostgresOutboxIT(ResourceApiTestClient resourceApiTestClient, PostgresOutboxInspector postgresOutboxInspector,
			EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.resourceApiTestClient = resourceApiTestClient;
		this.postgresOutboxInspector = postgresOutboxInspector;
	}

	@Override
	protected String getEntityType() {
		return ResourceTypeRegistry.ALL_RESOURCE_TYPES;
	}

	@Test
	void subTypeWritesEnqueueNoEvents() throws Exception {
		HttpResponse<ResourceVO> created = callAndCatch(() -> resourceApiTestClient.createResource(null,
				ResourceCreateVOTestExample.build().atSchemaLocation(null).place(null).resourceSpecification(null)
						.atType(LOGICAL_RESOURCE)));
		assertEquals(HttpStatus.CREATED, created.getStatus());
		String id = created.body().getId();

		assertEquals(HttpStatus.OK, callAndCatch(() -> resourceApiTestClient.patchResource(null, id,
				ResourceUpdateVOTestExample.build().atSchemaLocation(null).place(null).resourceSpecification(null)
						.name("updated"))).getStatus());

		assertEquals(List.of(), postgresOutboxInspector.outboxEventKinds(id));
	}
}
