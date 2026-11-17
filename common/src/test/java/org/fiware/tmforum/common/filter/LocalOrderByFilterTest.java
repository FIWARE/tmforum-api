package org.fiware.tmforum.common.filter;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.filter.ClientFilterChain;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LocalOrderByFilterTest {

	private static final String ENTITIES_PATH = "/ngsi-ld/v1/entities";

	private final LocalOrderByFilter filter = new LocalOrderByFilter();

	@ParameterizedTest(name = "{0}")
	@MethodSource("requests")
	void addsLocalOnlyToSortedQueries(String query, String expectedLocal) {
		ClientFilterChain chain = mock(ClientFilterChain.class);
		filter.doFilter(HttpRequest.GET(ENTITIES_PATH + query), chain);

		ArgumentCaptor<MutableHttpRequest<?>> forwarded = ArgumentCaptor.forClass(MutableHttpRequest.class);
		verify(chain).proceed(forwarded.capture());
		// the client sends getUri(), so the parameter must be in there
		String sentLocal = new QueryStringDecoder(forwarded.getValue().getUri())
				.parameters().getOrDefault(LocalOrderByFilter.LOCAL_PARAMETER, List.of()).stream()
				.findFirst().orElse(null);
		assertEquals(expectedLocal, sentLocal,
				"Only a sorted query without an explicit local must get local=true.");
	}

	private static Stream<Arguments> requests() {
		return Stream.of(
				Arguments.of("?type=T&orderBy=name", "true"),
				Arguments.of("?type=T&orderBy=name&local=false", "false"),
				Arguments.of("?type=T", null),
				Arguments.of("", null));
	}
}
