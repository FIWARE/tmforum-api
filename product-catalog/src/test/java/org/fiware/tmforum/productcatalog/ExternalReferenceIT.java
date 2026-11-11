package org.fiware.tmforum.productcatalog;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.env.Environment;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.productcatalog.api.CatalogApiTestClient;
import org.fiware.productcatalog.api.CategoryApiTestClient;
import org.fiware.productcatalog.api.ProductOfferingApiTestClient;
import org.fiware.productcatalog.api.ProductSpecificationApiTestClient;
import org.fiware.productcatalog.model.AgreementRefVO;
import org.fiware.productcatalog.model.BundledProductOfferingOptionVO;
import org.fiware.productcatalog.model.BundledProductOfferingVO;
import org.fiware.productcatalog.model.CatalogCreateVO;
import org.fiware.productcatalog.model.CatalogCreateVOTestExample;
import org.fiware.productcatalog.model.CatalogVO;
import org.fiware.productcatalog.model.CategoryCreateVOTestExample;
import org.fiware.productcatalog.model.CategoryRefVO;
import org.fiware.productcatalog.model.CategoryRefVOTestExample;
import org.fiware.productcatalog.model.CategoryVO;
import org.fiware.productcatalog.model.ChannelRefVO;
import org.fiware.productcatalog.model.MarketSegmentRefVO;
import org.fiware.productcatalog.model.PlaceRefVO;
import org.fiware.productcatalog.model.ProductOfferingCreateVO;
import org.fiware.productcatalog.model.ProductOfferingCreateVOTestExample;
import org.fiware.productcatalog.model.ProductOfferingPriceRefOrValueVO;
import org.fiware.productcatalog.model.ProductOfferingRelationshipVO;
import org.fiware.productcatalog.model.ProductOfferingUpdateVO;
import org.fiware.productcatalog.model.ProductOfferingUpdateVOTestExample;
import org.fiware.productcatalog.model.ProductOfferingVO;
import org.fiware.productcatalog.model.ProductSpecificationCreateVOTestExample;
import org.fiware.productcatalog.model.ProductSpecificationRefVO;
import org.fiware.productcatalog.model.ProductSpecificationUpdateVO;
import org.fiware.productcatalog.model.ProductSpecificationUpdateVOTestExample;
import org.fiware.productcatalog.model.ProductSpecificationVO;
import org.fiware.productcatalog.model.ResourceCandidateRefVO;
import org.fiware.productcatalog.model.ResourceSpecificationRefVO;
import org.fiware.productcatalog.model.SLARefVO;
import org.fiware.productcatalog.model.ServiceCandidateRefVO;
import org.fiware.productcatalog.model.ServiceCandidateRefVOTestExample;
import org.fiware.productcatalog.model.TargetProductSchemaVO;
import org.fiware.productcatalog.model.TimePeriodVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.ErrorDetails;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.fiware.tmforum.common.test.BrokerOnly;
import org.fiware.tmforum.product.ProductOffering;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Validation and read-back of references to entities held by another deployment (external: the href names a host
 * configured in {@code references.external-hosts}) next to local ones. Separate from the other ITs, since it
 * configures those hosts and {@code references.local-hosts}.
 */
@MicronautTest(packages = {"org.fiware.tmforum.productcatalog"}, contextBuilder = FirstMatchConfigContextBuilder.class)
@Property(name = "references.local-hosts", value = ExternalReferenceIT.LOCAL_ALIAS_HOST)
@Property(name = "references.external-hosts", value = ExternalReferenceIT.EXTERNAL_HOST)
@Property(name = BrokerRequestRecorder.ENABLED_PROPERTY, value = "true")
public class ExternalReferenceIT extends AbstractApiIT {

