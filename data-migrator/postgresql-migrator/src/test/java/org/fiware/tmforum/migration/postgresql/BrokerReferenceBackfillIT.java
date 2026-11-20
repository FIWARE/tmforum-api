package org.fiware.tmforum.migration.postgresql;

import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.tmforum.common.CommonConstants;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.mapping.ForbiddenCharacterEscaper;
import org.fiware.tmforum.common.test.BrokerOnly;
import org.fiware.tmforum.common.test.NgsiLdTestEntityCleaner;
import org.fiware.tmforum.migration.postgresql.ReferenceBackfillReport.AttributeResult;
import org.fiware.tmforum.product.Category;
import org.fiware.tmforum.product.CategoryRef;
import org.fiware.tmforum.product.Product;
import org.fiware.tmforum.product.ProductOffering;
import org.fiware.tmforum.product.RelatedProductOrderItemRef;
import org.fiware.tmforum.resource.ResourceCandidate;
import org.fiware.tmforum.storage.EntityRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs {@code backfill-references --target broker} against the test cluster's broker, seeded with pre-ticket-88
 * id-only references and a product in the old {@code productOrderItem} datasetId format.
 */
@BrokerOnly
class BrokerReferenceBackfillIT {

	/** Set by the failsafe configuration from the build's {@code -Dbroker}. */
	private static final String BROKER_PROPERTY = "broker";
	private static final String ENTITIES_PATH = "/ngsi-ld/v1/entities";
	private static final String SYS_ATTRS_QUERY = "?options=sysAttrs";
	private static final String JSON = "application/json";
	private static final int HTTP_CREATED = 201;
	private static final int PAGE_SIZE = 100;
	private static final List<String> SEEDED_TYPES = List.of(ProductOffering.TYPE_PRODUCT_OFFERING,
			Product.TYPE_PRODUCT, Category.TYPE_CATEGORY, ResourceCandidate.TYPE_RESOURCE_CANDIDATE);

	private static final String CANDIDATE_LABEL = ProductOffering.TYPE_PRODUCT_OFFERING + ".resourceCandidate";
	private static final String CATEGORY_LABEL = ProductOffering.TYPE_PRODUCT_OFFERING + ".category";
	private static final String ORDER_ITEM_LABEL = Product.TYPE_PRODUCT + ".productOrderItem"
			+ BrokerReferenceBackfill.DATASET_ID_REWRITE_SUFFIX;
	private static final String CLIENT_NAME = "client name";
	private static final String ITEM_A = "item-a";
	private static final String ITEM_B = "item-b";

	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final JsonMapper jsonMapper = JsonMapper.builder().build();
	private MigrationContexts contexts;

	@BeforeEach
	void startSource() {
		contexts = new MigrationContexts();
		new CommandLine(contexts).parseArgs(brokerTypeOption());
		contexts.startSource();
		deleteSeededEntities();
	}

	@AfterEach
	void cleanUp() {
		deleteSeededEntities();
		contexts.close();
		ForbiddenCharacterEscaper.setEnabled(true);
	}

