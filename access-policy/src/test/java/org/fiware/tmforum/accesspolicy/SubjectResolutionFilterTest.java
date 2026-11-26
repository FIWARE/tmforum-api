package org.fiware.tmforum.accesspolicy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import io.micronaut.core.order.Ordered;
import io.micronaut.core.propagation.PropagatedContext;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.context.ServerHttpRequestContext;
import io.micronaut.http.filter.ServerFilterChain;
import org.fiware.tmforum.accesspolicy.TestTokens.StaticJwkSetProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.fiware.tmforum.accesspolicy.TestTokens.AUDIENCE;
import static org.fiware.tmforum.accesspolicy.TestTokens.CLOCK;
import static org.fiware.tmforum.accesspolicy.TestTokens.EXPIRY;
import static org.fiware.tmforum.accesspolicy.TestTokens.FOREIGN_RSA_KEY;
import static org.fiware.tmforum.accesspolicy.TestTokens.NOW;
import static org.fiware.tmforum.accesspolicy.TestTokens.RSA_KEY;
import static org.fiware.tmforum.accesspolicy.TestTokens.SUBJECT_CLAIM_VALUE;
import static org.fiware.tmforum.accesspolicy.TestTokens.VC_ISSUER;
import static org.fiware.tmforum.accesspolicy.TestTokens.VERIFIABLE_CREDENTIAL;
import static org.fiware.tmforum.accesspolicy.TestTokens.claims;
import static org.fiware.tmforum.accesspolicy.TestTokens.properties;
import static org.fiware.tmforum.accesspolicy.TestTokens.publicSet;
import static org.fiware.tmforum.accesspolicy.TestTokens.sign;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests that every request proceeds exactly once, carrying the token's subject or anonymous, and that no token leaks
 * into the logs.
 */
class SubjectResolutionFilterTest {

	private static final Duration VERIFY_TIMEOUT = Duration.ofSeconds(5);
	private static final String PACKAGE_LOGGER = "org.fiware.tmforum.accesspolicy";
	private static final String TOKEN_SEGMENT_SEPARATOR = ".";

	private static final String VALID_TOKEN = sign(RSA_KEY, claims().build());
	private static final Subject VALID_SUBJECT = Subject.of(VC_ISSUER,
			Map.of("verifiableCredential", VERIFIABLE_CREDENTIAL), EXPIRY);

	private static SubjectResolutionFilter filter(AccessPolicyProperties properties) {
		return new SubjectResolutionFilter(
				new BearerTokenValidator(properties, new StaticJwkSetProvider(publicSet(RSA_KEY)), CLOCK));
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}

	/** Header-level defects, each with the token it carries ({@code null} if none) and the expected subject. */
	private static Stream<Arguments> authorizationHeaders() {
		return Stream.of(
				Arguments.of("no header", null, null, Subject.ANONYMOUS),
				Arguments.of("basic scheme", "Basic dXNlcjpwYXNzd29yZA==", null, Subject.ANONYMOUS),
				Arguments.of("bearer without token", "Bearer", null, Subject.ANONYMOUS),
				Arguments.of("bearer with blank token", "Bearer   ", null, Subject.ANONYMOUS),
				Arguments.of("scheme glued to the token", "Bearer" + VALID_TOKEN, VALID_TOKEN, Subject.ANONYMOUS),
				Arguments.of("valid", bearer(VALID_TOKEN), VALID_TOKEN, VALID_SUBJECT),
				Arguments.of("lowercase scheme", "bearer " + VALID_TOKEN, VALID_TOKEN, VALID_SUBJECT),
				Arguments.of("malformed", bearer("not-a-jwt"), "not-a-jwt", Subject.ANONYMOUS),
				defect("wrong signature", sign(FOREIGN_RSA_KEY, claims().build())),
				defect("expired", sign(RSA_KEY, claims()
						.expirationTime(Date.from(NOW.minus(TestTokens.TOKEN_LIFETIME))).build())),
				defect("wrong issuer", sign(RSA_KEY, claims().issuer("https://other.example.org").build())),
				defect("wrong audience", sign(RSA_KEY, claims().audience("other-audience").build())),
				defect("unknown kid", sign(RSA_KEY, "unknown-key", claims().build())));
	}

	private static Arguments defect(String name, String token) {
		return Arguments.of(name, bearer(token), token, Subject.ANONYMOUS);
	}

	private static MutableHttpRequest<?> request(String authorization) {
		MutableHttpRequest<?> request = HttpRequest.GET("/productOffering");
		if (authorization != null) {
			request.header(HttpHeaders.AUTHORIZATION, authorization);
		}
		return request;
	}