	static final String LOCAL_ALIAS_HOST = "tmf.local-alias.example.org";
	static final String EXTERNAL_HOST = "external-tmf.example.org";
	// not configured as external, so references to it are validated like local ones
	private static final String UNLISTED_HOST = "unlisted-tmf.example.org";
	private static final String ORION_LD_ENVIRONMENT = "orion-ld";
	private static final String EXTERNAL_BASE_URL = "https://" + EXTERNAL_HOST + "/tmf-api/";
	private static final String UNLISTED_BASE_URL = "https://" + UNLISTED_HOST + "/tmf-api/";
	private static final String LOCAL_ALIAS_BASE_URL = "https://" + LOCAL_ALIAS_HOST + "/tmf-api/";
	private static final String CATEGORY_PATH = "productCatalog/v4/category/";
	private static final String SERVICE_CANDIDATE_PATH = "serviceCatalog/v4/serviceCandidate/";
	// outside every local id namespace in use, so neither the write nor the read path finds a local entity
	private static final String REMOTE_CATEGORY_ID_PREFIX = "urn:ngsi-ld:category:remote-";
	private static final String MISSING_CATEGORY_ID_PREFIX = "urn:ngsi-ld:category:missing-";
	// what an external deployment with plain ids would send
	private static final String NON_URI_ID_VALUE = "1234";
	private static final String REMOTE_SERVICE_CANDIDATE_ID = "urn:ngsi-ld:service-candidate:remote-1";
	private static final String REMOTE_NAME = "Held by another deployment";
	private static final String FAN_OUT_STAGE = "fan-out";
	private static final String REMOTE_VERSION = "2.1";
	private static final String REMOTE_LIFECYCLE_STATUS = "Launched";
	private static final String REMOTE_RELATIONSHIP_TYPE = "requires";
	private static final String REMOTE_ROLE = "dependency";
	private static final Instant REMOTE_VALID_FROM = Instant.parse("2026-01-01T00:00:00Z");
	private static final String REMOTE_SCHEMA_TYPE = "RemoteProductSchema";
	private static final String REMOTE_SCHEMA_PATH = "schema/product.json";
	private static final String AGREEMENT_PATH = "agreementManagement/v4/agreement/";
	private static final String CHANNEL_PATH = "salesChannel/v4/channel/";
	private static final String MARKET_SEGMENT_PATH = "marketSegment/v4/marketSegment/";
	private static final String PLACE_PATH = "geographicAddress/v4/place/";
	private static final String PRODUCT_OFFERING_PATH = "productCatalog/v4/productOffering/";
	private static final String PRODUCT_OFFERING_PRICE_PATH = "productCatalog/v4/productOfferingPrice/";
	private static final String PRODUCT_SPECIFICATION_PATH = "productCatalog/v4/productSpecification/";
	private static final String RESOURCE_CANDIDATE_PATH = "resourceCatalog/v4/resourceCandidate/";
	private static final String RESOURCE_SPECIFICATION_PATH = "resourceCatalog/v4/resourceSpecification/";
	private static final String SLA_PATH = "slaManagement/v4/sla/";
	// the type segment of each external reference's id
	private static final String AGREEMENT_ID_TYPE = "agreement";
	private static final String CATEGORY_ID_TYPE = "category";
	private static final String CHANNEL_ID_TYPE = "channel";
	private static final String MARKET_SEGMENT_ID_TYPE = "market-segment";
	private static final String PLACE_ID_TYPE = "place";
	private static final String PRODUCT_OFFERING_ID_TYPE = "product-offering";
	private static final String PRODUCT_OFFERING_PRICE_ID_TYPE = "product-offering-price";
	private static final String PRODUCT_SPECIFICATION_ID_TYPE = "product-specification";
	private static final String RESOURCE_CANDIDATE_ID_TYPE = "resource-candidate";
	private static final String RESOURCE_SPECIFICATION_ID_TYPE = "resource-specification";
	private static final String SERVICE_CANDIDATE_ID_TYPE = "service-candidate";
	private static final String SLA_ID_TYPE = "service-level-agreement";
	private static final String REFERRED_TYPE_AGREEMENT = "Agreement";
	private static final String REFERRED_TYPE_CATEGORY = "Category";
	private static final String REFERRED_TYPE_CHANNEL = "Channel";
	private static final String REFERRED_TYPE_MARKET_SEGMENT = "MarketSegment";
	private static final String REFERRED_TYPE_PLACE = "Place";
	private static final String REFERRED_TYPE_PRODUCT_OFFERING = "ProductOffering";
	private static final String REFERRED_TYPE_PRODUCT_OFFERING_PRICE = "ProductOfferingPrice";
	private static final String REFERRED_TYPE_PRODUCT_SPECIFICATION = "ProductSpecification";
	private static final String REFERRED_TYPE_RESOURCE_CANDIDATE = "ResourceCandidate";
	private static final String REFERRED_TYPE_RESOURCE_SPECIFICATION = "ResourceSpecification";
	private static final String REFERRED_TYPE_SERVICE_CANDIDATE = "ServiceCandidate";
	private static final String REFERRED_TYPE_SLA = "ServiceLevelAgreement";
	// written into each reference's name, so a read-back tells the patched payload from the created one
	private static final String CREATE = "create";
	private static final String PATCH = "patch";
	// first position of a list's two external ids; a patch from REPLACING_POSITION swaps every id of the list
	private static final int CREATED_POSITION = 0;
	private static final int REPLACING_POSITION = 2;
	// the ProductOffering reference attributes; each list is written with two entries
	private static final List<String> LIST_REFERENCES = List.of("agreement", "bundledProductOffering", "category",
			"channel", "marketSegment", "place", "productOfferingPrice", "productOfferingRelationship");
	private static final List<String> SINGLE_REFERENCES = List.of("productSpecification", "resourceCandidate",
			"serviceCandidate", "serviceLevelAgreement");
	private static final List<String> ALL_REFERENCES = Stream.concat(LIST_REFERENCES.stream(),
			SINGLE_REFERENCES.stream()).toList();
	// TmForumExceptionHandler's INVALID_RELATIONSHIP message, telling a failed reference check from other 400s
	private static final String UNRESOLVED_REFERENCE_MESSAGE = "At least one of the references does not exist";

