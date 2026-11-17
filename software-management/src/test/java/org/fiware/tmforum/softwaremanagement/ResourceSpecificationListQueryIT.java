package org.fiware.tmforum.softwaremanagement;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.softwaremanagement.api.ResourceSpecificationApiTestClient;
import org.fiware.softwaremanagement.model.ResourceSpecificationCreateVO;
import org.fiware.softwaremanagement.model.ResourceSpecificationCreateVOTestExample;
import org.fiware.softwaremanagement.model.ResourceSpecificationVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.fiware.tmforum.resource.ResourceTypeRegistry;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Filters over the polymorphic ResourceSpecification list (TMF730 sub-types in one query), on every backend.
 */
@MicronautTest(packages = {"org.fiware.tmforum.softwaremanagement"}, contextBuilder = FirstMatchConfigContextBuilder.class)
public class ResourceSpecificationListQueryIT extends AbstractApiIT {

	private static final List<String> AT_TYPES = List.of("ResourceSpecification", "SoftwareResourceSpecification",
			"APISpecification", "LogicalResourceSpecification");
	// sub-types declaring buildNumber
	private static final List<String> WITH_BUILD_NUMBER = List.of("SoftwareResourceSpecification", "APISpecification");
	private static final List<String> LIFECYCLE_STATUS = List.of("created", "released", "retired");
	private static final int SPECIFICATION_COUNT = 12;
	private static final String BUILD_NUMBER = "buildNumber";
	private static final String BUILD_NUMBER_FORMAT = "build-%d";
	private static final String PATH = "/resourceSpecification";
	private static final int ALL = 100;

	private final ResourceSpecificationApiTestClient resourceSpecificationApiTestClient;
	private final HttpClient httpClient;

	public ResourceSpecificationListQueryIT(ResourceSpecificationApiTestClient resourceSpecificationApiTestClient,
			@Client("/") HttpClient httpClient, EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper,
			GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.resourceSpecificationApiTestClient = resourceSpecificationApiTestClient;
		this.httpClient = httpClient;
	}

	@MockBean(TMForumEventHandler.class)
	public TMForumEventHandler eventHandler() {
		TMForumEventHandler eventHandler = mock(TMForumEventHandler.class);
		when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
		when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());
		return eventHandler;
	}

	@Override
	protected String getEntityType() {
		return ResourceTypeRegistry.ALL_SPEC_TYPES;
	}

	/**
	 * One seeded specification, holding what the filters read.
	 */
	private record Specification(String id, String atType, String lifecycleStatus, String buildNumber) {
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("filterCases")
	public void filterPolymorphicList(String query, Predicate<Specification> matches) throws Exception {
		List<Specification> seeds = IntStream.range(0, SPECIFICATION_COUNT).mapToObj(this::create).toList();

		HttpResponse<String> response = callAndCatch(() -> httpClient.toBlocking()
				.exchange(HttpRequest.GET(PATH + "?" + query + "&limit=" + ALL), String.class));

		assertEquals(HttpStatus.OK, response.getStatus(), query);
		List<String> expected = seeds.stream().filter(matches).map(Specification::id).sorted().toList();
		assertEquals(expected, ids(response).stream().sorted().toList(), query);
	}

	private static Stream<Arguments> filterCases() {
		return Stream.of(
				Arguments.of("lifecycleStatus=released",
						(Predicate<Specification>) s -> s.lifecycleStatus().equals("released")),
				Arguments.of("@type=APISpecification",
						(Predicate<Specification>) s -> s.atType().equals("APISpecification")),
				Arguments.of("@type=SoftwareResourceSpecification&lifecycleStatus=created,retired",
						(Predicate<Specification>) s -> s.atType().equals("SoftwareResourceSpecification")
								&& !s.lifecycleStatus().equals("released")),
				// declared by some sub-types only, so it is undeclared on the queried base class
				Arguments.of(BUILD_NUMBER + "=" + String.format(BUILD_NUMBER_FORMAT, 1),
						(Predicate<Specification>) s -> String.format(BUILD_NUMBER_FORMAT, 1).equals(s.buildNumber())));
	}

	private Specification create(int index) {
		String atType = AT_TYPES.get(index % AT_TYPES.size());
		String lifecycleStatus = LIFECYCLE_STATUS.get(index % LIFECYCLE_STATUS.size());
		String buildNumber = WITH_BUILD_NUMBER.contains(atType)
				? String.format(BUILD_NUMBER_FORMAT, index % LIFECYCLE_STATUS.size())
				: null;
		ResourceSpecificationCreateVO createVO = ResourceSpecificationCreateVOTestExample.build()
				.atSchemaLocation(null)
				.targetResourceSchema(null)
				.lifecycleStatus(lifecycleStatus)
				.atType(atType);
		if (buildNumber != null) {
			createVO.setUnknownProperties(BUILD_NUMBER, buildNumber);
		}
		try {
			HttpResponse<ResourceSpecificationVO> created = callAndCatch(
					() -> resourceSpecificationApiTestClient.createResourceSpecification(null, createVO));
			assertEquals(HttpStatus.CREATED, created.getStatus(), "The specification should have been created.");
			return new Specification(created.body().getId(), atType, lifecycleStatus, buildNumber);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private List<String> ids(HttpResponse<String> response) {
		JsonNode body = objectMapper.readTree(response.body());
		List<String> ids = new ArrayList<>();
		body.forEach(entity -> ids.add(entity.get("id").asString()));
		return ids;
	}
}
