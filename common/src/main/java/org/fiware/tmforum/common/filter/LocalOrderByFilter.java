package org.fiware.tmforum.common.filter;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.ClientFilterChain;
import io.micronaut.http.filter.HttpClientFilter;
import org.reactivestreams.Publisher;

import java.net.URI;

/**
 * Adds {@code local=true} to every broker query carrying an {@code orderBy}: Orion-LD rejects {@code orderBy} without
 * it (501), and {@code EntitiesApiClient} has no such parameter. Without context-source registrations it changes nothing.
 * Orion-LD 1.9.0 then ignores the sort like Scorpio; experimental mode was not tested, and per its docs sorts on one
 * key only, with the direction from {@code reverse=true}, not {@code ;desc}.
 * The parameter is appended to the raw query, so the encoding of {@code q} and every other parameter is kept as sent.
 */
@Filter(serviceId = "ngsi")
public class LocalOrderByFilter implements HttpClientFilter {

	static final String ORDER_BY_PARAMETER = "orderBy";
	static final String LOCAL_PARAMETER = "local";
	private static final String LOCAL_QUERY = LOCAL_PARAMETER + "=true";

	@Override
	public Publisher<? extends HttpResponse<?>> doFilter(MutableHttpRequest<?> request, ClientFilterChain chain) {
		if (request.getParameters().contains(ORDER_BY_PARAMETER)
				&& !request.getParameters().contains(LOCAL_PARAMETER)) {
			URI uri = request.getUri();
			String separator = uri.getRawQuery() == null ? "?" : "&";
			request.uri(URI.create(uri + separator + LOCAL_QUERY));
		}
		return chain.proceed(request);
	}
}
