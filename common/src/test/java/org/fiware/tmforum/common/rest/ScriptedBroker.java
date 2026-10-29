package org.fiware.tmforum.common.rest;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Singleton;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.ngsi.api.SubscriptionsApiClient;
import org.fiware.tmforum.common.notification.EventMapper;
import org.fiware.tmforum.common.repository.EntityAttributesApiClient;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * Replaces the broker clients with mocks answering per method name, so tests script the broker's failures.
 * An unscripted call fails and is recorded, so a test cannot pass on an error it did not script.
 */
@Factory
@Requires(property = ScriptedBroker.ENABLED_PROPERTY, value = "true")
public class ScriptedBroker {

	public static final String ENABLED_PROPERTY = "test.scripted-broker";
	public static final String NO_ANSWER_MESSAGE = "Connect Error: Connection refused";

	private final Map<String, Function<Object[], Mono<?>>> answers = new ConcurrentHashMap<>();
	private final List<String> unscriptedCalls = new CopyOnWriteArrayList<>();

	/** The body the broker answers an error status with; the call site interpolates it into its message. */
	public static String body(HttpStatus status) {
		return String.format("{\"title\":\"%s\"}", status.getReason());
	}

	public static <T> Mono<T> status(HttpStatus status) {
		return Mono.error(new HttpClientResponseException(status.getReason(),
				HttpResponse.status(status).body(body(status))));
	}

	public static <T> Mono<T> noAnswer() {
		return Mono.error(new HttpClientException(NO_ANSWER_MESSAGE));
	}

	public void answer(String method, Function<Object[], Mono<?>> answer) {
		answers.put(method, answer);
	}

	public void reset() {
		answers.clear();
		unscriptedCalls.clear();
	}

	public List<String> unscriptedCalls() {
		return List.copyOf(unscriptedCalls);
	}

	@Bean
	@Singleton
	@Replaces(EntitiesApiClient.class)
	EntitiesApiClient entitiesApiClient() {
		return scripted(EntitiesApiClient.class);
	}

	@Bean
	@Singleton
	@Replaces(SubscriptionsApiClient.class)
	SubscriptionsApiClient subscriptionsApiClient() {
		return scripted(SubscriptionsApiClient.class);
	}

	@Bean
	@Singleton
	@Replaces(EntityAttributesApiClient.class)
	EntityAttributesApiClient entityAttributesApiClient() {
		return scripted(EntityAttributesApiClient.class);
	}

	/** Modules provide the event mapper; the failing calls never emit an event. */
	@Bean
	@Singleton
	EventMapper eventMapper() {
		return Mockito.mock(EventMapper.class);
	}

	private <T> T scripted(Class<T> api) {
		return Mockito.mock(api, invocation -> {
			if (!Mono.class.equals(invocation.getMethod().getReturnType())) {
				return Mockito.RETURNS_DEFAULTS.answer(invocation);
			}
			String method = invocation.getMethod().getName();
			Function<Object[], Mono<?>> answer = answers.get(method);
			if (answer == null) {
				unscriptedCalls.add(method);
				return Mono.error(new IllegalStateException("Unscripted broker call " + method));
			}
			return answer.apply(invocation.getArguments());
		});
	}
}
