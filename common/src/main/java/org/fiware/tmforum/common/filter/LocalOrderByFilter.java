package org.fiware.tmforum.common.filter;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.ClientFilterChain;
import io.micronaut.http.filter.HttpClientFilter;
import io.micronaut.http.uri.UriBuilder;
import org.reactivestreams.Publisher;

/**
 * Adds {@code local=true} to every broker query carrying an {@code orderBy}: Orion-LD rejects {@code orderBy} without
 * it (501), and {@code EntitiesApiClient} has no such parameter. Without context-source registrations it changes nothing.
 * Orion-LD 1.9.0 then sorts only in experimental mode; otherwise it ignores the sort like Scorpio.
 */
@Filter(serviceId = "ngsi")
public class LocalOrderByFilter implements HttpClientFilter {

	static final String ORDER_BY_PARAMETER = "orderBy";
	static final String LOCAL_PARAMETER = "local";
	private static final String LOCAL_VALUE = "true";

	@Override
	public Publisher<? extends HttpResponse<?>> doFilter(MutableHttpRequest<?> request, ClientFilterChain chain) {
		if (request.getParameters().contains(ORDER_BY_PARAMETER)
				&& !request.getParameters().contains(LOCAL_PARAMETER)) {
			request.uri(UriBuilder.of(request.getUri()).queryParam(LOCAL_PARAMETER, LOCAL_VALUE).build());
		}
		return chain.proceed(request);
	}
}
