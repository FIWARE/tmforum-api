package org.fiware.tmforum.common.mapping;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.ngsi.model.ProblemDetailsVO;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory stand-in for the NGSI-LD entities endpoint, so the declarative {@code EntitiesApiClient} can be exercised
 * against the embedded server. Unknown ids are answered with an NGSI-LD problem-details body.
 */
@Requires(property = StubBrokerController.SPEC_NAME_PROPERTY, value = NgsiLdMappingRegressionTest.SPEC_NAME)
@Controller(StubBrokerController.ENTITIES_PATH)
public class StubBrokerController {

	public static final String SPEC_NAME_PROPERTY = "spec.name";
	public static final String ENTITIES_PATH = "/ngsi-ld/v1/entities";
	public static final URI NOT_FOUND_TYPE = URI.create("https://uri.etsi.org/ngsi-ld/errors/ResourceNotFound");
	public static final String NOT_FOUND_TITLE = "Entity not found.";
	private static final String LD_JSON = "application/ld+json";

	private final Map<URI, EntityVO> entities = new ConcurrentHashMap<>();

	@Post(consumes = LD_JSON, produces = LD_JSON)
	public HttpResponse<Void> createEntity(@Body EntityVO entityVO) {
		entities.put(entityVO.getId(), entityVO);
		return HttpResponse.created(URI.create(ENTITIES_PATH + "/" + entityVO.getId()));
	}

	@Get(value = "/{entityId}", produces = LD_JSON)
	public HttpResponse<?> retrieveEntity(@PathVariable URI entityId) {
		EntityVO entityVO = entities.get(entityId);
		if (entityVO != null) {
			return HttpResponse.ok(entityVO);
		}
		ProblemDetailsVO problemDetails = new ProblemDetailsVO()
				.type(NOT_FOUND_TYPE)
				.title(NOT_FOUND_TITLE)
				.status(HttpStatus.NOT_FOUND.getCode())
				.detail(entityId.toString());
		return HttpResponse.notFound(problemDetails).contentType(MediaType.APPLICATION_JSON_TYPE);
	}
}