	@Test
	void fillsPre88ReferencesAndRewritesOrderItemDatasetIds() throws Exception {
		ResourceCandidate candidate = new ResourceCandidate(urn(ResourceCandidate.TYPE_RESOURCE_CANDIDATE).toString());
		candidate.setName("candidate");
		candidate.setHref(URI.create("http://candidate.example.org"));
		candidate.setVersion("1.0");
		Category categoryOne = category("category one", "2.0");
		Category categoryTwo = category("category two", "3.0");
		for (Object target : List.of(candidate, categoryOne, categoryTwo)) {
			repository().createDomainEntity(target).block();
		}

		// written before ticket 88: id-only references, and a list instance per datasetId
		URI offeringId = urn(ProductOffering.TYPE_PRODUCT_OFFERING);
		createRawEntity(String.format("""
				{"id": "%1$s", "type": "%2$s", "name": {"type": "Property", "value": "offering"},
				 "resourceCandidate": {"type": "Relationship", "object": "%3$s"},
				 "category": [
				  {"type": "Relationship", "object": "%4$s", "datasetId": "%4$s"},
				  {"type": "Relationship", "object": "%5$s", "datasetId": "%5$s",
				   "name": {"type": "Property", "value": "%6$s"}}]}""",
				offeringId, ProductOffering.TYPE_PRODUCT_OFFERING, candidate.getId(), categoryOne.getId(),
				categoryTwo.getId(), CLIENT_NAME));
		// the old productOrderItem datasetId is the product order id
		URI productId = urn(Product.TYPE_PRODUCT);
		URI orderOne = urn("product-order");
		URI orderTwo = urn("product-order");
		createRawEntity(String.format("""
				{"id": "%1$s", "type": "%2$s", "name": {"type": "Property", "value": "product"},
				 "productOrderItem": [
				  {"type": "Relationship", "object": "%3$s", "datasetId": "%3$s",
				   "orderItemId": {"type": "Property", "value": "%5$s"}},
				  {"type": "Relationship", "object": "%4$s", "datasetId": "%4$s",
				   "orderItemId": {"type": "Property", "value": "%6$s"}}]}""",
				productId, Product.TYPE_PRODUCT, orderOne, orderTwo, ITEM_A, ITEM_B));
		Map<URI, String> seededModifiedAt = modifiedAt(offeringId, productId);

		ReferenceBackfillReport dryRun = backfill().run(true);
		assertTrue(dryRun.succeeded(), report(dryRun));
		assertEquals(seededModifiedAt, modifiedAt(offeringId, productId), "A dry run must not write.");
		assertExpectedChanges(dryRun);

		runCommand();

		ProductOffering offering = repository().get(offeringId, ProductOffering.class).block();
		assertEquals("candidate", offering.getResourceCandidate().getName());
		assertEquals(URI.create("http://candidate.example.org"), offering.getResourceCandidate().getHref());
		assertEquals("1.0", offering.getResourceCandidate().getVersion());
		Map<URI, CategoryRef> categories = offering.getCategory().stream()
				.collect(Collectors.toMap(CategoryRef::getEntityId, ref -> ref));
		assertEquals(2, categories.size(), "Each category must be stored once.");
		assertEquals("category one", categories.get(categoryOne.getId()).getName());
		assertEquals("2.0", categories.get(categoryOne.getId()).getVersion());
		assertEquals(CLIENT_NAME, categories.get(categoryTwo.getId()).getName(), "A client-sent field is kept.");
		assertEquals("3.0", categories.get(categoryTwo.getId()).getVersion());
		// Category has no href, so none is filled
		assertNull(categories.get(categoryOne.getId()).getHref());

		assertEquals(new TreeSet<>(Set.of(orderOne + ":" + ITEM_A, orderTwo + ":" + ITEM_B)),
				datasetIds(brokerEntity(productId).path("productOrderItem")));
		Product product = repository().get(productId, Product.class).block();
		assertEquals(Set.of(ITEM_A, ITEM_B), product.getProductOrderItem().stream()
				.map(RelatedProductOrderItemRef::getOrderItemId).collect(Collectors.toSet()));

		Map<URI, String> backfilledModifiedAt = modifiedAt(offeringId, productId);
		ReferenceBackfillReport rerun = backfill().run(false);
		assertTrue(rerun.succeeded(), report(rerun));
		assertEquals(new AttributeResult(CANDIDATE_LABEL, 0, 0, 0), result(rerun, CANDIDATE_LABEL));
		assertEquals(new AttributeResult(CATEGORY_LABEL, 0, 0, 0), result(rerun, CATEGORY_LABEL));
		assertEquals(new AttributeResult(ORDER_ITEM_LABEL, 0, 0, 0), result(rerun, ORDER_ITEM_LABEL));
		assertEquals(backfilledModifiedAt, modifiedAt(offeringId, productId), "A re-run must not write.");
	}

	private static void assertExpectedChanges(ReferenceBackfillReport report) {
		assertEquals(new AttributeResult(CANDIDATE_LABEL, 1, 0, 1), result(report, CANDIDATE_LABEL));
		assertEquals(new AttributeResult(CATEGORY_LABEL, 2, 0, 1), result(report, CATEGORY_LABEL));
		assertEquals(new AttributeResult(ORDER_ITEM_LABEL, 2, 0, 1), result(report, ORDER_ITEM_LABEL));
	}