	/**
	 * How a category reference in a test case is built; resolved against the entities created by the test.
	 */
	enum RefKind {
		// an existing local category, referenced without href
		LOCAL,
		// a non-existent id with an href on a host configured as external
		EXTERNAL,
		// a non-existent id without href
		MISSING,
		// a non-existent id with an href naming general.serverHost
		MISSING_SERVER_HOST,
		// a non-existent id with an href naming a references.local-hosts entry
		MISSING_LOCAL_ALIAS,
		// the id of a local product specification with an href on the external host (the external type guard)
		EXTERNAL_WRONG_TYPE,
		// an existing local category with an href on the external host (the type guard resolves it locally)
		LOCAL_FOREIGN_HREF,
		// a non-existent id with an href on a host that is not configured as external
		MISSING_UNLISTED_HOST,
		// an id that is not a URI, with an href on the external host
		NON_URI_ID
	}

	private final ProductOfferingApiTestClient productOfferingApiTestClient;
	private final CatalogApiTestClient catalogApiTestClient;
	private final CategoryApiTestClient categoryApiTestClient;
	private final ProductSpecificationApiTestClient productSpecificationApiTestClient;
	private final GeneralProperties generalProperties;
	private final Environment environment;
	private final BrokerRequestRecorder brokerRequestRecorder;

	// cleanUp only removes product offerings
	private final List<String> createdCatalogs = new ArrayList<>();
	private final List<String> createdCategories = new ArrayList<>();
	private final List<String> createdProductSpecifications = new ArrayList<>();

