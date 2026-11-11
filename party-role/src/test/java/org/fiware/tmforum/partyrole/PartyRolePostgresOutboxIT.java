package org.fiware.tmforum.partyrole;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.partyRole.api.PartyRoleApiTestClient;
import org.fiware.partyRole.model.PartyRoleCreateVOTestExample;
import org.fiware.partyRole.model.PartyRoleUpdateVOTestExample;
import org.fiware.partyRole.model.PartyRoleVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.fiware.tmforum.partyrole.domain.PartyRole;
import org.fiware.tmforum.storage.postgresql.PostgresStorage;
import org.fiware.tmforum.storage.postgresql.PostgresTestEntityCleaner;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * party-role maps no type to an event, so its writes on PostgreSQL never enqueue an outbox row.
 */
@MicronautTest(packages = {"org.fiware.tmforum.partyRole"}, environments = PostgresStorage.ENVIRONMENT,
		contextBuilder = FirstMatchConfigContextBuilder.class)
class PartyRolePostgresOutboxIT extends AbstractApiIT {

	private final PartyRoleApiTestClient partyRoleApiTestClient;
	private final PostgresTestEntityCleaner postgresTestEntityCleaner;

	PartyRolePostgresOutboxIT(PartyRoleApiTestClient partyRoleApiTestClient,
							  PostgresTestEntityCleaner postgresTestEntityCleaner,
							  EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper,
							  GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.partyRoleApiTestClient = partyRoleApiTestClient;
		this.postgresTestEntityCleaner = postgresTestEntityCleaner;
	}

	@Override
	protected String getEntityType() {
		return PartyRole.TYPE_PR;
	}

	@Test
	void writesEnqueueNoEvents() throws Exception {
		HttpResponse<PartyRoleVO> created = callAndCatch(() -> partyRoleApiTestClient.createPartyRole(null,
				PartyRoleCreateVOTestExample.build().atSchemaLocation(null).engagedParty(null)));
		assertEquals(HttpStatus.CREATED, created.getStatus());
		String id = created.body().getId();
		assertEquals(List.of(), postgresTestEntityCleaner.outboxEventKinds(id), "create enqueued an event");

		assertEquals(HttpStatus.OK, callAndCatch(() -> partyRoleApiTestClient.patchPartyRole(null, id,
				PartyRoleUpdateVOTestExample.build().atSchemaLocation(null).engagedParty(null).name("updated")))
				.getStatus());
		assertEquals(List.of(), postgresTestEntityCleaner.outboxEventKinds(id), "patch enqueued an event");

		assertEquals(HttpStatus.NO_CONTENT,
				callAndCatch(() -> partyRoleApiTestClient.deletePartyRole(null, id)).getStatus());
		assertEquals(List.of(), postgresTestEntityCleaner.outboxEventKinds(id), "delete enqueued an event");
	}
}
