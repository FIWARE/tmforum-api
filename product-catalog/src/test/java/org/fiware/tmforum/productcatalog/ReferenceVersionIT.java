package org.fiware.tmforum.productcatalog;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.productcatalog.api.CategoryApiTestClient;
import org.fiware.productcatalog.api.ProductOfferingApiTestClient;
import org.fiware.productcatalog.api.ProductSpecificationApiTestClient;
import org.fiware.productcatalog.model.CategoryCreateVOTestExample;
import org.fiware.productcatalog.model.CategoryRefVO;
import org.fiware.productcatalog.model.CategoryVO;
import org.fiware.productcatalog.model.ProductOfferingCreateVO;
import org.fiware.productcatalog.model.ProductOfferingCreateVOTestExample;
import org.fiware.productcatalog.model.ProductOfferingUpdateVO;
import org.fiware.productcatalog.model.ProductOfferingVO;
import org.fiware.productcatalog.model.ProductSpecificationCreateVO;
import org.fiware.productcatalog.model.ProductSpecificationRefVO;
import org.fiware.productcatalog.model.ProductSpecificationUpdateVO;
import org.fiware.productcatalog.model.ProductSpecificationVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.fiware.tmforum.product.ProductOffering;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Reference version semantics end to end: a {@code *Ref} pinned to a version resolves only against the target's current
 * version, an unpinned one against any. Separate from the other ITs, since it configures an external host.
 */
@MicronautTest(packages = {"org.fiware.tmforum.productcatalog"}, contextBuilder = FirstMatchConfigContextBuilder.class)
@Property(name = "references.external-hosts", value = ReferenceVersionIT.EXTERNAL_HOST)
public class ReferenceVersionIT extends AbstractApiIT {

	static final String EXTERNAL_HOST = "external-tmf.example.org";
	private static final String EXTERNAL_SPECIFICATION_BASE_URL =
			"https://" + EXTERNAL_HOST + "/tmf-api/productCatalog/v4/productSpecification/";
	private static final String SPECIFICATION_NAME = "Versioned specification";
	private static final String CURRENT_VERSION = "1.0";
	private static final String OTHER_VERSION = "2.0";
	private static final String CASE_DIFFERENT_VERSION = "V1";
	private static final String LOWER_CASE_VERSION = "v1";
	private static final String NO_VERSION = "";
	private static final String INVALID_RELATIONSHIP_MESSAGE = "At least one of the references does not exist";
	private static final String VERSION_MISMATCH_MESSAGE = "Reference version mismatch";

	private final ProductOfferingApiTestClient productOfferingApiTestClient;
	private final ProductSpecificationApiTestClient productSpecificationApiTestClient;
	private final CategoryApiTestClient categoryApiTestClient;

	// cleanUp only removes product offerings
	private final List<String> createdProductSpecifications = new ArrayList<>();
	private final List<String> createdCategories = new ArrayList<>();

