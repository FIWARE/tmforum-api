package org.fiware.tmforum.accesspolicy;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.order.Ordered;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.context.ServerRequestContext;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.filter.ServerFilterPhase;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Publishes the request's {@link Subject} under {@link #SUBJECT_ATTR}: the bearer token's subject, else anonymous.
 * A missing or invalid token never fails the request, and the token itself is kept nowhere.
 */
@Filter(Filter.MATCH_ALL_PATTERN)
@Requires(property = AccessPolicyProperties.ENABLED_PROPERTY, value = "true")
public class SubjectResolutionFilter implements HttpServerFilter, Ordered {

	public static final String SUBJECT_ATTR = "access-policy.subject";

	private static final String BEARER_SCHEME = "Bearer";

	private final BearerTokenValidator validator;

	public SubjectResolutionFilter(BearerTokenValidator validator) {
		this.validator = validator;
	}

	@Override
	public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
		Optional<String> token = bearerToken(request);
		if (token.isEmpty()) {
			return chain.proceed(request.setAttribute(SUBJECT_ATTR, Subject.ANONYMOUS));
		}
		return validator.resolve(token.get())
				.onErrorReturn(Subject.ANONYMOUS)
				.defaultIfEmpty(Subject.ANONYMOUS)
				.flatMap(subject -> Mono.from(chain.proceed(request.setAttribute(SUBJECT_ATTR, subject))));
	}

	/** The subject this filter resolved for {@code request}; empty when the filter has not run for it. */
	public static Optional<Subject> subjectOf(HttpRequest<?> request) {
		return request.getAttribute(SUBJECT_ATTR, Subject.class);
	}

	/** The subject of the request being served; empty outside a request or when the filter has not run for it. */
	public static Optional<Subject> current() {
		return ServerRequestContext.currentRequest().flatMap(SubjectResolutionFilter::subjectOf);
	}

	@Override
	public int getOrder() {
		// Must run after ForwardedForFilter (HIGHEST_PRECEDENCE) and before anything reading the subject, including
		// PaginationFilter (LOWEST_PRECEDENCE); the security phase is where Micronaut places authentication.
		return ServerFilterPhase.SECURITY.order();
	}

	private static Optional<String> bearerToken(HttpRequest<?> request) {
		return request.getHeaders().getAuthorization()
				.map(String::strip)
				.filter(header -> header.regionMatches(true, 0, BEARER_SCHEME, 0, BEARER_SCHEME.length()))
				.map(header -> header.substring(BEARER_SCHEME.length()))
				// the scheme must be followed by whitespace, so "BearerX" is another scheme
				.filter(rest -> rest.isEmpty() || Character.isWhitespace(rest.charAt(0)))
				.map(String::strip)
				.filter(rest -> !rest.isEmpty());
	}
}
