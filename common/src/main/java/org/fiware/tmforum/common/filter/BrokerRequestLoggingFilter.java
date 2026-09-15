package org.fiware.tmforum.common.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.http.filter.ClientFilterChain;
import io.micronaut.http.filter.HttpClientFilter;
import jakarta.inject.Singleton;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Logs every outgoing request to the NGSI-LD broker ({@code @Client(id = "ngsi")}, see
 * {@code EntitiesApiClient}/{@code SubscriptionsApiClient}) with the resolved request URL, the
 * request body (100% wire fidelity, see {@link #toWireJson}), and the outcome - success with its
 * status at DEBUG, an {@link HttpClientResponseException} with its status at WARN - plus how long
 * the call took, in a single place, without touching every repository call site.
 */
@Slf4j
@Filter(serviceId = "ngsi")
@RequiredArgsConstructor
public class BrokerRequestLoggingFilter implements HttpClientFilter {

	private final AccessLogConfiguration accessLogConfiguration;
	private final ObjectMapper objectMapper;

	@Override
	public Publisher<? extends HttpResponse<?>> doFilter(MutableHttpRequest<?> request, ClientFilterChain chain) {
		if (accessLogConfiguration.getExcludePaths().stream().anyMatch(request.getUri().getPath()::startsWith)) {
			return chain.proceed(request);
		}

		long start = System.currentTimeMillis();

		return Flux.from(chain.proceed(request))
				.doOnNext(res -> {
					long duration = System.currentTimeMillis() - start;
					String protocol = request.getHttpVersion().toString();
					String method = request.getMethod().name();
					String uri = request.getUri().toString();
					int status = res.getStatus().getCode();

					if (log.isDebugEnabled()) {
						log.debug("{} - {} - {} {} {} - {}ms", "Broker", protocol, method, uri, status,
								duration);
						var body = request.getBody();
						if (body.isPresent()) {
							log.debug("Body: {}", toWireJson(request.getBody().orElse(null)));
						}
					}
				})
				.doOnError(e -> {
					long duration = System.currentTimeMillis() - start;
					String status = e instanceof HttpStatusException hse ? Integer.toString(hse.getStatus().getCode()) : "ERROR";
					log.warn("{} - {} - {} {} {} - {}ms ", "Broker", request.getHttpVersion(), request.getMethod(),
							request.getUri(), status, duration, e);
				});
	}

	@Data
	@Singleton
	@ConfigurationProperties("http.server.log")
	public static class AccessLogConfiguration {
		private List<String> excludePaths = List.of();
	}

	/**
	 * Serialize a value exactly as it would go out on the wire, using the same (primary) {@link ObjectMapper}
	 * Micronaut's HTTP client codec uses to serialize outgoing request bodies - for diagnosing a rejected
	 * request with 100% fidelity to what the broker actually received, instead of re-deriving/re-serializing
	 * with a different mapper.
	 */
	private String toWireJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException e) {
			return String.valueOf(value);
		}
	}
}