	/** Runs the filter and returns the single request the chain proceeded with. */
	private static HttpRequest<?> proceededRequest(SubjectResolutionFilter filter, HttpRequest<?> request) {
		ServerFilterChain chain = mock(ServerFilterChain.class);
		when(chain.proceed(any())).thenReturn(Mono.just(HttpResponse.ok()));

		StepVerifier.create(Mono.from(filter.doFilter(request, chain)))
				.expectNextCount(1)
				.expectComplete()
				.verify(VERIFY_TIMEOUT);

		ArgumentCaptor<HttpRequest<?>> proceeded = ArgumentCaptor.forClass(HttpRequest.class);
		verify(chain, times(1)).proceed(proceeded.capture());
		return proceeded.getValue();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("authorizationHeaders")
	void proceedsOnceWithTheResolvedSubject(String name, String authorization, String token, Subject expected) {
		HttpRequest<?> proceeded = proceededRequest(filter(properties()), request(authorization));

		assertEquals(Optional.of(expected), SubjectResolutionFilter.subjectOf(proceeded),
				"The request must proceed with the token's subject, or anonymous for any defect.");
	}

	private static Stream<Arguments> identityClaims() {
		return Stream.of(
				Arguments.of(AccessPolicyProperties.DEFAULT_IDENTITY_CLAIM, VC_ISSUER),
				Arguments.of("sub", SUBJECT_CLAIM_VALUE));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("identityClaims")
	void validTokenYieldsTheConfiguredIdentityAndEveryCredentialClaim(String identityClaim, String identity) {
		HttpRequest<?> proceeded = proceededRequest(filter(properties(AUDIENCE, identityClaim)),
				request(bearer(VALID_TOKEN)));

		assertEquals(Optional.of(Subject.of(identity, Map.of("verifiableCredential", VERIFIABLE_CREDENTIAL), EXPIRY)),
				SubjectResolutionFilter.subjectOf(proceeded));
	}

	@Test
	void subjectIsUnresolvedBeforeTheFilterRan() {
		assertEquals(Optional.empty(), SubjectResolutionFilter.subjectOf(request(bearer(VALID_TOKEN))),
				"An unfiltered request must report no subject, which differs from anonymous.");
	}

	@Test
	void currentReadsTheSubjectOfTheRequestInContext() {
		HttpRequest<?> proceeded = proceededRequest(filter(properties()), request(bearer(VALID_TOKEN)));

		assertEquals(Optional.empty(), SubjectResolutionFilter.current(), "Outside a request nothing is resolved.");
		try (PropagatedContext.Scope ignored = PropagatedContext.getOrEmpty()
				.plus(new ServerHttpRequestContext(proceeded)).propagate()) {
			assertEquals(Optional.of(VALID_SUBJECT), SubjectResolutionFilter.current());
		}
	}

	@Test
	void runsBetweenTheForwardingAndThePaginationFilter() {
		int order = filter(properties()).getOrder();

		assertTrue(Ordered.HIGHEST_PRECEDENCE < order && order < Ordered.LOWEST_PRECEDENCE,
				"The filter must run after ForwardedForFilter and before PaginationFilter.");
	}

	@Test
	void neverLogsTheToken() {
		Logger logger = (Logger) LoggerFactory.getLogger(PACKAGE_LOGGER);
		Level previousLevel = logger.getLevel();
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		logger.setLevel(Level.TRACE);
		List<String> tokens = new ArrayList<>();
		try {
			authorizationHeaders().map(Arguments::get).forEach(arguments -> {
				proceededRequest(filter(properties()), request((String) arguments[1]));
				if (arguments[2] != null) {
					tokens.add((String) arguments[2]);
				}
			});
		} finally {
			logger.detachAppender(appender);
			logger.setLevel(previousLevel);
		}

		assertFalse(appender.list.isEmpty(), "The rejections must be logged, else this test proves nothing.");
		List<String> logged = appender.list.stream().flatMap(SubjectResolutionFilterTest::loggedTexts).toList();
		for (String token : tokens) {
			String signature = token.substring(token.lastIndexOf(TOKEN_SEGMENT_SEPARATOR) + 1);
			for (String text : logged) {
				assertFalse(text.contains(token) || text.contains(signature),
						String.format("A log entry contains a token: %s", text));
			}
		}
	}

	private static Stream<String> loggedTexts(ILoggingEvent event) {
		List<String> texts = new ArrayList<>();
		texts.add(event.getFormattedMessage());
		Optional.ofNullable(event.getArgumentArray()).stream().flatMap(Arrays::stream)
				.map(String::valueOf).forEach(texts::add);
		for (IThrowableProxy thrown = event.getThrowableProxy(); thrown != null; thrown = thrown.getCause()) {
			texts.add(String.valueOf(thrown.getMessage()));
		}
		return texts.stream();
	}
}
