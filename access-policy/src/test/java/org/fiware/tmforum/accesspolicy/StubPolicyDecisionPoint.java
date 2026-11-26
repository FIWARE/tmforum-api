package org.fiware.tmforum.accesspolicy;

import com.nimbusds.jose.util.JSONObjectUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A loopback HTTP PDP answering by {@code (policy uid, subject identity)} rules, for tests that exercise the real
 * {@link PolicyDecisionPoint}. Exported in the module's test-jar; thread-safe.
 */
public final class StubPolicyDecisionPoint implements AutoCloseable {

	public static final String DEFAULT_DENY_REASON = "stub: no rule matched";
	public static final String NO_POLICY_UID_REASON = "stub: policy has no uid";

	/** Keys the policy uid is read from, first present wins: expanded JSON-LD, compacted, plain, node id. */
	static final List<String> POLICY_UID_KEYS = List.of("http://www.w3.org/ns/odrl/2/uid", "odrl:uid", "uid", "@id");

	private static final String POLICY_FIELD = "policy";
	private static final String JSON_INPUT_FIELD = "jsonInput";
	private static final String SUBJECT_FIELD = "subject";
	private static final String IDENTITY_FIELD = "identity";
	private static final String ID_FIELD = "@id";
	private static final String PERMITTED_FIELD = "permitted";
	private static final String REASON_FIELD = "reason";
	private static final String POST = "POST";
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String APPLICATION_JSON = "application/json";
	private static final int OK = 200;
	private static final int BAD_REQUEST = 400;
	private static final int METHOD_NOT_ALLOWED = 405;
	private static final int NO_BODY = -1;
	private static final int EPHEMERAL_PORT = 0;
	private static final int DEFAULT_BACKLOG = 0;
	private static final int IMMEDIATELY = 0;

	private final HttpServer server;
	private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
	private final Map<RuleKey, Answer> rules = new ConcurrentHashMap<>();
	private final List<Map<String, Object>> requests = new CopyOnWriteArrayList<>();
	private final AtomicInteger requestCount = new AtomicInteger();
	private final AtomicReference<Answer> defaultAnswer = new AtomicReference<>(denyByDefault());
	private final AtomicReference<Integer> failureStatus = new AtomicReference<>();
	private final AtomicReference<Duration> delay = new AtomicReference<>(Duration.ZERO);

	private StubPolicyDecisionPoint(HttpServer server) {
		this.server = server;
	}

	/** Starts the stub on the loopback address and an ephemeral port, serving the default PDP path. */
	public static StubPolicyDecisionPoint start() {
		return start(AccessPolicyProperties.DEFAULT_PDP_PATH);
	}

