package org.fiware.tmforum.common.mapping;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

import java.util.Map;

/**
 * Serves a deployment-specific JSON-LD context at {@code mapping.context-url}, so the injected key compactor's
 * resolution of the configured context can be checked against the embedded server.
 */
@Requires(property = StubBrokerController.SPEC_NAME_PROPERTY, value = NgsiLdMappingRegressionTest.SPEC_NAME)
@Controller(StubContextController.CONTEXT_PATH)
public class StubContextController {

	public static final String CONTEXT_PATH = "/context.jsonld";
	private static final String LD_JSON = "application/ld+json";
	private static final String CONTEXT_KEY = "@context";
	private static final String NAME_TERM = "name";

	@Get(produces = LD_JSON)
	public Map<String, Object> context() {
		return Map.of(CONTEXT_KEY, Map.of(NAME_TERM, TolerantEntityVOMapperTest.CUSTOM_NAME_IRI));
	}
}
