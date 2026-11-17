package org.fiware.tmforum.common.repository;

import io.micronaut.http.HttpResponse;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.model.EntityListVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.QueryException;
import org.fiware.tmforum.common.querying.NgsiLdQueryRenderer;
import org.fiware.tmforum.storage.query.AttributePath;
import org.fiware.tmforum.storage.query.ComparisonOperator;
import org.fiware.tmforum.storage.query.EntityQuery;
import org.fiware.tmforum.storage.query.QueryFilter;
import org.fiware.tmforum.storage.query.SortKey;
import org.fiware.tmforum.storage.query.ValueType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NgsiLdEntityRepositoryTest {

	private GeneralProperties properties;
	private NgsiLdEntityRepository repository;

	@BeforeEach
	public void setUp() {
		properties = new GeneralProperties();
		repository = new NgsiLdEntityRepository(properties, null, null, null, null, null, null);
	}

	@Test
	public void findEntitiesPolymorphicForwardsTheIdFilterToTheBroker() {
		// Regression test: findEntitiesPolymorphic used to hard-code the broker's native
		// "id" query param to null, silently dropping any ?id= filter on polymorphic list
		// endpoints (e.g. resourceSpecification, which spans several NGSI-LD types).
		EntitiesApiClient entitiesApi = mock(EntitiesApiClient.class);
		NgsiLdEntityRepository repositoryWithClient = new NgsiLdEntityRepository(properties, entitiesApi, null, null, null, null,
				new NgsiLdQueryRenderer(properties));
		String requestedId = "urn:ngsi-ld:software-specification:0e2d5c4a-cf51-43cf-a510-dff06f62f4a3";

		when(entitiesApi.queryEntities(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
				any(), any(), any(), any(), any(), any()))
				.thenReturn(Mono.just(HttpResponse.ok(new EntityListVO())));

		repositoryWithClient
				.findEntitiesPolymorphic(0, 10, "software-specification,resource-specification", null, requestedId,
						type -> Object.class)
				.block();

		verify(entitiesApi).queryEntities(eq(properties.getTenant()), eq(requestedId), any(), any(), any(), any(),
				any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
	}

	@ParameterizedTest
	@MethodSource("queries")
	public void findEntitiesRendersTheQueryForTheBroker(EntityQuery query, String expectedQ, String expectedOrderBy) {
		EntitiesApiClient entitiesApi = mock(EntitiesApiClient.class);
		NgsiLdEntityRepository repositoryWithClient = new NgsiLdEntityRepository(properties, entitiesApi, null, null,
				null, null, new NgsiLdQueryRenderer(properties));
		when(entitiesApi.queryEntities(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
				any(), any(), any(), any(), any(), any()))
				.thenReturn(Mono.just(HttpResponse.ok(new EntityListVO())));

		repositoryWithClient.findEntities(0, 10, Object.class, query, null, "test").block();

		verify(entitiesApi).queryEntities(any(), any(), any(), eq("test"), any(), eq(expectedQ), eq(expectedOrderBy),
				any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
	}

	private static Stream<Arguments> queries() {
		// undeclared, so that rendering needs no field of the query class
		AttributePath color = new AttributePath(List.of("color"), false);
		QueryFilter isRed = new QueryFilter.Comparison(color, ComparisonOperator.EQUALS, ValueType.STRING,
				List.of("Red"));
		List<SortKey> byColorDescending = List.of(new SortKey(color, null, true));
		return Stream.of(
				Arguments.of(null, null, null),
				Arguments.of(new EntityQuery(Object.class, isRed, List.of()), "color==\"Red\"", null),
				Arguments.of(new EntityQuery(Object.class, null, byColorDescending), null, "color;desc"),
				Arguments.of(new EntityQuery(Object.class, isRed, byColorDescending), "color==\"Red\"",
						"color;desc"));
	}

	@ParameterizedTest
	@MethodSource("listCalls")
	public void aRejectedFilterArrivesAsQueryExceptionSignal(
			BiFunction<NgsiLdEntityRepository, EntityQuery, Mono<?>> listCall) {
		EntitiesApiClient entitiesApi = mock(EntitiesApiClient.class);
		NgsiLdEntityRepository repositoryWithClient = new NgsiLdEntityRepository(properties, entitiesApi, null, null,
				null, null, new NgsiLdQueryRenderer(properties));
		QueryFilter alternation = new QueryFilter.Comparison(new AttributePath(List.of("color"), false),
				ComparisonOperator.REGEX, ValueType.STRING, List.of("a|b"));

		Mono<?> list = assertDoesNotThrow(
				() -> listCall.apply(repositoryWithClient, new EntityQuery(Object.class, alternation, List.of())),
				"A rejected filter has to be an error signal of the Mono, not a synchronous throw.");

		assertThrows(QueryException.class, list::block,
				"A rejected filter is the client's error, not a StorageException.");
		verifyNoInteractions(entitiesApi);
	}

	private static Stream<Arguments> listCalls() {
		BiFunction<NgsiLdEntityRepository, EntityQuery, Mono<?>> plain =
				(repo, query) -> repo.findEntities(0, 10, Object.class, query, null, "test");
		BiFunction<NgsiLdEntityRepository, EntityQuery, Mono<?>> polymorphic =
				(repo, query) -> repo.findEntitiesPolymorphic(0, 10, "test", query, null, type -> Object.class);
		return Stream.of(Arguments.of(plain), Arguments.of(polymorphic));
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
