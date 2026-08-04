package org.fiware.tmforum.common.rest;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.context.ServerRequestContext;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.querying.MyPojo;
import org.fiware.tmforum.common.querying.QueryParser;
import org.fiware.tmforum.common.repository.PagedResult;
import org.fiware.tmforum.common.repository.TmForumRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

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

	@AfterEach
	public void clearRequestContext() {
		ServerRequestContext.set(null);
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
		ServerRequestContext.set(
				HttpRequest.GET("/resourceSpecification?id=" + requestedId + "&fields=lifecycleStatus"));

		when(repository.findEntitiesPolymorphic(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(Mono.just(new PagedResult<>(List.of(), 0, 10, null)));

		controller.listPolymorphic(0, 10, "software-specification,resource-specification", MyPojo.class,
				type -> MyPojo.class).block();

		verify(repository).findEntitiesPolymorphic(any(), any(), any(), any(), eq(requestedId), any(), any());
	}
}
