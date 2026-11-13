package org.fiware.tmforum.productcatalog;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.ClientFilterChain;
import io.micronaut.http.filter.HttpClientFilter;
import org.reactivestreams.Publisher;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Records the URI of every request to the NGSI-LD broker, so an IT can assert which entities a call fetched.
 * Only active when {@link #ENABLED_PROPERTY} is set.
 */
@Requires(property = BrokerRequestRecorder.ENABLED_PROPERTY, value = "true")
@Filter(serviceId = "ngsi")
public class BrokerRequestRecorder implements HttpClientFilter {

	static final String ENABLED_PROPERTY = "test.broker-request-recorder.enabled";

	private final List<URI> requests = new CopyOnWriteArrayList<>();

	@Override
	public Publisher<? extends HttpResponse<?>> doFilter(MutableHttpRequest<?> request, ClientFilterChain chain) {
		requests.add(request.getUri());
		return chain.proceed(request);
	}

	/** The broker requests recorded since the last {@link #clear()}, in order. */
	public List<URI> requests() {
		return List.copyOf(requests);
	}

	public void clear() {
		requests.clear();
	}
}
