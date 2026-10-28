package org.fiware.tmforum.productcatalog;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.productcatalog.api.CatalogApiTestClient;
import org.fiware.productcatalog.api.CategoryApiTestClient;
import org.fiware.productcatalog.api.ProductOfferingApiTestClient;
import org.fiware.productcatalog.api.ProductSpecificationApiTestClient;
import org.fiware.productcatalog.model.CatalogCreateVO;
import org.fiware.productcatalog.model.CatalogCreateVOTestExample;
import org.fiware.productcatalog.model.CatalogVO;
import org.fiware.productcatalog.model.CategoryCreateVOTestExample;
import org.fiware.productcatalog.model.CategoryRefVO;
import org.fiware.productcatalog.model.CategoryRefVOTestExample;
import org.fiware.productcatalog.model.CategoryVO;
import org.fiware.productcatalog.model.ProductOfferingCreateVO;
import org.fiware.productcatalog.model.ProductOfferingCreateVOTestExample;
import org.fiware.productcatalog.model.ProductOfferingUpdateVO;
import org.fiware.productcatalog.model.ProductOfferingUpdateVOTestExample;
import org.fiware.productcatalog.model.ProductOfferingVO;
import org.fiware.productcatalog.model.ProductSpecificationCreateVOTestExample;
import org.fiware.productcatalog.model.ProductSpecificationVO;
import org.fiware.productcatalog.model.ServiceCandidateRefVO;
import org.fiware.productcatalog.model.ServiceCandidateRefVOTestExample;
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
 * Validation and read-back of references to entities held by another deployment (external: the href names a foreign
 * host) next to local ones. Separate from the other ITs, since it configures {@code references.local-hosts}.
 */
@MicronautTest(packages = {"org.fiware.tmforum.productcatalog"}, contextBuilder = FirstMatchConfigContextBuilder.class)
@Property(name = "references.local-hosts", value = ExternalReferenceIT.LOCAL_ALIAS_HOST)
public class ExternalReferenceIT extends AbstractApiIT {

	static final String LOCAL_ALIAS_HOST = "tmf.local-alias.example.org";
	private static final String EXTERNAL_BASE_URL = "https://external-tmf.example.org/tmf-api/";
	private static final String LOCAL_ALIAS_BASE_URL = "https://" + LOCAL_ALIAS_HOST + "/tmf-api/";
	private static final String CATEGORY_PATH = "productCatalog/v4/category/";
	private static final String SERVICE_CANDIDATE_PATH = "serviceCatalog/v4/serviceCandidate/";
	// outside every local id namespace in use, so neither the write nor the read path finds a local entity
	private static final String REMOTE_CATEGORY_ID_PREFIX = "urn:ngsi-ld:category:remote-";
	private static final String MISSING_CATEGORY_ID_PREFIX = "urn:ngsi-ld:category:missing-";
	private static final String REMOTE_SERVICE_CANDIDATE_ID = "urn:ngsi-ld:service-candidate:remote-1";
	private static final String REMOTE_NAME = "Held by another deployment";
	// TmForumExceptionHandler's INVALID_RELATIONSHIP message, telling a failed reference check from other 400s
	private static final String UNRESOLVED_REFERENCE_MESSAGE = "At least one of the references does not exist";

	/**
	 * How a category reference in a test case is built; resolved against the entities created by the test.
	 */
	enum RefKind {
		// an existing local category, referenced without href
		LOCAL,
		// a non-existent id with a foreign href
		EXTERNAL,
		// a non-existent id without href
		MISSING,
		// a non-existent id with an href naming general.serverHost
		MISSING_SERVER_HOST,
		// a non-existent id with an href naming a references.local-hosts entry
		MISSING_LOCAL_ALIAS,
		// the id of a local product specification with a foreign href (the external type guard)
		EXTERNAL_WRONG_TYPE,
		// an existing local category with a foreign href (the type guard resolves it locally)
		LOCAL_FOREIGN_HREF
	}

	private final ProductOfferingApiTestClient productOfferingApiTestClient;
	private final CatalogApiTestClient catalogApiTestClient;
	private final CategoryApiTestClient categoryApiTestClient;
	private final ProductSpecificationApiTestClient productSpecificationApiTestClient;
	private final GeneralProperties generalProperties;

