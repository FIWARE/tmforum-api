package org.fiware.tmforum.common.rest;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.context.ServerRequestContext;
import org.fiware.tmforum.common.CommonConstants;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.notification.EntityEventPublisher;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.querying.MyPojo;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.storage.PagedResult;
import org.fiware.tmforum.storage.EntityRepository;
import org.fiware.tmforum.storage.query.AttributePath;
import org.fiware.tmforum.storage.query.ComparisonOperator;
import org.fiware.tmforum.storage.query.EntityQuery;
import org.fiware.tmforum.storage.query.QueryFilter;
import org.fiware.tmforum.storage.query.QueryFilter.Comparison;
import org.fiware.tmforum.storage.query.SortKey;
import org.fiware.tmforum.storage.query.ValueType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AbstractApiControllerTest {

	private static final String LIST_TYPE = "my-pojo";
	private static final String COLOR_VALUE = "Red";
	private static final String COLOR_FILTER = "color=" + COLOR_VALUE;
	private static final AttributePath COLOR_PATH = new AttributePath(List.of("color"), true);
	private static final String COLOR_SORT = "sort=-color";

	private static class TestController extends AbstractApiController<MyPojo> {
		TestController(QueryParser queryParser, EntityRepository repository) {
			super(queryParser, null, repository, null);
		}
	}

	@Test
	public void listPolymorphicForwardsTheIdFilterToTheRepository() {
		// Regression test: listPolymorphic used to only forward "type" and "query" from the
		// parsed request to the repository, silently dropping any ?id= filter - even though it
		// was correctly parsed into ParsedQuery.ids(). The non-polymorphic list() already forwarded
		// it; listPolymorphic must do the same.
		GeneralProperties properties = new GeneralProperties();
		QueryParser queryParser = new QueryParser(new GeneralProperties());
		EntityRepository repository = mock(EntityRepository.class);
		TestController controller = new TestController(queryParser, repository);

		String requestedId = "urn:ngsi-ld:software-specification:0e2d5c4a-cf51-43cf-a510-dff06f62f4a3";
		HttpRequest<?> request = HttpRequest.GET("/resourceSpecification?id=" + requestedId + "&fields=lifecycleStatus");

		when(repository.findEntitiesPolymorphic(any(), any(), any(), any(), any(), any()))
				.thenReturn(Mono.just(new PagedResult<>(List.of(), 0, 10, null)));

		ServerRequestContext.with(request, () -> {
			controller.listPolymorphic(0, 10, "software-specification,resource-specification", MyPojo.class,
					type -> MyPojo.class).block();
		});

		verify(repository).findEntitiesPolymorphic(any(), any(), any(), any(), eq(requestedId), any());
	}

	/**
	 * A non-integer offset/limit is bound as null by Micronaut, because the generated api interfaces
	 * declare them as nullable Integers. Without an explicit check, the invalid value is therefore
	 * indistinguishable from an absent one and the request is answered with the default page instead
	 * of being rejected.
	 *
	 * @param message     description of the concrete case
	 * @param queryString query string of the listing request
	 */
	@ParameterizedTest
	@MethodSource("provideNonIntegerPaginationParameters")
	public void listRejectsNonIntegerPaginationParameters(String message, String queryString) {
		TestController controller = new TestController(new QueryParser(new GeneralProperties()),
				mock(EntityRepository.class));
		HttpRequest<?> request = HttpRequest.GET("/resource" + queryString);

		Supplier<TmForumException> assertion = () -> assertThrows(TmForumException.class,
				() -> controller.list(null, null, null, MyPojo.class), message);
		TmForumException exception = ServerRequestContext.with(request, assertion);

		assertEquals(TmForumExceptionReason.INVALID_DATA, exception.getExceptionReason(), message);
	}

	@ParameterizedTest
	@MethodSource("provideNonIntegerPaginationParameters")
	public void listPolymorphicRejectsNonIntegerPaginationParameters(String message, String queryString) {
		TestController controller = new TestController(new QueryParser(new GeneralProperties()),
				mock(EntityRepository.class));
		HttpRequest<?> request = HttpRequest.GET("/resource" + queryString);

		Supplier<TmForumException> assertion = () -> assertThrows(TmForumException.class,
				() -> controller.listPolymorphic(null, null, null, MyPojo.class, type -> MyPojo.class), message);
		TmForumException exception = ServerRequestContext.with(request, assertion);

		assertEquals(TmForumExceptionReason.INVALID_DATA, exception.getExceptionReason(), message);
	}

	private static Stream<Arguments> provideNonIntegerPaginationParameters() {
		return Stream.of(
				Arguments.of("A non-numeric limit should be rejected.", "?limit=my-non-int"),
				Arguments.of("A non-numeric offset should be rejected.", "?offset=my-non-int"),
				Arguments.of("A decimal limit should be rejected.", "?limit=1.5"),
				Arguments.of("A decimal offset should be rejected.", "?offset=1.5"),
				Arguments.of("An empty limit should be rejected.", "?limit="),
				Arguments.of("A limit exceeding the integer range should be rejected.", "?limit=99999999999"),
				Arguments.of("Invalid values should be rejected next to a valid filter.",
						"?name=my-name&limit=my-non-int"));
	}

	/**
	 * The rejection has to stay limited to the invalid values - properly provided integers still have
	 * to be passed on to the repository.
	 */
	@Test
	public void listAcceptsIntegerPaginationParameters() {
		EntityRepository repository = mock(EntityRepository.class);
		TestController controller = new TestController(new QueryParser(new GeneralProperties()), repository);
		HttpRequest<?> request = HttpRequest.GET("/resource?offset=10&limit=5");
		when(repository.findEntities(any(), any(), any(), any(), any(), any()))
				.thenReturn(Mono.just(new PagedResult<>(List.of(), 10, 5, null)));

		ServerRequestContext.with(request, () -> {
			controller.list(10, 5, null, MyPojo.class).block();
		});

		verify(repository).findEntities(eq(10), eq(5), any(), any(), any(), any());
	}

	/**
	 * DELETE used to check only that the id is an NGSI-LD id, so an id of another type deleted that other entity
	 * through this endpoint and answered 204, while GET and PATCH on the same URL answered 404.
	 */
	@ParameterizedTest
	@MethodSource("provideForeignIds")
	public void deleteRejectsAnIdOfAnotherType(String message, String id) {
		EntityEventPublisher publisher = mock(EntityEventPublisher.class);
		TestController controller = new TestController(new QueryParser(new GeneralProperties()),
				mock(EntityRepository.class));
		controller.setEventPublisher(publisher);

		TmForumException exception = assertThrows(TmForumException.class,
				() -> controller.delete(id, LIST_TYPE), message);

		assertEquals(TmForumExceptionReason.NOT_FOUND, exception.getExceptionReason(), message);
		verifyNoInteractions(publisher);
	}

	private static Stream<Arguments> provideForeignIds() {
		return Stream.of(
				Arguments.of("An id of another type should not be deleted.", "urn:ngsi-ld:category:1"),
				Arguments.of("A non-ngsi-ld id should not be deleted.", "not-an-id"),
				Arguments.of("An id without a type segment should not be deleted.", "urn:ngsi-ld"));
	}

	@ParameterizedTest
	@MethodSource("provideOwnedIds")
	public void deletePassesAnIdOfAnAcceptedTypeToThePublisher(String acceptedTypes, String id,
			List<String> expectedTypes) {
		EntityEventPublisher publisher = mock(EntityEventPublisher.class);
		when(publisher.deleteWithEvent(any(), any())).thenReturn(Mono.empty());
		TestController controller = new TestController(new QueryParser(new GeneralProperties()),
				mock(EntityRepository.class));
		controller.setEventPublisher(publisher);

		controller.delete(id, acceptedTypes).block();

		verify(publisher).deleteWithEvent(URI.create(id), expectedTypes);
	}

	private static Stream<Arguments> provideOwnedIds() {
		return Stream.of(
				Arguments.of(LIST_TYPE, "urn:ngsi-ld:" + LIST_TYPE + ":1", List.of(LIST_TYPE)),
				// a polymorphic endpoint owns every registered sub-type, so each of them stays deletable
				Arguments.of(LIST_TYPE + ",sub-pojo", "urn:ngsi-ld:sub-pojo:1", List.of(LIST_TYPE, "sub-pojo")),
				Arguments.of(LIST_TYPE + ", sub-pojo", "urn:ngsi-ld:" + LIST_TYPE + ":1",
						List.of(LIST_TYPE, "sub-pojo")));
	}

	/**
	 * An unbounded limit would make the storage read, count and map every stored entity of the requested types in
	 * one request, so anything above the maximum is clamped to it instead of being served.
	 */
	@ParameterizedTest
	@MethodSource("provideLimits")
	public void listClampsTheLimitToTheMaximum(Integer requestedLimit, int expectedLimit) {
		EntityRepository repository = mock(EntityRepository.class);
		TestController controller = new TestController(new QueryParser(new GeneralProperties()), repository);
		when(repository.findEntities(any(), any(), any(), any(), any(), any()))
				.thenReturn(Mono.just(new PagedResult<>(List.of(), 0, expectedLimit, null)));
		when(repository.findEntitiesPolymorphic(any(), any(), any(), any(), any(), any()))
				.thenReturn(Mono.just(new PagedResult<>(List.of(), 0, expectedLimit, null)));

		ServerRequestContext.with(HttpRequest.GET("/resource"), () -> {
			controller.list(0, requestedLimit, LIST_TYPE, MyPojo.class).block();
			controller.listPolymorphic(0, requestedLimit, LIST_TYPE, MyPojo.class, type -> MyPojo.class).block();
		});

		verify(repository).findEntities(any(), eq(expectedLimit), any(), any(), any(), any());
		verify(repository).findEntitiesPolymorphic(any(), eq(expectedLimit), any(), any(), any(), any());
	}

	private static Stream<Arguments> provideLimits() {
		return Stream.of(
				Arguments.of(null, CommonConstants.DEFAULT_LIMIT),
				Arguments.of(1, 1),
				Arguments.of(CommonConstants.MAX_LIMIT - 1, CommonConstants.MAX_LIMIT - 1),
				Arguments.of(CommonConstants.MAX_LIMIT, CommonConstants.MAX_LIMIT),
				Arguments.of(CommonConstants.MAX_LIMIT + 1, CommonConstants.MAX_LIMIT),
				Arguments.of(Integer.MAX_VALUE, CommonConstants.MAX_LIMIT));
	}

	@ParameterizedTest
	@MethodSource("provideInvalidPagination")
	public void listRejectsAnOutOfRangePaginationParameter(String message, Integer offset, Integer limit) {
		TestController controller = new TestController(new QueryParser(new GeneralProperties()),
				mock(EntityRepository.class));

		Supplier<TmForumException> assertion = () -> assertThrows(TmForumException.class,
				() -> controller.list(offset, limit, LIST_TYPE, MyPojo.class), message);
		TmForumException exception = ServerRequestContext.with(HttpRequest.GET("/resource"), assertion);

		assertEquals(TmForumExceptionReason.INVALID_DATA, exception.getExceptionReason(), message);
	}

	private static Stream<Arguments> provideInvalidPagination() {
		return Stream.of(
				Arguments.of("A limit below one should be rejected.", 0, 0),
				Arguments.of("A negative limit should be rejected.", 0, -1),
				Arguments.of("A negative offset should be rejected.", -1, 10));
	}

	@ParameterizedTest
	@MethodSource("provideListQueries")
	public void listPassesTheParsedFilterAndSortToTheRepository(String queryString, EntityQuery expected) {
		EntityRepository repository = mock(EntityRepository.class);
		TestController controller = new TestController(new QueryParser(new GeneralProperties()), repository);
		when(repository.findEntities(any(), any(), any(), any(), any(), any()))
				.thenReturn(Mono.just(new PagedResult<>(List.of(), 0, 10, null)));

		ServerRequestContext.with(HttpRequest.GET("/resource?" + queryString), () -> {
			controller.list(0, 10, LIST_TYPE, MyPojo.class).block();
		});

		verify(repository).findEntities(any(), any(), eq(MyPojo.class), eq(expected), any(), eq(LIST_TYPE));
	}

	@ParameterizedTest
	@MethodSource("provideListQueries")
	public void listPolymorphicPassesTheParsedFilterAndSortToTheRepository(String queryString, EntityQuery expected) {
		EntityRepository repository = mock(EntityRepository.class);
		TestController controller = new TestController(new QueryParser(new GeneralProperties()), repository);
		when(repository.findEntitiesPolymorphic(any(), any(), any(), any(), any(), any()))
				.thenReturn(Mono.just(new PagedResult<>(List.of(), 0, 10, null)));

		ServerRequestContext.with(HttpRequest.GET("/resource?" + queryString), () -> {
			controller.listPolymorphic(0, 10, LIST_TYPE, MyPojo.class, type -> MyPojo.class).block();
		});

		verify(repository).findEntitiesPolymorphic(any(), any(), eq(LIST_TYPE), eq(expected), any(), any());
	}

	private static Stream<Arguments> provideListQueries() {
		QueryFilter colorFilter = new Comparison(COLOR_PATH, ComparisonOperator.EQUALS, ValueType.STRING,
				List.of(COLOR_VALUE));
		SortKey colorSort = new SortKey(COLOR_PATH, ValueType.STRING, true);
		return Stream.of(
				Arguments.of("fields=color", null),
				Arguments.of(COLOR_FILTER, new EntityQuery(MyPojo.class, colorFilter, List.of())),
				Arguments.of(COLOR_SORT, new EntityQuery(MyPojo.class, null, List.of(colorSort))),
				Arguments.of(COLOR_FILTER + "&" + COLOR_SORT,
						new EntityQuery(MyPojo.class, colorFilter, List.of(colorSort))));
	}
}
