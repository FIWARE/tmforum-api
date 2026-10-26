package org.fiware.tmforum.common.health;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import org.fiware.tmforum.common.mapping.StubBrokerController;

/**
 * Stands in for the broker's health endpoint in {@link NgsiLdBrokerReadinessIndicatorTest}; answers with whatever
 * status the test sets.
 */
@Requires(property = StubBrokerController.SPEC_NAME_PROPERTY, value = NgsiLdBrokerReadinessIndicatorTest.SPEC_NAME)
@Controller(StubBrokerHealthController.PATH)
public class StubBrokerHealthController {

	public static final String PATH = "/stub-broker-health";

	private volatile HttpStatus status = HttpStatus.OK;

	public void setStatus(HttpStatus status) {
		this.status = status;
	}

	@Get
	public HttpResponse<String> health() {
		return HttpResponse.status(status).body(status.getReason());
	}
}