	// cleanUp only removes product offerings
	private final List<String> createdCatalogs = new ArrayList<>();
	private final List<String> createdCategories = new ArrayList<>();
	private final List<String> createdProductSpecifications = new ArrayList<>();

	public ExternalReferenceIT(EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper,
							   GeneralProperties generalProperties,
							   ProductOfferingApiTestClient productOfferingApiTestClient,
							   CatalogApiTestClient catalogApiTestClient,
							   CategoryApiTestClient categoryApiTestClient,
							   ProductSpecificationApiTestClient productSpecificationApiTestClient) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.generalProperties = generalProperties;
		this.productOfferingApiTestClient = productOfferingApiTestClient;
		this.catalogApiTestClient = catalogApiTestClient;
		this.categoryApiTestClient = categoryApiTestClient;
		this.productSpecificationApiTestClient = productSpecificationApiTestClient;
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
		for (String id : createdCatalogs) {
			callAndCatch(() -> catalogApiTestClient.deleteCatalog(null, id));
		}
		for (String id : createdCategories) {
			callAndCatch(() -> categoryApiTestClient.deleteCategory(null, id));
		}
		for (String id : createdProductSpecifications) {
			callAndCatch(() -> productSpecificationApiTestClient.deleteProductSpecification(null, id));
		}
	}

	private static Stream<Arguments> categoryReferences() {
		return Stream.of(
				Arguments.of("An external category is accepted.", List.of(RefKind.EXTERNAL), HttpStatus.CREATED),
				Arguments.of("Several external categories are accepted.",
						List.of(RefKind.EXTERNAL, RefKind.EXTERNAL), HttpStatus.CREATED),
				Arguments.of("An existing local and an external category are accepted.",
						List.of(RefKind.LOCAL, RefKind.EXTERNAL), HttpStatus.CREATED),
				Arguments.of("A missing local category is rejected.", List.of(RefKind.MISSING),
						HttpStatus.BAD_REQUEST),
				Arguments.of("An existing and a missing local category are rejected (#178).",
						List.of(RefKind.LOCAL, RefKind.MISSING), HttpStatus.BAD_REQUEST),
				Arguments.of("An external category does not excuse a missing local one.",
						List.of(RefKind.EXTERNAL, RefKind.MISSING), HttpStatus.BAD_REQUEST),
				Arguments.of("An href naming this deployment keeps a missing category local.",
						List.of(RefKind.MISSING_SERVER_HOST), HttpStatus.BAD_REQUEST),
				Arguments.of("An href naming a configured local host keeps a missing category local.",
						List.of(RefKind.MISSING_LOCAL_ALIAS), HttpStatus.BAD_REQUEST),
				Arguments.of("A foreign href must not smuggle in a local entity of another type.",
						List.of(RefKind.EXTERNAL_WRONG_TYPE), HttpStatus.BAD_REQUEST),
				Arguments.of("A foreign href on an existing local category of the accepted type is accepted.",
						List.of(RefKind.LOCAL_FOREIGN_HREF), HttpStatus.CREATED));
	}

	@ParameterizedTest
	@MethodSource("categoryReferences")
	public void createProductOfferingWithCategories(String message, List<RefKind> kinds, HttpStatus expectedStatus)
			throws Exception {
		List<CategoryRefVO> categories = categoryRefs(kinds);
		ProductOfferingCreateVO createVO = productOfferingCreate().category(categories);

		HttpResponse<ProductOfferingVO> createResponse = callAndCatch(
				() -> productOfferingApiTestClient.createProductOffering(null, createVO));
		assertEquals(expectedStatus, createResponse.getStatus(), message);

		if (expectedStatus == HttpStatus.CREATED) {
			assertCategoriesReadBack(createResponse.body().getId(), categories, message);
		} else {
			assertRejected(createResponse);
		}
	}

	@ParameterizedTest
	@MethodSource("categoryReferences")
	public void patchProductOfferingWithCategories(String message, List<RefKind> kinds, HttpStatus createdStatus)
			throws Exception {
		HttpStatus expectedStatus = createdStatus == HttpStatus.CREATED ? HttpStatus.OK : createdStatus;
		HttpResponse<ProductOfferingVO> createResponse = callAndCatch(
				() -> productOfferingApiTestClient.createProductOffering(null, productOfferingCreate()));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The product offering should have been created.");
		String id = createResponse.body().getId();

		List<CategoryRefVO> categories = categoryRefs(kinds);
		ProductOfferingUpdateVO updateVO = ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null)
				.productSpecification(null)
				.resourceCandidate(null)
				.serviceCandidate(null)
				.serviceLevelAgreement(null)
				.category(categories);
		HttpResponse<ProductOfferingVO> patchResponse = callAndCatch(
				() -> productOfferingApiTestClient.patchProductOffering(null, id, updateVO));
		assertEquals(expectedStatus, patchResponse.getStatus(), message);

		if (expectedStatus == HttpStatus.OK) {
			assertCategoriesReadBack(id, categories, message);
		} else {
			assertRejected(patchResponse);
			List<CategoryRefVO> stored = retrieve(id).getCategory();
			assertTrue(stored == null || stored.isEmpty(), "A rejected patch must not store anything: " + message);
		}
	}

	@Test
	public void createProductOfferingWithExternalServiceCandidate() throws Exception {
		ServiceCandidateRefVO serviceCandidate = ServiceCandidateRefVOTestExample.build().atSchemaLocation(null)
				.id(REMOTE_SERVICE_CANDIDATE_ID)
				.href(URI.create(EXTERNAL_BASE_URL + SERVICE_CANDIDATE_PATH + REMOTE_SERVICE_CANDIDATE_ID))
				.name(REMOTE_NAME);
		ProductOfferingCreateVO createVO = productOfferingCreate().serviceCandidate(serviceCandidate);

		HttpResponse<ProductOfferingVO> createResponse = callAndCatch(
				() -> productOfferingApiTestClient.createProductOffering(null, createVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"A product offering with an external service candidate should be created.");

		// ServiceCandidateRef has no @MappingEnabled, so the unresolved target is built through its String constructor
		ProductOfferingVO retrieved = retrieve(createResponse.body().getId());
		assertEquals(REMOTE_SERVICE_CANDIDATE_ID, retrieved.getServiceCandidate().getId(),
				"The external service candidate should be read back.");
	}

	@Test
	public void createCatalogWithExternalCategory() throws Exception {
		CategoryRefVO externalCategory = externalCategoryRef(0);
		CatalogCreateVO createVO = CatalogCreateVOTestExample.build().atSchemaLocation(null)
				.relatedParty(null)
				.category(List.of(externalCategory));

		HttpResponse<CatalogVO> createResponse = callAndCatch(() -> catalogApiTestClient.createCatalog(null, createVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"A catalog with an external category should be created.");
		String id = createResponse.body().getId();
		createdCatalogs.add(id);

		HttpResponse<CatalogVO> retrieved = callAndCatch(() -> catalogApiTestClient.retrieveCatalog(null, id, null));
		assertEquals(HttpStatus.OK, retrieved.getStatus(), "The catalog should be retrievable.");
		// Catalog.category is read from the relationship's own properties, so the whole reference round-trips
		CategoryRefVO readBack = retrieved.body().getCategory().getFirst();
		assertEquals(externalCategory.getId(), readBack.getId(), "The id should round-trip.");
		assertEquals(externalCategory.getHref(), readBack.getHref(), "The href should round-trip.");
		assertEquals(externalCategory.getName(), readBack.getName(), "The name should round-trip.");
	}

	private ProductOfferingCreateVO productOfferingCreate() {
		return ProductOfferingCreateVOTestExample.build().atSchemaLocation(null)
				.productSpecification(null)
				.resourceCandidate(null)
				.serviceCandidate(null)
				.serviceLevelAgreement(null)
				.category(null);
	}

	private ProductOfferingVO retrieve(String id) throws Exception {
		HttpResponse<ProductOfferingVO> retrieved = callAndCatch(
				() -> productOfferingApiTestClient.retrieveProductOffering(null, id, null));
		assertEquals(HttpStatus.OK, retrieved.getStatus(), "An unresolvable target must not fail the retrieval.");
		return retrieved.body();
	}

	private void assertCategoriesReadBack(String productOfferingId, List<CategoryRefVO> expected, String message)
			throws Exception {
		List<CategoryRefVO> readBack = retrieve(productOfferingId).getCategory();
		// only ids: ProductOffering.category fetches its targets, so an external one reads back without its
		// href/name until ticket 5 stores the reference payload inline (fromProperties)
		assertEquals(expected.stream().map(CategoryRefVO::getId).sorted().toList(),
				readBack.stream().map(CategoryRefVO::getId).sorted().toList(), message);
	}

	private static void assertRejected(HttpResponse<?> response) {
		ErrorDetails details = response.getBody(ErrorDetails.class)
				.orElseThrow(() -> new AssertionError("Error details should be provided."));
		assertTrue(details.message().startsWith(UNRESOLVED_REFERENCE_MESSAGE),
				"The request should fail the reference validation, not something else: " + details.message());
	}

	private List<CategoryRefVO> categoryRefs(List<RefKind> kinds) throws Exception {
		List<CategoryRefVO> refs = new ArrayList<>();
		for (int i = 0; i < kinds.size(); i++) {
			refs.add(categoryRef(kinds.get(i), i));
		}
		return refs;
	}

	// the position keeps the ids of several references of one kind apart
	private CategoryRefVO categoryRef(RefKind kind, int position) throws Exception {
		String missingId = MISSING_CATEGORY_ID_PREFIX + position;
		return switch (kind) {
			case LOCAL -> categoryRefVO(createCategory(), null);
			case EXTERNAL -> externalCategoryRef(position);
			case MISSING -> categoryRefVO(missingId, null);
			case MISSING_SERVER_HOST -> categoryRefVO(missingId,
					URI.create(generalProperties.getServerHost() + "/" + CATEGORY_PATH + missingId));
			case MISSING_LOCAL_ALIAS -> categoryRefVO(missingId,
					URI.create(LOCAL_ALIAS_BASE_URL + CATEGORY_PATH + missingId));
			case EXTERNAL_WRONG_TYPE -> {
				String specId = createProductSpecification();
				yield categoryRefVO(specId, URI.create(EXTERNAL_BASE_URL + CATEGORY_PATH + specId));
			}
			case LOCAL_FOREIGN_HREF -> {
				String categoryId = createCategory();
				yield categoryRefVO(categoryId, URI.create(EXTERNAL_BASE_URL + CATEGORY_PATH + categoryId));
			}
		};
	}

	private static CategoryRefVO externalCategoryRef(int position) {
		String id = REMOTE_CATEGORY_ID_PREFIX + position;
		return categoryRefVO(id, URI.create(EXTERNAL_BASE_URL + CATEGORY_PATH + id)).name(REMOTE_NAME);
	}

	private static CategoryRefVO categoryRefVO(String id, URI href) {
		return CategoryRefVOTestExample.build().atSchemaLocation(null)
				.id(id)
				.href(href)
				.name(null)
				.version(null)
				.atBaseType(null)
				.atType(null)
				.atReferredType(null);
	}

	private String createCategory() throws Exception {
		HttpResponse<CategoryVO> response = callAndCatch(() -> categoryApiTestClient.createCategory(null,
				CategoryCreateVOTestExample.build().atSchemaLocation(null).parentId(null)));
		assertEquals(HttpStatus.CREATED, response.getStatus(), "The local category should have been created.");
		createdCategories.add(response.body().getId());
		return response.body().getId();
	}

	private String createProductSpecification() throws Exception {
		HttpResponse<ProductSpecificationVO> response = callAndCatch(
				() -> productSpecificationApiTestClient.createProductSpecification(null,
						ProductSpecificationCreateVOTestExample.build().atSchemaLocation(null)
								.targetProductSchema(null)
								.productSpecCharacteristic(null)));
		assertEquals(HttpStatus.CREATED, response.getStatus(),
				"The local product specification should have been created.");
		createdProductSpecifications.add(response.body().getId());
		return response.body().getId();
	}

	@Override
	protected String getEntityType() {
		return ProductOffering.TYPE_PRODUCT_OFFERING;
	}
}
