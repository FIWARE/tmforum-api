package org.fiware.tmforum.common.repository;

import io.micronaut.http.HttpResponse;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityListVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TmForumRepositoryTest {

	private GeneralProperties properties;
	private TmForumRepository repository;

	@BeforeEach
	public void setUp() {
		properties = new GeneralProperties();
		repository = new TmForumRepository(properties, null, null, null, null, null);
	}

	@Test
	public void findEntitiesPolymorphicForwardsTheIdFilterToTheBroker() {
		// Regression test: findEntitiesPolymorphic used to hard-code the broker's native
		// "id" query param to null, silently dropping any ?id= filter on polymorphic list
		// endpoints (e.g. resourceSpecification, which spans several NGSI-LD types).
		EntitiesApiClient entitiesApi = mock(EntitiesApiClient.class);
		TmForumRepository repositoryWithClient = new TmForumRepository(properties, entitiesApi, null, null, null, null);
		String requestedId = "urn:ngsi-ld:software-specification:0e2d5c4a-cf51-43cf-a510-dff06f62f4a3";

		when(entitiesApi.queryEntities(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
				any(), any(), any(), any(), any(), any()))
				.thenReturn(Mono.just(HttpResponse.ok(new EntityListVO())));

		repositoryWithClient
				.findEntitiesPolymorphic(0, 10, "software-specification,resource-specification", null, requestedId,
						null, type -> Object.class)
				.block();

		verify(entitiesApi).queryEntities(eq(properties.getTenant()), eq(requestedId), any(), any(), any(), any(),
				any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	public void extractsTheConfiguredHeader() {
		properties.setCountHeader("NGSILD-Results-Count");
		HttpResponse<Object> response = HttpResponse.ok().header("NGSILD-Results-Count", "42");

		assertEquals(42, repository.extractTotalCount(response));
	}

	@Test
	public void returnsNullWhenNoHeaderIsConfigured() {
		properties.setCountHeader(null);
		HttpResponse<Object> response = HttpResponse.ok().header("NGSILD-Results-Count", "42");

		assertNull(repository.extractTotalCount(response),
				"Without a configured header name, the total is unknown even if the broker sent one.");
	}

	@Test
	public void returnsNullWhenTheBrokerDidNotSendTheHeader() {
		properties.setCountHeader("NGSILD-Results-Count");
		HttpResponse<Object> response = HttpResponse.ok();

		assertNull(repository.extractTotalCount(response));
	}

	@Test
	public void returnsNullWhenTheHeaderIsNotAnInteger() {
		properties.setCountHeader("NGSILD-Results-Count");
		HttpResponse<Object> response = HttpResponse.ok().header("NGSILD-Results-Count", "not-a-number");

		assertNull(repository.extractTotalCount(response),
				"A malformed header must degrade to unknown, not fail the request.");
	}
}
