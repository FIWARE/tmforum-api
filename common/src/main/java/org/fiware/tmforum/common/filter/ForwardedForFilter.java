package org.fiware.tmforum.common.filter;

import io.micronaut.core.order.Ordered;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.uri.UriBuilder;
import org.reactivestreams.Publisher;

import java.net.URI;

/**
 * HTTP server filter that normalizes and exposes forwarding information for incoming requests.
 *
 * <p>
 * This filter reads the RFC 7239 Forwarded header as well as legacy X-Forwarded-* headers
 * (such as X-Forwarded-For, X-Forwarded-Proto, X-Forwarded-Host, X-Forwarded-Prefix) from
 * each request. It parses these headers to determine the original client IP, protocol,
 * host, port, and any path prefix applied by upstream proxies or gateways.
 * </p>
 *
 * <p>
 * The filter then computes the full original request URL and stores it, along with the parsed
 * forwarding details, as attributes in the request:
 * <ul>
 *     <li>{@link #REQ_ATTR}: the reconstructed request URI</li>
 *     <li>{@link #FORWARD_INFO_ATTR}: the {@link ForwardedInfo} object containing all parsed forwarding information</li>
 * </ul>
 * </p>
 *
 * <p>
 * Downstream controllers and filters can use these attributes to correctly generate
 * absolute URLs, redirects, or logs that reflect the original client-facing request
 * even when behind reverse proxies (e.g. {@link PaginationFilter} uses {@link #REQ_ATTR} to build
 * absolute pagination links).
 * </p>
 */
@Filter(Filter.MATCH_ALL_PATTERN)
public class ForwardedForFilter implements HttpServerFilter, Ordered {

	public static final String REQ_ATTR = "server-req";
	public static final String FORWARD_INFO_ATTR = "forwarded-info";

	private static final String HTTP_PROTOCOL = "http";
	private static final String HTTPS_PROTOCOL = "https";

	private final ForwardHeaderParser forwardHeaderParser;

	public ForwardedForFilter(ForwardHeaderParser forwardHeaderParser) {

		this.forwardHeaderParser = forwardHeaderParser;
	}

	@Override
	public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {

		ForwardedInfo forwardedInfo = forwardHeaderParser.parse(request);

		URI reqUrl = getReqUrl(forwardedInfo);

		HttpRequest<?> modifiedRequest = request
				.setAttribute(REQ_ATTR, reqUrl)
				.setAttribute(FORWARD_INFO_ATTR, forwardedInfo);

		return chain.proceed(modifiedRequest);
	}

	private URI getReqUrl(ForwardedInfo forwardedInfo) {


		String protocol = forwardedInfo.getForwardedProto();
		String host = forwardedInfo.getForwardedHost();
		int port = forwardedInfo.getForwardedPort();
		String prefix = forwardedInfo.getForwardedPrefix();

		// Ignore default ports
		Integer portToUse = null;
		if (!(HTTP_PROTOCOL.equalsIgnoreCase(protocol) && port == ForwardHeaderParser.DEFAULT_HTTP_PORT)
				&& !(HTTPS_PROTOCOL.equalsIgnoreCase(protocol) && port == ForwardHeaderParser.DEFAULT_HTTPS_PORT)) {
			portToUse = port;
		}

		UriBuilder builder = UriBuilder.of(protocol + "://" + host).path(prefix);

		if (portToUse != null) {
			builder.port(portToUse);
		}

		return builder.build();
	}

	@Override
	public int getOrder() {

		return Ordered.HIGHEST_PRECEDENCE;
	}
}