	public ReferenceVersionIT(EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper,
							  GeneralProperties generalProperties,
							  ProductOfferingApiTestClient productOfferingApiTestClient,
							  ProductSpecificationApiTestClient productSpecificationApiTestClient,
							  CategoryApiTestClient categoryApiTestClient) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.productOfferingApiTestClient = productOfferingApiTestClient;
		this.productSpecificationApiTestClient = productSpecificationApiTestClient;
		this.categoryApiTestClient = categoryApiTestClient;
	}

	@MockBean(TMForumEventHandler.class)
	public TMForumEventHandler eventHandler() {
		TMForumEventHandler eventHandler = mock(TMForumEventHandler.class);
		when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
		when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());
		return eventHandler;
	}

	@AfterEach
	public void removeReferencedEntities() throws Exception {
		for (String id : createdProductSpecifications) {
			callAndCatch(() -> productSpecificationApiTestClient.deleteProductSpecification(null, id));
		}
		for (String id : createdCategories) {
			callAndCatch(() -> categoryApiTestClient.deleteCategory(null, id));
		}
	}

	private static Stream<Arguments> specificationReferences() {
		return Stream.of(
				Arguments.of("An unpinned reference resolves any version.", null, HttpStatus.CREATED),
				Arguments.of("An empty version pins nothing.", NO_VERSION, HttpStatus.CREATED),
				Arguments.of("A reference pinned to the current version resolves.", CURRENT_VERSION,
						HttpStatus.CREATED),
				Arguments.of("A reference pinned to another version is rejected.", OTHER_VERSION,
						HttpStatus.BAD_REQUEST));
	}

	private static Stream<Arguments> referencesAfterVersionPatch() {
		return Stream.of(
				Arguments.of("A reference pinned to the superseded version is rejected.", CURRENT_VERSION,
						HttpStatus.BAD_REQUEST),
				Arguments.of("A reference pinned to the patched version resolves.", OTHER_VERSION,
						HttpStatus.CREATED),
				Arguments.of("An unpinned reference follows the patched version.", null, HttpStatus.CREATED));
	}

	private static Stream<Arguments> referencesToUnversionedCategory() {
		return Stream.of(
				Arguments.of("A pinned reference to a category without version is rejected.", CURRENT_VERSION,
						HttpStatus.BAD_REQUEST),
				Arguments.of("An unpinned reference to a category without version resolves.", null,
						HttpStatus.CREATED));
	}

	private static Stream<Arguments> externalReferencesToLocalSpecification() {
		return Stream.of(
				Arguments.of("An external href must not smuggle in a wrong version of a local entity.", OTHER_VERSION,
						HttpStatus.BAD_REQUEST),
				Arguments.of("An external href on a local entity of the pinned version resolves.", CURRENT_VERSION,
						HttpStatus.CREATED));
	}

	@ParameterizedTest
	@MethodSource("specificationReferences")
	public void createWithPinnedSpecification(String message, String refVersion, HttpStatus expectedStatus)
			throws Exception {
		String specificationId = createProductSpecification(CURRENT_VERSION);
		ProductSpecificationRefVO ref = new ProductSpecificationRefVO().id(specificationId).version(refVersion);

		assertCreate(productOfferingCreate().productSpecification(ref), expectedStatus, message);
	}

	@ParameterizedTest
	@MethodSource("specificationReferences")
	public void patchWithPinnedSpecification(String message, String refVersion, HttpStatus createdStatus)
			throws Exception {
		HttpStatus expectedStatus = createdStatus == HttpStatus.CREATED ? HttpStatus.OK : createdStatus;
		String specificationId = createProductSpecification(CURRENT_VERSION);
		HttpResponse<ProductOfferingVO> createResponse = callAndCatch(
				() -> productOfferingApiTestClient.createProductOffering(null, productOfferingCreate()));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The product offering should have been created.");
		String id = createResponse.body().getId();

		ProductOfferingUpdateVO updateVO = new ProductOfferingUpdateVO()
				.productSpecification(new ProductSpecificationRefVO().id(specificationId).version(refVersion));
		HttpResponse<ProductOfferingVO> patchResponse = callAndCatch(
				() -> productOfferingApiTestClient.patchProductOffering(null, id, updateVO));
		assertEquals(expectedStatus, patchResponse.getStatus(), message);
		if (expectedStatus == HttpStatus.BAD_REQUEST) {
			assertReferenceRejected(patchResponse);
		}
	}

	@ParameterizedTest
	@MethodSource("referencesAfterVersionPatch")
	public void referenceFollowsPatchedVersion(String message, String refVersion, HttpStatus expectedStatus)
			throws Exception {
		String specificationId = createProductSpecification(CURRENT_VERSION);
		patchSpecificationVersion(specificationId, OTHER_VERSION);
		ProductSpecificationRefVO ref = new ProductSpecificationRefVO().id(specificationId).version(refVersion);

		assertCreate(productOfferingCreate().productSpecification(ref), expectedStatus, message);
	}

	// no version-specific endpoint or query parameter exists, the patch replaces the version in place
	@Test
	public void patchUpdatesTheVersionInPlace() throws Exception {
		String specificationId = createProductSpecification(CURRENT_VERSION);
		patchSpecificationVersion(specificationId, OTHER_VERSION);

		HttpResponse<ProductSpecificationVO> retrieved = callAndCatch(
				() -> productSpecificationApiTestClient.retrieveProductSpecification(null, specificationId, null));
		assertEquals(HttpStatus.OK, retrieved.getStatus(), "The specification should be retrievable.");
		assertEquals(OTHER_VERSION, retrieved.body().getVersion(), "The specification should carry the new version.");

		HttpResponse<List<ProductSpecificationVO>> listed = callAndCatch(
				() -> productSpecificationApiTestClient.listProductSpecification(null, null, null, null));
		assertEquals(HttpStatus.OK, listed.getStatus(), "The specifications should be listable.");
		List<ProductSpecificationVO> matching = listed.body().stream()
				.filter(specification -> specificationId.equals(specification.getId()))
				.toList();
		assertEquals(1, matching.size(), "Only the current version of the specification should exist.");
		assertEquals(OTHER_VERSION, matching.getFirst().getVersion(), "The listed version should be the new one.");
	}

	@ParameterizedTest
	@MethodSource("referencesToUnversionedCategory")
	public void referenceToCategoryWithoutVersion(String message, String refVersion, HttpStatus expectedStatus)
			throws Exception {
		CategoryRefVO ref = new CategoryRefVO().id(createCategoryWithoutVersion()).version(refVersion);

		assertCreate(productOfferingCreate().category(List.of(ref)), expectedStatus, message);
	}

	@ParameterizedTest
	@MethodSource("externalReferencesToLocalSpecification")
	public void externalReferenceToLocalSpecification(String message, String refVersion, HttpStatus expectedStatus)
			throws Exception {
		String specificationId = createProductSpecification(CURRENT_VERSION);
		ProductSpecificationRefVO ref = new ProductSpecificationRefVO().id(specificationId)
				.href(URI.create(EXTERNAL_SPECIFICATION_BASE_URL + specificationId))
				.version(refVersion);

		assertCreate(productOfferingCreate().productSpecification(ref), expectedStatus, message);
	}

	// a lower-case label next to an upper-case pin, so a case-insensitive comparison would resolve
	@Test
	public void caseDifferentPinIsRejectedAgainstLowerCaseVersion() throws Exception {
		String specificationId = createProductSpecification(LOWER_CASE_VERSION);
		ProductSpecificationRefVO ref = new ProductSpecificationRefVO().id(specificationId)
				.version(CASE_DIFFERENT_VERSION);

		assertCreate(productOfferingCreate().productSpecification(ref), HttpStatus.BAD_REQUEST,
				"The version is compared case-sensitively.");
	}

	private void assertCreate(ProductOfferingCreateVO createVO, HttpStatus expectedStatus, String message)
			throws Exception {
		HttpResponse<ProductOfferingVO> createResponse = callAndCatch(
				() -> productOfferingApiTestClient.createProductOffering(null, createVO));
		assertEquals(expectedStatus, createResponse.getStatus(), message);
		if (expectedStatus == HttpStatus.BAD_REQUEST) {
			assertReferenceRejected(createResponse);
		}
	}

	private static void assertReferenceRejected(HttpResponse<?> response) {
		ErrorDetails details = response.getBody(ErrorDetails.class)
				.orElseThrow(() -> new AssertionError("Error details should be provided."));
		assertTrue(details.message().startsWith(INVALID_RELATIONSHIP_MESSAGE),
				"The request should fail on the reference, not something else: " + details.message());
		assertTrue(details.message().contains(VERSION_MISMATCH_MESSAGE),
				"The reference should be rejected for its version, not as missing: " + details.message());
	}

	private ProductOfferingCreateVO productOfferingCreate() {
		return ProductOfferingCreateVOTestExample.build().atSchemaLocation(null)
				.productSpecification(null)
				.resourceCandidate(null)
				.serviceCandidate(null)
				.serviceLevelAgreement(null)
				.category(null);
	}

	private String createProductSpecification(String version) throws Exception {
		ProductSpecificationCreateVO createVO = new ProductSpecificationCreateVO()
				.name(SPECIFICATION_NAME)
				.version(version);
		HttpResponse<ProductSpecificationVO> response = callAndCatch(
				() -> productSpecificationApiTestClient.createProductSpecification(null, createVO));
		assertEquals(HttpStatus.CREATED, response.getStatus(), "The product specification should have been created.");
		createdProductSpecifications.add(response.body().getId());
		return response.body().getId();
	}

	private void patchSpecificationVersion(String specificationId, String version) throws Exception {
		HttpResponse<ProductSpecificationVO> response = callAndCatch(
				() -> productSpecificationApiTestClient.patchProductSpecification(null, specificationId,
						new ProductSpecificationUpdateVO().version(version)));
		assertEquals(HttpStatus.OK, response.getStatus(), "The specification version should have been patched.");
	}

	private String createCategoryWithoutVersion() throws Exception {
		HttpResponse<CategoryVO> response = callAndCatch(() -> categoryApiTestClient.createCategory(null,
				CategoryCreateVOTestExample.build().atSchemaLocation(null).parentId(null).version(null)));
		assertEquals(HttpStatus.CREATED, response.getStatus(), "The category should have been created.");
		createdCategories.add(response.body().getId());
		return response.body().getId();
	}

	@Override
	protected String getEntityType() {
		return ProductOffering.TYPE_PRODUCT_OFFERING;
	}
}
