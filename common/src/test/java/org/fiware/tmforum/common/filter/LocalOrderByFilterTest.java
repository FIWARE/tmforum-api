package org.fiware.tmforum.common.filter;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.filter.ClientFilterChain;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LocalOrderByFilterTest {

	private static final String ENTITIES_PATH = "/ngsi-ld/v1/entities";

	private static final String RAW_SORTED_QUERY = "?type=T&q=name%3D%3D%22a%20b%22%3Bx~%3D2024%5C-01%7Cy%3D%3D%22c%2Bd%22"
			+ "&offset=10&limit=5&count=true&orderBy=name";
	private static final String DECODED_Q = "name==\"a b\";x~=2024\\-01|y==\"c+d\"";
	private static final String Q_PARAMETER = "q";

	private final LocalOrderByFilter filter = new LocalOrderByFilter();

	@Test
	void keepsTheRawQueryOfSortedRequests() {
		ClientFilterChain chain = mock(ClientFilterChain.class);
		filter.doFilter(HttpRequest.GET(ENTITIES_PATH + RAW_SORTED_QUERY), chain);

		ArgumentCaptor<MutableHttpRequest<?>> forwarded = ArgumentCaptor.forClass(MutableHttpRequest.class);
		verify(chain).proceed(forwarded.capture());
		URI sent = forwarded.getValue().getUri();
		assertEquals(RAW_SORTED_QUERY.substring(1) + "&local=true", sent.getRawQuery(),
				"Every parameter, q included, must reach the broker encoded as sent, with only local appended.");
		assertEquals(List.of(DECODED_Q), new QueryStringDecoder(sent).parameters().get(Q_PARAMETER),
				"The broker must decode the same q.");
	}

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