	private Category category(String name, String version) {
		Category category = new Category(urn(Category.TYPE_CATEGORY).toString());
		category.setName(name);
		category.setVersion(version);
		return category;
	}

	private BrokerReferenceBackfill backfill() {
		return new BrokerReferenceBackfill(contexts.source(), ReferenceBackfillCatalog.load(),
				EntityTypeRegistry.scan(), PAGE_SIZE);
	}

	private void runCommand() {
		StringWriter out = new StringWriter();
		StringWriter err = new StringWriter();
		int exitCode = new CommandLine(new Application())
				.setOut(new PrintWriter(out))
				.setErr(new PrintWriter(err))
				.execute("backfill-references", "--target", "broker", brokerTypeOption());
		assertEquals(CommandLine.ExitCode.OK, exitCode, out + "\n" + err);
	}

	private EntityRepository repository() {
		return contexts.source().getBean(EntityRepository.class);
	}

	private void deleteSeededEntities() {
		NgsiLdTestEntityCleaner cleaner = new NgsiLdTestEntityCleaner(
				contexts.source().getBean(EntitiesApiClient.class), contexts.source().getBean(GeneralProperties.class));
		SEEDED_TYPES.forEach(cleaner::deleteAll);
	}

	private void createRawEntity(String json) throws IOException, InterruptedException {
		HttpResponse<String> response = httpClient.send(brokerRequest(ENTITIES_PATH)
						.header("Content-Type", JSON)
						.POST(HttpRequest.BodyPublishers.ofString(json))
						.build(),
				HttpResponse.BodyHandlers.ofString());
		assertEquals(HTTP_CREATED, response.statusCode(), response.body());
	}

	private Map<URI, String> modifiedAt(URI... ids) throws IOException, InterruptedException {
		Map<URI, String> modifiedAt = new HashMap<>();
		for (URI id : ids) {
			modifiedAt.put(id, brokerEntity(id).path("modifiedAt").asString());
		}
		return modifiedAt;
	}

	private JsonNode brokerEntity(URI id) throws IOException, InterruptedException {
		HttpResponse<String> response = httpClient.send(
				brokerRequest(ENTITIES_PATH + "/" + id + SYS_ATTRS_QUERY).GET().build(),
				HttpResponse.BodyHandlers.ofString());
		return jsonMapper.readTree(response.body());
	}

	private HttpRequest.Builder brokerRequest(String path) {
		GeneralProperties properties = contexts.source().getBean(GeneralProperties.class);
		String brokerUrl = contexts.source().getRequiredProperty(MigrationContexts.NGSI_URL_PROPERTY, String.class);
		return HttpRequest.newBuilder(URI.create(brokerUrl + path))
				.header("Accept", JSON)
				.header("Link", String.format(CommonConstants.JSON_LD_CONTEXT_LINK_TEMPLATE, properties.getContextUrl()));
	}

	private static Set<String> datasetIds(JsonNode attribute) {
		Set<String> datasetIds = new TreeSet<>();
		attribute.forEach(instance -> datasetIds.add(instance.path("datasetId").asString()));
		return datasetIds;
	}

	private static AttributeResult result(ReferenceBackfillReport report, String label) {
		return report.attributes().stream()
				.filter(result -> result.attribute().equals(label))
				.findFirst()
				.orElseThrow(() -> new AssertionError(label + " is missing from the report:\n" + report(report)));
	}

	private static String report(ReferenceBackfillReport report) {
		StringWriter out = new StringWriter();
		report.print(new PrintWriter(out));
		return out.toString();
	}

	private static String brokerTypeOption() {
		return "--broker-type=" + System.getProperty(BROKER_PROPERTY, BrokerType.ORION_LD.getEnvironment());
	}

	private static URI urn(String type) {
		return URI.create(String.format("urn:ngsi-ld:%s:%s", type, UUID.randomUUID()));
	}
}
