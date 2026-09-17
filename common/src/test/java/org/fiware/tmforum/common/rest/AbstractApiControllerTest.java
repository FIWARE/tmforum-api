package org.fiware.tmforum.common.rest;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.context.ServerRequestContext;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.querying.MyPojo;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.PagedResult;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbstractApiControllerTest {

	private static class TestController extends AbstractApiController<MyPojo> {
		TestController(QueryParser queryParser, TmForumRepository repository) {
			super(queryParser, null, repository, null);
		}
	}

	@Test
	public void listPolymorphicForwardsTheIdFilterToTheRepository() {
		// Regression test: listPolymorphic used to only forward "type" and "query" from the
		// parsed request to the repository, silently dropping any ?id= filter - even though it
		// was correctly parsed into QueryParams.id(). The non-polymorphic list() already forwarded
		// it; listPolymorphic must do the same.
		GeneralProperties properties = new GeneralProperties();
		QueryParser queryParser = new QueryParser(properties);
		TmForumRepository repository = mock(TmForumRepository.class);
		TestController controller = new TestController(queryParser, repository);

		String requestedId = "urn:ngsi-ld:software-specification:0e2d5c4a-cf51-43cf-a510-dff06f62f4a3";
		HttpRequest<?> request = HttpRequest.GET("/resourceSpecification?id=" + requestedId + "&fields=lifecycleStatus");

		when(repository.findEntitiesPolymorphic(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(Mono.just(new PagedResult<>(List.of(), 0, 10, null)));

		ServerRequestContext.with(request, () -> {
			controller.listPolymorphic(0, 10, "software-specification,resource-specification", MyPojo.class,
					type -> MyPojo.class).block();
		});

		verify(repository).findEntitiesPolymorphic(any(), any(), any(), any(), eq(requestedId), any(), any());
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
				mock(TmForumRepository.class));
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
				mock(TmForumRepository.class));
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
		TmForumRepository repository = mock(TmForumRepository.class);
		TestController controller = new TestController(new QueryParser(new GeneralProperties()), repository);
		HttpRequest<?> request = HttpRequest.GET("/resource?offset=10&limit=5");
		when(repository.findEntities(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(Mono.just(new PagedResult<>(List.of(), 10, 5, null)));

		ServerRequestContext.with(request, () -> {
			controller.list(10, 5, null, MyPojo.class).block();
		});

		verify(repository).findEntities(eq(10), eq(5), any(), any(), any(), any(), any());
	}
}
