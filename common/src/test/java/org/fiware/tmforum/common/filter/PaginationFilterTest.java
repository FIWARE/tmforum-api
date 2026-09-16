package org.fiware.tmforum.common.filter;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaginationFilterTest {

	private static final URI BASE_URI = URI.create("https://example.com");

	/** Number of entities matching the query, only known if the broker reports it. */
	private static final String TOTAL_COUNT_HEADER = "X-Total-Count";

	/** Number of entities contained in the response's payload. */
	private static final String RESULT_COUNT_HEADER = "X-Result-Count";

	private final PaginationFilter filter = new PaginationFilter();

	@Test
	public void fullPageInTheMiddleHasAllFourLinks() {
		HttpRequest<?> request = HttpRequest.GET("/resource?fields=name&offset=10&limit=10");

		List<String> entries = PaginationFilter.buildLinkEntries(request, BASE_URI, 10, 10, 10, null);

		assertEquals(4, entries.size(), "self/first/prev/next should all be present.");
		assertTrue(entries.get(0).contains("offset=10") && entries.get(0).contains("rel=\"self\""));
		assertTrue(entries.get(1).contains("offset=0") && entries.get(1).contains("rel=\"first\""));
		assertTrue(entries.get(2).contains("offset=0") && entries.get(2).contains("rel=\"prev\""));
		assertTrue(entries.get(3).contains("offset=20") && entries.get(3).contains("rel=\"next\""));
		entries.forEach(entry -> assertTrue(entry.contains("fields=name"), "Other query params must survive: " + entry));
		entries.forEach(entry -> assertTrue(entry.startsWith("<https://example.com/resource?"), "Must use the forwarded base URI: " + entry));
	}

	@Test
	public void firstPageOmitsPrev() {
		HttpRequest<?> request = HttpRequest.GET("/resource?offset=0&limit=10");

		List<String> entries = PaginationFilter.buildLinkEntries(request, BASE_URI, 0, 10, 10, null);

		assertEquals(3, entries.size(), "prev should be omitted on the first page.");
		assertTrue(entries.stream().noneMatch(e -> e.contains("rel=\"prev\"")));
	}

	@Test
	public void partialPageOmitsNext() {
		HttpRequest<?> request = HttpRequest.GET("/resource?offset=10&limit=10");

		List<String> entries = PaginationFilter.buildLinkEntries(request, BASE_URI, 10, 10, 5, null);

		assertFalse(entries.stream().anyMatch(e -> e.contains("rel=\"next\"")),
				"A page returning fewer items than the limit is assumed to be the last one.");
	}

	@Test
	public void prevIsClampedToZero() {
		HttpRequest<?> request = HttpRequest.GET("/resource?offset=5&limit=10");

		List<String> entries = PaginationFilter.buildLinkEntries(request, BASE_URI, 5, 10, 10, null);

		String prev = entries.stream().filter(e -> e.contains("rel=\"prev\"")).findFirst().orElseThrow();
		assertTrue(prev.contains("offset=0"), "prev must not go negative: " + prev);
	}

	@Test
	public void baseUriPrefixIsPreserved() {
		HttpRequest<?> request = HttpRequest.GET("/resource?offset=0&limit=10");
		URI prefixedBaseUri = URI.create("https://example.com/api/v1");

		List<String> entries = PaginationFilter.buildLinkEntries(request, prefixedBaseUri, 0, 10, 10, null);

		entries.forEach(entry -> assertTrue(entry.contains("https://example.com/api/v1/resource"),
				"The forwarded prefix must be kept ahead of the resource path: " + entry));
	}

	@Test
	public void withTotalCountNextIsExactRatherThanHeuristic() {
		HttpRequest<?> request = HttpRequest.GET("/resource?offset=10&limit=10");

		// returnedCount == limit, so the heuristic would say "next", but total says this is the last page.
		List<String> exactlyAtTotal = PaginationFilter.buildLinkEntries(request, BASE_URI, 10, 10, 10, 20);
		assertFalse(exactlyAtTotal.stream().anyMatch(e -> e.contains("rel=\"next\"")),
				"offset+limit == total means there is no next page, even though the page came back full.");

		List<String> beforeTotal = PaginationFilter.buildLinkEntries(request, BASE_URI, 10, 10, 10, 25);
		assertTrue(beforeTotal.stream().anyMatch(e -> e.contains("rel=\"next\"") && e.contains("offset=20")));
	}

	@Test
	public void lastLinkIsOnlyAddedWithATotalCount() {
		HttpRequest<?> request = HttpRequest.GET("/resource?offset=0&limit=10");

		List<String> withoutTotal = PaginationFilter.buildLinkEntries(request, BASE_URI, 0, 10, 10, null);
		assertFalse(withoutTotal.stream().anyMatch(e -> e.contains("rel=\"last\"")),
				"last cannot be computed without a total count.");

		// 25 items, limit 10 -> pages at offset 0, 10, 20 -> last page starts at offset 20.
		List<String> withTotal = PaginationFilter.buildLinkEntries(request, BASE_URI, 0, 10, 10, 25);
		String last = withTotal.stream().filter(e -> e.contains("rel=\"last\"")).findFirst().orElseThrow();
		assertTrue(last.contains("offset=20"), "last must point at the final page: " + last);
	}

	@Test
	public void lastLinkWithZeroTotalPointsAtOffsetZero() {
		HttpRequest<?> request = HttpRequest.GET("/resource?offset=0&limit=10");

		List<String> entries = PaginationFilter.buildLinkEntries(request, BASE_URI, 0, 10, 0, 0);

		String last = entries.stream().filter(e -> e.contains("rel=\"last\"")).findFirst().orElseThrow();
		assertTrue(last.contains("offset=0"), "An empty result set's last page is offset 0: " + last);
	}

	@Test
	public void isPartialOnlyWhenTotalCountIsKnownAndGreaterThanReturned() {
		assertFalse(PaginationFilter.isPartial(10, null), "Unknown total must never claim partial.");
		assertFalse(PaginationFilter.isPartial(10, 10), "A full result set is not partial.");
		assertTrue(PaginationFilter.isPartial(10, 25), "Fewer items returned than the total is partial.");
	}

	@Test
	public void addPaginationLinkSetsTotalCountHeaderAndPartialStatus() {
		HttpRequest<?> request = HttpRequest.GET("/resource?offset=0&limit=10")
				.setAttribute(PaginationFilter.OFFSET_ATTR, 0)
				.setAttribute(PaginationFilter.LIMIT_ATTR, 10)
				.setAttribute(PaginationFilter.RETURNED_COUNT_ATTR, 10)
				.setAttribute(PaginationFilter.TOTAL_COUNT_ATTR, 25);
		MutableHttpResponse<Object> response = HttpResponse.ok();

		filter.addPaginationLink(request, response);

		assertEquals("25", response.getHeaders().get(TOTAL_COUNT_HEADER));
		assertEquals(HttpStatus.PARTIAL_CONTENT, response.getStatus());
	}

	@Test
	public void addPaginationLinkKeeps200WhenResultIsComplete() {
		HttpRequest<?> request = HttpRequest.GET("/resource?offset=0&limit=10")
				.setAttribute(PaginationFilter.OFFSET_ATTR, 0)
				.setAttribute(PaginationFilter.LIMIT_ATTR, 10)
				.setAttribute(PaginationFilter.RETURNED_COUNT_ATTR, 10)
				.setAttribute(PaginationFilter.TOTAL_COUNT_ATTR, 10);
		MutableHttpResponse<Object> response = HttpResponse.ok();

		filter.addPaginationLink(request, response);

		assertEquals(HttpStatus.OK, response.getStatus());
	}

	@Test
	public void addPaginationLinkOmitsTotalCountHeaderWhenNotConfigured() {
		HttpRequest<?> request = HttpRequest.GET("/resource?offset=0&limit=10")
				.setAttribute(PaginationFilter.OFFSET_ATTR, 0)
				.setAttribute(PaginationFilter.LIMIT_ATTR, 10)
				.setAttribute(PaginationFilter.RETURNED_COUNT_ATTR, 10);
		MutableHttpResponse<Object> response = HttpResponse.ok();

		filter.addPaginationLink(request, response);

		assertNull(response.getHeaders().get(TOTAL_COUNT_HEADER));
		assertEquals(HttpStatus.OK, response.getStatus());
	}

	/**
	 * The returned count is always known once a list endpoint ran, so - unlike the total - the header
	 * has to be set no matter whether the broker reported a total and no matter how full the page is.
	 *
	 * @param message       description of the concrete case
	 * @param returnedCount number of entities in the payload
	 * @param totalCount    number of matching entities, {@code null} if the broker didn't report one
	 */
	@ParameterizedTest
	@MethodSource("providePageSizes")
	public void addPaginationLinkSetsResultCountHeader(String message, int returnedCount, Integer totalCount) {
		HttpRequest<?> request = HttpRequest.GET("/resource?offset=0&limit=10")
				.setAttribute(PaginationFilter.OFFSET_ATTR, 0)
				.setAttribute(PaginationFilter.LIMIT_ATTR, 10)
				.setAttribute(PaginationFilter.RETURNED_COUNT_ATTR, returnedCount);
		if (totalCount != null) {
			request.setAttribute(PaginationFilter.TOTAL_COUNT_ATTR, totalCount);
		}
		MutableHttpResponse<Object> response = HttpResponse.ok();

		filter.addPaginationLink(request, response);

		assertEquals(String.valueOf(returnedCount), response.getHeaders().get(RESULT_COUNT_HEADER), message);
	}

	private static Stream<Arguments> providePageSizes() {
		return Stream.of(
				Arguments.of("A full page without a total should report its own size.", 10, null),
				Arguments.of("A partial page without a total should report its own size.", 4, null),
				Arguments.of("An empty page without a total should report zero.", 0, null),
				Arguments.of("A page of a larger result set should report the page's size, not the total.", 10, 25),
				Arguments.of("A complete result set should report its size.", 10, 10),
				Arguments.of("An empty result set should report zero.", 0, 0));
	}

	@Test
	public void addPaginationLinkSetsNoHeadersOnNonListEndpoints() {
		// create/patch/delete/get-by-id never set the pagination attributes - the filter must not
		// claim a result count for a response that isn't a list.
		HttpRequest<?> request = HttpRequest.GET("/resource/urn:ngsi-ld:resource:my-resource");
		MutableHttpResponse<Object> response = HttpResponse.ok();

		filter.addPaginationLink(request, response);

		assertNull(response.getHeaders().get(RESULT_COUNT_HEADER));
		assertNull(response.getHeaders().get(TOTAL_COUNT_HEADER));
	}
}
