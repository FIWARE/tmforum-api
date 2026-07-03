package org.fiware.tmforum.common.filter;

import io.micronaut.core.order.Ordered;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.uri.UriBuilder;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adds a TMF630-style {@code Link} header (self/first/prev/next) to responses produced by
 * {@code AbstractApiController#list}, which stashes the resolved
 * {@code offset}/{@code limit}/returned-count as request attributes for this filter to read - a
 * request-attribute handoff, not a change to {@code list()}'s return type, so none of the ~44
 * controllers calling it need to change.
 *
 * <p>
 * Absolute URLs are built from {@link ForwardedForFilter#REQ_ATTR} so links reflect the
 * client-facing scheme/host/port/prefix when this service sits behind a load balancer, rather
 * than the internal address this server sees itself as.
 * </p>
 *
 * <p>
 * {@code X-Total-Count} and the {@code last} link are only added when
 * {@link org.fiware.tmforum.common.configuration.GeneralProperties#getCountHeader()} is configured
 * for the active broker profile and the broker actually sent that header (see
 * {@link org.fiware.tmforum.common.repository.TmForumRepository#findEntities}). Without a total,
 * {@code next} is inferred from whether the page came back full ({@code returnedCount == limit}) -
 * a reasonable heuristic that can produce a false positive exactly on the last page; with a total,
 * both {@code next} and the response status ({@code 206 Partial Content} vs {@code 200 OK}, per
 * TMF630) are exact.
 * </p>
 */
@Filter(Filter.MATCH_ALL_PATTERN)
public class PaginationFilter implements HttpServerFilter, Ordered {

	public static final String OFFSET_ATTR = "pagination-offset";
	public static final String LIMIT_ATTR = "pagination-limit";
	public static final String RETURNED_COUNT_ATTR = "pagination-returned-count";
	public static final String TOTAL_COUNT_ATTR = "pagination-total-count";

	private static final String OFFSET_PARAM = "offset";
	private static final String LIMIT_PARAM = "limit";
	private static final String TOTAL_COUNT_HEADER = "X-Total-Count";

	@Override
	public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
		return Flux.from(chain.proceed(request))
				.doOnNext(response -> addPaginationLink(request, response));
	}

	void addPaginationLink(HttpRequest<?> request, MutableHttpResponse<?> response) {
		Optional<Integer> offsetAttr = request.getAttribute(OFFSET_ATTR, Integer.class);
		Optional<Integer> limitAttr = request.getAttribute(LIMIT_ATTR, Integer.class);
		Optional<Integer> returnedCountAttr = request.getAttribute(RETURNED_COUNT_ATTR, Integer.class);
		if (offsetAttr.isEmpty() || limitAttr.isEmpty() || returnedCountAttr.isEmpty()) {
			// not a paginated list endpoint (create/patch/delete/get-by-id never set these) - no-op.
			return;
		}
		int offset = offsetAttr.get();
		int limit = limitAttr.get();
		int returnedCount = returnedCountAttr.get();
		Integer totalCount = request.getAttribute(TOTAL_COUNT_ATTR, Integer.class).orElse(null);

		URI baseUri = (URI) request.getAttribute(ForwardedForFilter.REQ_ATTR).orElse(URI.create(""));
		List<String> linkEntries = buildLinkEntries(request, baseUri, offset, limit, returnedCount, totalCount);
		response.header(HttpHeaders.LINK, String.join(", ", linkEntries));

		if (totalCount != null) {
			response.header(TOTAL_COUNT_HEADER, String.valueOf(totalCount));
			if (isPartial(returnedCount, totalCount)) {
				response.status(HttpStatus.PARTIAL_CONTENT);
			}
		}
	}

	/**
	 * True per TMF630 iff the response is genuinely a subset of the matching entities - only
	 * decidable once the total is known, otherwise the status always stays 200 OK.
	 */
	static boolean isPartial(int returnedCount, Integer totalCount) {
		return totalCount != null && returnedCount < totalCount;
	}

	/**
	 * {@code totalCount} is null when the broker profile has no count header configured, or the
	 * broker didn't send a parsable value for it - in that case {@code next} falls back to the
	 * returned-count-equals-limit heuristic and {@code last} is omitted entirely.
	 */
	static List<String> buildLinkEntries(HttpRequest<?> request, URI baseUri, int offset, int limit, int returnedCount, Integer totalCount) {
		List<String> entries = new ArrayList<>();
		entries.add(buildLink(request, baseUri, offset, limit, "self"));
		entries.add(buildLink(request, baseUri, 0, limit, "first"));
		if (offset > 0) {
			entries.add(buildLink(request, baseUri, Math.max(0, offset - limit), limit, "prev"));
		}
		boolean hasNext = totalCount != null ? (offset + limit < totalCount) : (returnedCount == limit);
		if (hasNext) {
			entries.add(buildLink(request, baseUri, offset + limit, limit, "next"));
		}
		if (totalCount != null) {
			int lastOffset = totalCount > 0 ? ((totalCount - 1) / limit) * limit : 0;
			entries.add(buildLink(request, baseUri, lastOffset, limit, "last"));
		}
		return entries;
	}

	/**
	 * Rebuilds the current request's path and query string against {@code baseUri} (so the link
	 * reflects the client-facing host behind a load balancer), preserving every query parameter
	 * except {@code offset}/{@code limit}, which are overridden with the given values.
	 */
	private static String buildLink(HttpRequest<?> request, URI baseUri, int offset, int limit, String rel) {
		Map<String, List<String>> remainingParams = new LinkedHashMap<>(request.getParameters().asMap());
		remainingParams.remove(OFFSET_PARAM);
		remainingParams.remove(LIMIT_PARAM);

		UriBuilder builder = UriBuilder.of(baseUri).path(request.getPath());
		remainingParams.forEach((key, values) -> values.forEach(value -> builder.queryParam(key, value)));
		builder.queryParam(OFFSET_PARAM, offset);
		builder.queryParam(LIMIT_PARAM, limit);

		return String.format("<%s>; rel=\"%s\"", builder.build(), rel);
	}

	@Override
	public int getOrder() {
		// Must run after ForwardedForFilter (HIGHEST_PRECEDENCE) has populated REQ_ATTR before
		// this filter's own request-phase code (the part of doFilter before chain.proceed) runs.
		return Ordered.LOWEST_PRECEDENCE;
	}
}
