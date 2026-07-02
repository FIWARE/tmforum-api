package org.fiware.tmforum.common.filter;

import io.micronaut.http.HttpRequest;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaginationFilterTest {

	private static final URI BASE_URI = URI.create("https://example.com");

	@Test
	public void fullPageInTheMiddleHasAllFourLinks() {
		HttpRequest<?> request = HttpRequest.GET("/resource?fields=name&offset=10&limit=10");

		List<String> entries = PaginationFilter.buildLinkEntries(request, BASE_URI, 10, 10, 10);

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

		List<String> entries = PaginationFilter.buildLinkEntries(request, BASE_URI, 0, 10, 10);

		assertEquals(3, entries.size(), "prev should be omitted on the first page.");
		assertTrue(entries.stream().noneMatch(e -> e.contains("rel=\"prev\"")));
	}

	@Test
	public void partialPageOmitsNext() {
		HttpRequest<?> request = HttpRequest.GET("/resource?offset=10&limit=10");

		List<String> entries = PaginationFilter.buildLinkEntries(request, BASE_URI, 10, 10, 5);

		assertFalse(entries.stream().anyMatch(e -> e.contains("rel=\"next\"")),
				"A page returning fewer items than the limit is assumed to be the last one.");
	}

	@Test
	public void prevIsClampedToZero() {
		HttpRequest<?> request = HttpRequest.GET("/resource?offset=5&limit=10");

		List<String> entries = PaginationFilter.buildLinkEntries(request, BASE_URI, 5, 10, 10);

		String prev = entries.stream().filter(e -> e.contains("rel=\"prev\"")).findFirst().orElseThrow();
		assertTrue(prev.contains("offset=0"), "prev must not go negative: " + prev);
	}

	@Test
	public void baseUriPrefixIsPreserved() {
		HttpRequest<?> request = HttpRequest.GET("/resource?offset=0&limit=10");
		URI prefixedBaseUri = URI.create("https://example.com/api/v1");

		List<String> entries = PaginationFilter.buildLinkEntries(request, prefixedBaseUri, 0, 10, 10);

		entries.forEach(entry -> assertTrue(entry.contains("https://example.com/api/v1/resource"),
				"The forwarded prefix must be kept ahead of the resource path: " + entry));
	}
}