	public ExternalReferenceIT(EntitiesApiClient entitiesApiClient, ObjectMapper objectMapper,
							   GeneralProperties generalProperties,
							   ProductOfferingApiTestClient productOfferingApiTestClient,
							   CatalogApiTestClient catalogApiTestClient,
							   CategoryApiTestClient categoryApiTestClient,
							   ProductSpecificationApiTestClient productSpecificationApiTestClient,
							   Environment environment, BrokerRequestRecorder brokerRequestRecorder) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.generalProperties = generalProperties;
		this.environment = environment;
		this.brokerRequestRecorder = brokerRequestRecorder;
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
				Arguments.of("An external href must not smuggle in a local entity of another type.",
						List.of(RefKind.EXTERNAL_WRONG_TYPE), HttpStatus.BAD_REQUEST),
				Arguments.of("An external href on an existing local category of the accepted type is accepted.",
						List.of(RefKind.LOCAL_FOREIGN_HREF), HttpStatus.CREATED),
				Arguments.of("An href to a host that is not configured as external keeps the category local.",
						List.of(RefKind.MISSING_UNLISTED_HOST), HttpStatus.BAD_REQUEST),
				Arguments.of("An external id that is not a URI is rejected.", List.of(RefKind.NON_URI_ID),
						HttpStatus.BAD_REQUEST));
	}

	// references are read from their stored properties (ticket 88), so a GET must not fetch any referenced entity
	@BrokerOnly
	@Test
	public void retrieveProductOfferingDoesNotFetchReferences() throws Exception {
		// keep the external refs: a just-created local target sits in the entities cache, so resolving it would not
		// reach the broker either, and only the external ids make this test fail when reads resolve again
		List<CategoryRefVO> categories = categoryRefs(
				List.of(RefKind.LOCAL, RefKind.LOCAL, RefKind.EXTERNAL, RefKind.EXTERNAL));
		String productSpecificationId = createProductSpecification();
		ServiceCandidateRefVO serviceCandidate = serviceCandidateRef(FAN_OUT_STAGE);
		ProductOfferingCreateVO createVO = productOfferingCreate().category(categories)
				.productSpecification(new ProductSpecificationRefVO().id(productSpecificationId))
				.serviceCandidate(serviceCandidate);
		HttpResponse<ProductOfferingVO> createResponse = callAndCatch(
				() -> productOfferingApiTestClient.createProductOffering(null, createVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), "The product offering should have been created.");
		String id = createResponse.body().getId();
		List<String> referencedIds = new ArrayList<>(categories.stream().map(CategoryRefVO::getId).toList());
		referencedIds.add(productSpecificationId);
		referencedIds.add(serviceCandidate.getId());

		assertFalse(brokerRequestRecorder.requests().isEmpty(), "The recorder should have seen the create's requests.");
		brokerRequestRecorder.clear();
		ProductOfferingVO readBack = retrieve(id);
		List<String> requests = brokerRequestRecorder.requests().stream()
				.map(uri -> URLDecoder.decode(uri.toString(), StandardCharsets.UTF_8))
				.toList();

		assertEquals(categories.size(), readBack.getCategory().size(), "Every category should be read back.");
		for (String referencedId : referencedIds) {
			assertTrue(requests.stream().noneMatch(request -> request.contains(referencedId)),
					"The referenced entity " + referencedId + " must not be fetched: " + requests);
		}
		// createEntity puts the offering into the entities cache, so the GET may not reach the broker at all
		assertTrue(requests.stream().filter(request -> request.contains(id)).count() <= 1,
				"The offering should be retrieved at most once: " + requests);
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
		CategoryRefVO externalCategory = externalCategoryRef(CREATED_POSITION);
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

	// the patch keeps the ids, since append mode keeps the instances a patch omits (see below)
	@Test
	public void externalReferencePayloadsRoundTrip() throws Exception {
		String id = createWithAllReferences(createWithAllReferencesVO());
		ProductOfferingUpdateVO updateVO = updateWithAllReferences(CREATED_POSITION);
		HttpResponse<ProductOfferingVO> patchResponse = callAndCatch(
				() -> productOfferingApiTestClient.patchProductOffering(null, id, updateVO));
		assertEquals(HttpStatus.OK, patchResponse.getStatus(),
				"A product offering should be patchable with external references.");
		assertPayloads(updateVO, patchResponse.body(), ALL_REFERENCES, "patch response");
		assertPayloads(updateVO, retrieve(id), ALL_REFERENCES, "retrieval after patch");
	}

	// Known defect (ticket 88): on Orion-LD without replaceOnUpdate, the production default and forced for every IT by
	// the root pom's failsafe argLine, a PATCH merges a reference list by datasetId and keeps the instances it omits.
	// Scorpio replaces all instances of an appended attribute, so it drops them. This pins
	// that behaviour, so a fix (or the PostgreSQL adapter of ticket 6) has to update the expectation on purpose.
	// The created entries survive next to the patched ones because a RefEntity's datasetId is its id (@DatasetId),
	// not its list position, so new ids are new instances.
	@Test
	public void patchReplacingTheIdsOfAReferenceList() throws Exception {
		ProductOfferingCreateVO createVO = createWithAllReferencesVO();
		String id = createWithAllReferences(createVO);
		ProductOfferingUpdateVO updateVO = updateWithAllReferences(REPLACING_POSITION);
		HttpResponse<ProductOfferingVO> patchResponse = callAndCatch(
				() -> productOfferingApiTestClient.patchProductOffering(null, id, updateVO));
		assertEquals(HttpStatus.OK, patchResponse.getStatus(),
				"A product offering should be patchable with external references.");

		ProductOfferingVO readBack = retrieve(id);
		JsonNode created = objectMapper.valueToTree(createVO);
		JsonNode patched = objectMapper.valueToTree(updateVO);
		boolean keepsCreated = !generalProperties.isReplaceOnUpdate()
				&& environment.getActiveNames().contains(ORION_LD_ENVIRONMENT);
		assertAll(LIST_REFERENCES.stream().map(attribute -> () -> {
			Map<JsonNode, Long> expected = new HashMap<>(multiset(patched.get(attribute)));
			if (keepsCreated) {
				multiset(created.get(attribute)).forEach((entry, count) -> expected.merge(entry, count, Long::sum));
			}
			assertEquals(expected, multiset(objectMapper.valueToTree(readBack).get(attribute)),
					String.format("A patch swapping the ids of %s should %s the created entries.", attribute,
							keepsCreated ? "(known defect) keep" : "drop"));
		}));
	}

	// ResourceSpecificationRef.version is stored since ticket 88; before, a posted version was dropped
	@Test
	public void externalResourceSpecificationsRoundTrip() throws Exception {
		List<ResourceSpecificationRefVO> created = List.of(resourceSpecificationRef(CREATE, CREATED_POSITION),
				resourceSpecificationRef(CREATE, CREATED_POSITION + 1));
		HttpResponse<ProductSpecificationVO> createResponse = callAndCatch(
				() -> productSpecificationApiTestClient.createProductSpecification(null,
						ProductSpecificationCreateVOTestExample.build().atSchemaLocation(null)
								.targetProductSchema(null)
								.productSpecCharacteristic(null)
								.resourceSpecification(created)));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"A product specification with external resource specifications should be created.");
		String id = createResponse.body().getId();
		createdProductSpecifications.add(id);
		assertResourceSpecifications(created, createResponse.body(), "create response");
		assertResourceSpecifications(created, retrieveProductSpecification(id), "retrieval after create");

		// same ids, so the patch updates the stored instances on both brokers
		List<ResourceSpecificationRefVO> patched = List.of(resourceSpecificationRef(PATCH, CREATED_POSITION),
				resourceSpecificationRef(PATCH, CREATED_POSITION + 1));
		ProductSpecificationUpdateVO updateVO = ProductSpecificationUpdateVOTestExample.build().atSchemaLocation(null)
				.targetProductSchema(null)
				.productSpecCharacteristic(null)
				.resourceSpecification(patched);
		HttpResponse<ProductSpecificationVO> patchResponse = callAndCatch(
				() -> productSpecificationApiTestClient.patchProductSpecification(null, id, updateVO));
		assertEquals(HttpStatus.OK, patchResponse.getStatus(),
				"A product specification should be patchable with external resource specifications.");
		assertResourceSpecifications(patched, patchResponse.body(), "patch response");
		assertResourceSpecifications(patched, retrieveProductSpecification(id), "retrieval after patch");
	}

	private void assertResourceSpecifications(List<ResourceSpecificationRefVO> posted, ProductSpecificationVO readBack,
			String stage) {
		assertEquals(multiset(objectMapper.valueToTree(posted)),
				multiset(objectMapper.valueToTree(readBack.getResourceSpecification())),
				String.format("The posted resourceSpecification should be read back intact in the %s.", stage));
	}

	private ProductSpecificationVO retrieveProductSpecification(String id) throws Exception {
		HttpResponse<ProductSpecificationVO> retrieved = callAndCatch(
				() -> productSpecificationApiTestClient.retrieveProductSpecification(null, id, null));
		assertEquals(HttpStatus.OK, retrieved.getStatus(), "The product specification should be retrievable.");
		return retrieved.body();
	}

	// checks the create response and the read-back
	private String createWithAllReferences(ProductOfferingCreateVO createVO) throws Exception {
		HttpResponse<ProductOfferingVO> createResponse = callAndCatch(
				() -> productOfferingApiTestClient.createProductOffering(null, createVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(),
				"A product offering with external references should be created.");
		String id = createResponse.body().getId();
		assertPayloads(createVO, createResponse.body(), ALL_REFERENCES, "create response");
		assertPayloads(createVO, retrieve(id), ALL_REFERENCES, "retrieval after create");
		return id;
	}

	private ProductOfferingCreateVO createWithAllReferencesVO() {
		return productOfferingCreate()
				.agreement(List.of(agreementRef(CREATE, CREATED_POSITION), agreementRef(CREATE, CREATED_POSITION + 1)))
				.bundledProductOffering(List.of(bundledProductOffering(CREATE, CREATED_POSITION),
						bundledProductOffering(CREATE, CREATED_POSITION + 1)))
				.category(List.of(externalCategoryRef(CREATE, CREATED_POSITION),
						externalCategoryRef(CREATE, CREATED_POSITION + 1)))
				.channel(List.of(channelRef(CREATE, CREATED_POSITION), channelRef(CREATE, CREATED_POSITION + 1)))
				.marketSegment(List.of(marketSegmentRef(CREATE, CREATED_POSITION),
						marketSegmentRef(CREATE, CREATED_POSITION + 1)))
				.place(List.of(placeRef(CREATE, CREATED_POSITION), placeRef(CREATE, CREATED_POSITION + 1)))
				.productOfferingPrice(List.of(productOfferingPriceRef(CREATE, CREATED_POSITION),
						productOfferingPriceRef(CREATE, CREATED_POSITION + 1)))
				.productOfferingRelationship(List.of(productOfferingRelationship(CREATE, CREATED_POSITION),
						productOfferingRelationship(CREATE, CREATED_POSITION + 1)))
				.productSpecification(productSpecificationRef(CREATE))
				.resourceCandidate(resourceCandidateRef(CREATE))
				.serviceCandidate(serviceCandidateRef(CREATE))
				.serviceLevelAgreement(slaRef(CREATE));
	}

	// the lists start at patchPosition; the single references keep their ids
	private static ProductOfferingUpdateVO updateWithAllReferences(int patchPosition) {
		return ProductOfferingUpdateVOTestExample.build().atSchemaLocation(null)
				.agreement(List.of(agreementRef(PATCH, patchPosition), agreementRef(PATCH, patchPosition + 1)))
				.bundledProductOffering(List.of(bundledProductOffering(PATCH, patchPosition),
						bundledProductOffering(PATCH, patchPosition + 1)))
				.category(List.of(externalCategoryRef(PATCH, patchPosition),
						externalCategoryRef(PATCH, patchPosition + 1)))
				.channel(List.of(channelRef(PATCH, patchPosition), channelRef(PATCH, patchPosition + 1)))
				.marketSegment(List.of(marketSegmentRef(PATCH, patchPosition),
						marketSegmentRef(PATCH, patchPosition + 1)))
				.place(List.of(placeRef(PATCH, patchPosition), placeRef(PATCH, patchPosition + 1)))
				.productOfferingPrice(List.of(productOfferingPriceRef(PATCH, patchPosition),
						productOfferingPriceRef(PATCH, patchPosition + 1)))
				.productOfferingRelationship(List.of(productOfferingRelationship(PATCH, patchPosition),
						productOfferingRelationship(PATCH, patchPosition + 1)))
				.productSpecification(productSpecificationRef(PATCH))
				.resourceCandidate(resourceCandidateRef(PATCH))
				.serviceCandidate(serviceCandidateRef(PATCH))
				.serviceLevelAgreement(slaRef(PATCH));
	}

	private void assertPayloads(Object posted, ProductOfferingVO readBack, List<String> attributes, String stage) {
		assertAll(attributes.stream().map(attribute -> () -> assertPayload(posted, readBack, attribute, stage)));
	}

	private void assertPayload(Object posted, ProductOfferingVO readBack, String attribute, String stage) {
		JsonNode expected = objectMapper.valueToTree(posted).get(attribute);
		assertNotNull(expected, "The request should carry " + attribute);
		assertEquals(multiset(expected), multiset(objectMapper.valueToTree(readBack).get(attribute)),
				String.format("The posted %s should be read back intact in the %s.", attribute, stage));
	}

	private static String externalId(String type, int position) {
		return String.format("urn:ngsi-ld:%s:remote-%d", type, position);
	}

	private static String remoteName(String stage) {
		return String.format("%s (%s)", REMOTE_NAME, stage);
	}

	private static String externalHref(String path, String id) {
		return EXTERNAL_BASE_URL + path + id;
	}

	private static AgreementRefVO agreementRef(String stage, int position) {
		String id = externalId(AGREEMENT_ID_TYPE, position);
		return new AgreementRefVO().id(id).href(URI.create(externalHref(AGREEMENT_PATH, id))).name(remoteName(stage))
				.atReferredType(REFERRED_TYPE_AGREEMENT);
	}

	private static BundledProductOfferingVO bundledProductOffering(String stage, int position) {
		String id = externalId(PRODUCT_OFFERING_ID_TYPE, position);
		return new BundledProductOfferingVO().id(id).href(externalHref(PRODUCT_OFFERING_PATH, id)).name(remoteName(stage))
				.lifecycleStatus(REMOTE_LIFECYCLE_STATUS)
				.bundledProductOfferingOption(new BundledProductOfferingOptionVO()
						.numberRelOfferDefault(position + 1)
						.numberRelOfferLowerLimit(position)
						.numberRelOfferUpperLimit(position + 2));
	}

	private static CategoryRefVO externalCategoryRef(String stage, int position) {
		String id = externalId(CATEGORY_ID_TYPE, position);
		return new CategoryRefVO().id(id).href(URI.create(externalHref(CATEGORY_PATH, id))).name(remoteName(stage))
				.version(REMOTE_VERSION).atReferredType(REFERRED_TYPE_CATEGORY);
	}

	private static ChannelRefVO channelRef(String stage, int position) {
		String id = externalId(CHANNEL_ID_TYPE, position);
		return new ChannelRefVO().id(id).href(URI.create(externalHref(CHANNEL_PATH, id))).name(remoteName(stage))
				.atReferredType(REFERRED_TYPE_CHANNEL);
	}

	private static MarketSegmentRefVO marketSegmentRef(String stage, int position) {
		String id = externalId(MARKET_SEGMENT_ID_TYPE, position);
		return new MarketSegmentRefVO().id(id).href(externalHref(MARKET_SEGMENT_PATH, id)).name(remoteName(stage))
				.atReferredType(REFERRED_TYPE_MARKET_SEGMENT);
	}

	private static PlaceRefVO placeRef(String stage, int position) {
		String id = externalId(PLACE_ID_TYPE, position);
		return new PlaceRefVO().id(id).href(URI.create(externalHref(PLACE_PATH, id))).name(remoteName(stage))
				.atReferredType(REFERRED_TYPE_PLACE);
	}

	private static ProductOfferingPriceRefOrValueVO productOfferingPriceRef(String stage, int position) {
		String id = externalId(PRODUCT_OFFERING_PRICE_ID_TYPE, position);
		return new ProductOfferingPriceRefOrValueVO().id(id).href(URI.create(externalHref(PRODUCT_OFFERING_PRICE_PATH, id)))
				.name(remoteName(stage)).atReferredType(REFERRED_TYPE_PRODUCT_OFFERING_PRICE);
	}

	private static ProductOfferingRelationshipVO productOfferingRelationship(String stage, int position) {
		String id = externalId(PRODUCT_OFFERING_ID_TYPE, position);
		return new ProductOfferingRelationshipVO().id(id).href(URI.create(externalHref(PRODUCT_OFFERING_PATH, id)))
				.name(remoteName(stage)).relationshipType(REMOTE_RELATIONSHIP_TYPE).role(REMOTE_ROLE)
				.validFor(new TimePeriodVO().startDateTime(REMOTE_VALID_FROM))
				.atReferredType(REFERRED_TYPE_PRODUCT_OFFERING);
	}

	private static ProductSpecificationRefVO productSpecificationRef(String stage) {
		String id = externalId(PRODUCT_SPECIFICATION_ID_TYPE, CREATED_POSITION);
		return new ProductSpecificationRefVO().id(id).href(URI.create(externalHref(PRODUCT_SPECIFICATION_PATH, id)))
				.name(remoteName(stage)).version(REMOTE_VERSION)
				.targetProductSchema(new TargetProductSchemaVO().atType(REMOTE_SCHEMA_TYPE)
						.atSchemaLocation(URI.create(EXTERNAL_BASE_URL + REMOTE_SCHEMA_PATH)))
				.atReferredType(REFERRED_TYPE_PRODUCT_SPECIFICATION);
	}

	private static ResourceCandidateRefVO resourceCandidateRef(String stage) {
		String id = externalId(RESOURCE_CANDIDATE_ID_TYPE, CREATED_POSITION);
		return new ResourceCandidateRefVO().id(id).href(URI.create(externalHref(RESOURCE_CANDIDATE_PATH, id)))
				.name(remoteName(stage)).version(REMOTE_VERSION).atReferredType(REFERRED_TYPE_RESOURCE_CANDIDATE);
	}

	private static ResourceSpecificationRefVO resourceSpecificationRef(String stage, int position) {
		String id = externalId(RESOURCE_SPECIFICATION_ID_TYPE, position);
		return new ResourceSpecificationRefVO().id(id).href(URI.create(externalHref(RESOURCE_SPECIFICATION_PATH, id)))
				.name(remoteName(stage)).version(REMOTE_VERSION).atReferredType(REFERRED_TYPE_RESOURCE_SPECIFICATION);
	}

	private static ServiceCandidateRefVO serviceCandidateRef(String stage) {
		String id = externalId(SERVICE_CANDIDATE_ID_TYPE, CREATED_POSITION);
		return new ServiceCandidateRefVO().id(id).href(URI.create(externalHref(SERVICE_CANDIDATE_PATH, id)))
				.name(remoteName(stage)).version(REMOTE_VERSION).atReferredType(REFERRED_TYPE_SERVICE_CANDIDATE);
	}

	private static SLARefVO slaRef(String stage) {
		String id = externalId(SLA_ID_TYPE, CREATED_POSITION);
		return new SLARefVO().id(id).href(externalHref(SLA_PATH, id)).name(remoteName(stage))
				.atReferredType(REFERRED_TYPE_SLA);
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
		// ProductOffering.category is read from the relationship's own properties (ticket 88), so the whole posted
		// reference round-trips
		assertEquals(multiset(objectMapper.valueToTree(expected)), multiset(objectMapper.valueToTree(readBack)),
				message);
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
			case MISSING_UNLISTED_HOST -> categoryRefVO(missingId,
					URI.create(UNLISTED_BASE_URL + CATEGORY_PATH + missingId));
			// external deployments may use plain ids; the broker only accepts URIs, so such a reference is rejected
			case NON_URI_ID -> categoryRefVO(NON_URI_ID_VALUE + position,
					URI.create(EXTERNAL_BASE_URL + CATEGORY_PATH + NON_URI_ID_VALUE + position));
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