	/** Serves {@code path} instead, e.g. below the base path of a {@code micronaut.http.services.pdp.url}. */
	public static StubPolicyDecisionPoint start(String path) {
		HttpServer server;
		try {
			server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), EPHEMERAL_PORT),
					DEFAULT_BACKLOG);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		StubPolicyDecisionPoint stub = new StubPolicyDecisionPoint(server);
		server.createContext(path, stub::handle);
		// the default executor is the single dispatcher thread, which would queue every call behind a delayed one
		server.setExecutor(stub.executor);
		server.start();
		return stub;
	}

	/** The base URL, without the path, for {@code access-policy.pdp.url}. */
	public String url() {
		InetSocketAddress address = server.getAddress();
		return "http://" + address.getHostString() + ":" + address.getPort();
	}

	/** A {@code null} identity matches the anonymous subject. */
	public StubPolicyDecisionPoint permit(String policyUid, String identity) {
		rules.put(new RuleKey(policyUid, identity), new Answer(true, null));
		return this;
	}

	/** A {@code null} identity matches the anonymous subject. */
	public StubPolicyDecisionPoint deny(String policyUid, String identity, String reason) {
		rules.put(new RuleKey(policyUid, identity), new Answer(false, reason));
		return this;
	}

	/** The answer to a request no rule matches; denies with {@link #DEFAULT_DENY_REASON} until set. */
	public StubPolicyDecisionPoint defaultAnswer(boolean permitted) {
		defaultAnswer.set(permitted ? new Answer(true, null) : denyByDefault());
		return this;
	}

	/** Answers every request with {@code status} and no body, until {@link #reset()}. */
	public StubPolicyDecisionPoint failWith(int status) {
		failureStatus.set(status);
		return this;
	}

	/** Holds every answer back by {@code delay}, until {@link #reset()}. */
	public StubPolicyDecisionPoint delay(Duration delay) {
		this.delay.set(delay);
		return this;
	}

	/** Counts every request that reached the PDP path, answered or not. */
	public int requestCount() {
		return requestCount.get();
	}

	/** The parsed bodies, in arrival order; an unparsable body is not recorded. */
	public List<Map<String, Object>> requests() {
		return List.copyOf(requests);
	}

	/** Drops rules, failures, delay, recorded requests and the count. */
	public void reset() {
		rules.clear();
		requests.clear();
		requestCount.set(0);
		defaultAnswer.set(denyByDefault());
		failureStatus.set(null);
		delay.set(Duration.ZERO);
	}

	@Override
	public void close() {
		server.stop(IMMEDIATELY);
		// interrupts answers still held back by a delay
		executor.shutdownNow();
	}

	/** The uid of the request's policy, unwrapped to a string; {@code null} when there is none. */
	static String policyUid(Map<String, Object> request) {
		if (!(request.get(POLICY_FIELD) instanceof Map<?, ?> policy)) {
			return null;
		}
		return POLICY_UID_KEYS.stream()
				.filter(policy::containsKey)
				.findFirst()
				.map(key -> unwrap(policy.get(key)))
				.orElse(null);
	}

	private static String unwrap(Object value) {
		if (value instanceof String uid) {
			return uid;
		}
		if (value instanceof Map<?, ?> node) {
			return unwrap(node.get(ID_FIELD));
		}
		if (value instanceof List<?> values && !values.isEmpty()) {
			return unwrap(values.getFirst());
		}
		return null;
	}

	private static String identity(Map<String, Object> request) {
		if (request.get(JSON_INPUT_FIELD) instanceof Map<?, ?> input
				&& input.get(SUBJECT_FIELD) instanceof Map<?, ?> subject
				&& subject.get(IDENTITY_FIELD) instanceof String identity) {
			return identity;
		}
		return null;
	}

	private void handle(HttpExchange exchange) throws IOException {
		try (exchange) {
			requestCount.incrementAndGet();
			if (!POST.equals(exchange.getRequestMethod())) {
				exchange.sendResponseHeaders(METHOD_NOT_ALLOWED, NO_BODY);
				return;
			}
			Map<String, Object> request;
			try {
				request = JSONObjectUtils.parse(
						new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			} catch (ParseException e) {
				exchange.sendResponseHeaders(BAD_REQUEST, NO_BODY);
				return;
			}
			requests.add(request);
			pause();
			Integer status = failureStatus.get();
			if (status != null) {
				exchange.sendResponseHeaders(status, NO_BODY);
				return;
			}
			respond(exchange, answer(request));
		}
	}

	private Answer answer(Map<String, Object> request) {
		String policyUid = policyUid(request);
		if (policyUid == null) {
			return new Answer(false, NO_POLICY_UID_REASON);
		}
		return rules.getOrDefault(new RuleKey(policyUid, identity(request)), defaultAnswer.get());
	}

	private void pause() {
		try {
			Thread.sleep(delay.get());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private static void respond(HttpExchange exchange, Answer answer) throws IOException {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put(PERMITTED_FIELD, answer.permitted());
		if (answer.reason() != null) {
			body.put(REASON_FIELD, answer.reason());
		}
		byte[] bytes = JSONObjectUtils.toJSONString(body).getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add(CONTENT_TYPE, APPLICATION_JSON);
		exchange.sendResponseHeaders(OK, bytes.length);
		try (OutputStream out = exchange.getResponseBody()) {
			out.write(bytes);
		}
	}

	private static Answer denyByDefault() {
		return new Answer(false, DEFAULT_DENY_REASON);
	}

	private record RuleKey(String policyUid, String identity) {
	}

	private record Answer(boolean permitted, String reason) {
	}
}
