package org.fiware.tmforum.party;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.party.api.OrganizationApiTestClient;
import org.fiware.party.model.OrganizationCreateVO;
import org.fiware.party.model.OrganizationCreateVOTestExample;
import org.fiware.party.model.OrganizationVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.fiware.tmforum.party.domain.organization.Organization;
import org.fiware.tmforum.storage.postgresql.EventKind;
import org.fiware.tmforum.storage.postgresql.PostgresStorage;
import org.fiware.tmforum.storage.postgresql.PostgresTestEntityCleaner;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * party-catalog maps {@code organization}, so deleting one on PostgreSQL enqueues exactly one DELETE outbox row.
 */
@MicronautTest(packages = {"org.fiware.tmforum.party"}, environments = PostgresStorage.ENVIRONMENT,
		contextBuilder = FirstMatchConfigContextBuilder.class)
class OrganizationPostgresOutboxIT extends AbstractApiIT {

	private final OrganizationApiTestClient organizationApiTestClient;
	private final PostgresTestEntityCleaner postgresTestEntityCleaner;

	OrganizationPostgresOutboxIT(OrganizationApiTestClient organizationApiTestClient,
								 PostgresTestEntityCleaner postgresTestEntityCleaner,
								 EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper,
								 GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.organizationApiTestClient = organizationApiTestClient;
		this.postgresTestEntityCleaner = postgresTestEntityCleaner;
	}

	@Override
	protected String getEntityType() {
		return Organization.TYPE_ORGANIZATION;
	}

	@Test
	void deleteEnqueuesOneDeleteEvent() throws Exception {
		OrganizationCreateVO organizationCreateVO = OrganizationCreateVOTestExample.build().atSchemaLocation(null);
		organizationCreateVO.setOrganizationParentRelationship(null);
		HttpResponse<OrganizationVO> created = callAndCatch(
				() -> organizationApiTestClient.createOrganization(null, organizationCreateVO));
		assertEquals(HttpStatus.CREATED, created.getStatus());
		String id = created.body().getId();
		assertEquals(List.of(EventKind.CREATE), postgresTestEntityCleaner.outboxEventKinds(id));

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> organizationApiTestClient.deleteOrganization(null, id)).getStatus());

		assertEquals(List.of(EventKind.CREATE, EventKind.DELETE), postgresTestEntityCleaner.outboxEventKinds(id));
	}
}
