package org.fiware.tmforum.productcatalog;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import org.fiware.tmforum.accesspolicy.Subject;
import org.fiware.tmforum.accesspolicy.SubjectResolutionFilter;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;

/**
 * Echoes the subject {@link SubjectResolutionFilter} resolved, for the access-policy ITs.
 * {@code propagated} tells whether {@link SubjectResolutionFilter#current()} still sees the same subject after a
 * {@link Mono} hops to another scheduler thread.
 */
@Controller(SubjectEchoController.PATH)
@Requires(property = SubjectEchoController.ENABLED_PROPERTY, value = "true")
public class SubjectEchoController {

	static final String PATH = "/test-subject";
	static final String ENABLED_PROPERTY = "test.subject-echo.enabled";

	@Get
	public Mono<SubjectEcho> echo(HttpRequest<?> request) {
		Optional<Subject> fromAttribute = SubjectResolutionFilter.subjectOf(request);
		// hop to another thread first, as the AP-08 decorator reads the subject after a storage I/O hop
		return Mono.just(fromAttribute)
				.publishOn(Schedulers.boundedElastic())
				.map(ignored -> SubjectResolutionFilter.current())
				.map(fromContext -> SubjectEcho.of(fromAttribute,
						fromAttribute.isPresent() && fromAttribute.equals(fromContext)));
	}

	/** The echoed subject; {@code resolved} is false when the filter did not run. */
	public record SubjectEcho(boolean resolved, boolean anonymous, String identity, String fingerprint,
							  boolean propagated) {

		static SubjectEcho of(Optional<Subject> subject, boolean propagated) {
			return new SubjectEcho(subject.isPresent(), subject.map(Subject::isAnonymous).orElse(false),
					subject.map(Subject::identity).orElse(null),
					subject.map(Subject::credentialFingerprint).orElse(null), propagated);
		}
	}
}
