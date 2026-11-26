package org.fiware.tmforum.accesspolicy;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.json.JsonMapper;
import io.micronaut.json.tree.JsonNode;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Asks the external PDP for its verdict on one policy. An absolute {@code pdp.url} is used as is, otherwise the
 * relative {@code pdp.path} resolves against the {@code micronaut.http.services.pdp} service.
 */
@Slf4j
@Singleton
@Requires(property = AccessPolicyProperties.ENABLED_PROPERTY, value = "true")
public class PolicyDecisionPoint {

	static final String SERVICE_ID = "pdp";
	static final String SERVICE_PREFIX = "micronaut.http.services." + SERVICE_ID;

	static final String TIMED_OUT = "PDP timed out after %s";
	static final String ANSWERED = "PDP answered %d";
	static final String UNPARSABLE = "PDP answer unparsable";
	static final String NO_ANSWER = "PDP answered nothing";
	static final String UNREACHABLE = "PDP unreachable: %s";

	private static final String PERMITTED_FIELD = "permitted";
	private static final String REASON_FIELD = "reason";

	private final AccessPolicyProperties properties;
	private final HttpClient httpClient;
	private final JsonMapper jsonMapper;
	private final String endpoint;

	@Inject
	public PolicyDecisionPoint(AccessPolicyProperties properties, @Client(id = SERVICE_ID) HttpClient httpClient,
			JsonMapper jsonMapper) {
		this.properties = properties;
		this.httpClient = httpClient;
		this.jsonMapper = jsonMapper;
		this.endpoint = endpoint(properties.getPdp());
	}

	/**
	 * Never errors or completes empty: a timeout, a non-200 answer or an unparsable body is a
	 * {@link AccessDecision.Source#PDP_FAILURE} decision that permits only under {@code fail-open}.
	 */
	public Mono<AccessDecision> decide(String policyUid, ValidationRequest request) {
		Duration timeout = properties.getPdp().getTimeout();
		HttpRequest<ValidationRequest> pdpRequest = HttpRequest.POST(endpoint, request)
				.contentType(MediaType.APPLICATION_JSON_TYPE);
		return Mono.from(httpClient.exchange(pdpRequest, String.class))
				// bounds the call whatever the HTTP client's own timeouts are
				.timeout(timeout)
				.map(response -> verdict(policyUid, response))
				.switchIfEmpty(Mono.error(() -> new UnusableAnswer(NO_ANSWER)))
				.onErrorResume(error -> Mono.just(failure(policyUid, reasonOf(error, timeout))));
	}

	private AccessDecision verdict(String policyUid, HttpResponse<String> response) {
		if (response.code() != HttpStatus.OK.getCode()) {
			throw new UnusableAnswer(String.format(ANSWERED, response.code()));
		}
		PdpResponse answer = parse(response.getBody().orElse(""));
		return AccessDecision.policy(answer.permitted(), answer.reason(), policyUid);
	}

	// read as a tree, since binding would coerce a "true" string and default a missing permitted to a policy denial
	private PdpResponse parse(String body) {
		JsonNode tree;
		try {
			tree = jsonMapper.readValue(body, JsonNode.class);
		} catch (IOException | RuntimeException e) {
			throw new UnusableAnswer(UNPARSABLE);
		}
		JsonNode permitted = tree != null && tree.isObject() ? tree.get(PERMITTED_FIELD) : null;
		if (permitted == null || !permitted.isBoolean()) {
			throw new UnusableAnswer(UNPARSABLE);
		}
		JsonNode reason = tree.get(REASON_FIELD);
		return new PdpResponse(permitted.getBooleanValue(),
				reason != null && reason.isString() ? reason.getStringValue() : null);
	}

	private AccessDecision failure(String policyUid, String reason) {
		log.warn("The PDP call for policy {} failed: {}", policyUid, reason);
		return AccessDecision.failure(policyUid, reason, properties.isFailOpen());
	}

	// never the body, the subject or its claims: the reason ends up in logs and audit records
	private static String reasonOf(Throwable error, Duration timeout) {
		if (error instanceof UnusableAnswer unusable) {
			return unusable.getMessage();
		}
		if (error instanceof TimeoutException) {
			return String.format(TIMED_OUT, timeout);
		}
		if (error instanceof HttpClientResponseException response) {
			return String.format(ANSWERED, response.code());
		}
		return String.format(UNREACHABLE, error.getClass().getName());
	}

	private static String endpoint(AccessPolicyProperties.Pdp pdp) {
		String url = pdp.getUrl();
		if (url == null || url.isBlank()) {
			return pdp.getPath();
		}
		return UriBuilder.of(url).path(pdp.getPath()).build().toString();
	}

	private record PdpResponse(boolean permitted, String reason) {
	}

	private static final class UnusableAnswer extends RuntimeException {
		UnusableAnswer(String reason) {
			super(reason, null, false, false);
		}
	}
}
