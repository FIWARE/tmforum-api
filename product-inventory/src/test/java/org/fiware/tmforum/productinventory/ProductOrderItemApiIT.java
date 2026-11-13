package org.fiware.tmforum.productinventory;

import io.github.wistefan.mapping.annotations.MappingEnabled;
import io.micronaut.http.HttpResponse;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.ngsi.api.EntitiesApiClient;
import org.fiware.productinventory.api.ProductApiTestClient;
import org.fiware.productinventory.model.ProductCreateVO;
import org.fiware.productinventory.model.ProductCreateVOTestExample;
import org.fiware.productinventory.model.ProductUpdateVO;
import org.fiware.productinventory.model.ProductUpdateVOTestExample;
import org.fiware.productinventory.model.ProductVO;
import org.fiware.productinventory.model.RelatedProductOrderItemVO;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.notification.TMForumEventHandler;
import org.fiware.tmforum.common.test.AbstractApiIT;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.fiware.tmforum.product.Product;
import org.fiware.tmforum.storage.EntityRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Round-trip of a product's productOrderItem references to stored product orders through the API. Before ticket 88
 * {@code RelatedProductOrderItemRef} had no String constructor, so a stored item could not be read back.
 */
@MicronautTest(packages = {"org.fiware.tmforum.productinventory"}, contextBuilder = FirstMatchConfigContextBuilder.class)
@Property(name = "references.external-hosts", value = ProductOrderItemApiIT.EXTERNAL_HOST)
public class ProductOrderItemApiIT extends AbstractApiIT {

	static final String EXTERNAL_HOST = "external-tmf.example.org";

	// product orders live in product-ordering-management, so the targets are written through the storage port
	private static final String PRODUCT_ORDER_TYPE = "product-order";
	private static final String FIRST_ORDER_ID = "urn:ngsi-ld:product-order:order-item-api-it-1";
	private static final String SECOND_ORDER_ID = "urn:ngsi-ld:product-order:order-item-api-it-2";
	private static final List<String> ORDER_IDS = List.of(FIRST_ORDER_ID, SECOND_ORDER_ID);
	private static final Order FIRST_ORDER = Order.stored(FIRST_ORDER_ID);
	private static final Order SECOND_ORDER = Order.stored(SECOND_ORDER_ID);
	// held by another deployment's product ordering, so neither stored nor validated here
	private static final String EXTERNAL_ORDER_ID = "urn:ngsi-ld:product-order:order-item-api-it-external";
	private static final Order EXTERNAL_ORDER = new Order(EXTERNAL_ORDER_ID,
			"https://" + EXTERNAL_HOST + "/tmf-api/productOrderingManagement/v4/productOrder/" + EXTERNAL_ORDER_ID);
	private static final String REFERRED_TYPE = "ProductOrderItem";
	private static final String ITEM_ID_PREFIX = "item-";
	// written into the items' action and role, so a read-back tells the patched payload from the created one
	private static final String CREATE = "add";
	private static final String PATCH = "modify";

	/**
	 * How much of each item a test case posts.
	 */
	enum Payload {
		// the required productOrderId and orderItemId only
		MINIMAL,
		// every field the domain class stores
		FULL
	}

	/**
	 * A referenced product order. An external order carries the href that classifies it as external in every payload.
	 */
	record Order(String id, String externalHref) {

		static Order stored(String id) {
			return new Order(id, null);
		}

		String fullHref() {
			return externalHref == null ? id : externalHref;
		}
	}

	private final ProductApiTestClient productApiTestClient;
	private final EntityRepository entityRepository;

	public ProductOrderItemApiIT(ProductApiTestClient productApiTestClient, EntitiesApiClient entitiesApiClient,
			EntityRepository entityRepository, ObjectMapper objectMapper, GeneralProperties generalProperties) {
		super(entitiesApiClient, objectMapper, generalProperties);
		this.productApiTestClient = productApiTestClient;
		this.entityRepository = entityRepository;
	}

	/**
	 * Stand-in for product-ordering-management's ProductOrder, which this module does not depend on.
	 */
	@MappingEnabled(entityType = PRODUCT_ORDER_TYPE)
	public static class ProductOrderTarget extends EntityWithId {

		public ProductOrderTarget(String id) {
			super(PRODUCT_ORDER_TYPE, id);
		}
	}

	@MockBean(TMForumEventHandler.class)
	public TMForumEventHandler eventHandler() {
		TMForumEventHandler eventHandler = mock(TMForumEventHandler.class);
		when(eventHandler.handleCreateEvent(any())).thenReturn(Mono.empty());
		when(eventHandler.handleUpdateEvent(any(), any())).thenReturn(Mono.empty());
		return eventHandler;
	}

	@BeforeEach
	public void createProductOrders() {
		removeProductOrders();
		ORDER_IDS.forEach(orderId -> entityRepository.createDomainEntity(new ProductOrderTarget(orderId)).block());
	}

	@AfterEach
	public void removeProductOrders() {
		ORDER_IDS.forEach(orderId -> {
			try {
				entityRepository.deleteDomainEntity(URI.create(orderId)).block();
			} catch (Exception e) {
				// noop - not there yet or already removed
			}
		});
	}

	private static Stream<Arguments> orderItems() {
		return Stream.of(
				Arguments.of("A minimal order item should round-trip.", Payload.MINIMAL, List.of(FIRST_ORDER)),
				Arguments.of("A full order item should round-trip.", Payload.FULL, List.of(FIRST_ORDER)),
				Arguments.of("Minimal items of two orders should round-trip.", Payload.MINIMAL,
						List.of(FIRST_ORDER, SECOND_ORDER)),
				Arguments.of("Full items of two orders should round-trip.", Payload.FULL,
						List.of(FIRST_ORDER, SECOND_ORDER)),
				// the datasetId combines order and item id, so items of the same order do not collide
				Arguments.of("Minimal items of one order should round-trip.", Payload.MINIMAL,
						List.of(FIRST_ORDER, FIRST_ORDER)),
				Arguments.of("Full items of one order should round-trip.", Payload.FULL,
						List.of(FIRST_ORDER, FIRST_ORDER)),
				// the productOrderHref classifies the order as external, so it is not validated against the broker
				Arguments.of("A minimal item of an external order should round-trip.", Payload.MINIMAL,
						List.of(EXTERNAL_ORDER)),
				Arguments.of("A full item of an external order should round-trip.", Payload.FULL,
						List.of(EXTERNAL_ORDER)),
				Arguments.of("Full items of a stored and an external order should round-trip.", Payload.FULL,
						List.of(FIRST_ORDER, EXTERNAL_ORDER)));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("orderItems")
	public void productOrderItemsRoundTrip(String message, Payload payload, List<Order> orders) throws Exception {
		List<RelatedProductOrderItemVO> created = items(orders, payload, CREATE);
		ProductCreateVO createVO = ProductCreateVOTestExample.build().atSchemaLocation(null)
				.productSpecification(null)
				.billingAccount(null)
				.productOffering(null)
				.productOrderItem(created);
		HttpResponse<ProductVO> createResponse = callAndCatch(
				() -> productApiTestClient.createProduct(null, createVO));
		assertEquals(HttpStatus.CREATED, createResponse.getStatus(), message);
		String id = createResponse.body().getId();
		assertRoundTrip(created, createResponse.body(), "create response: " + message);
		assertRoundTrip(created, retrieve(id), "retrieval after create: " + message);

		List<RelatedProductOrderItemVO> patched = items(orders, payload, PATCH);
		ProductUpdateVO updateVO = ProductUpdateVOTestExample.build().atSchemaLocation(null)
				.productSpecification(null)
				.billingAccount(null)
				.productOffering(null)
				.productOrderItem(patched);
		HttpResponse<ProductVO> patchResponse = callAndCatch(
				() -> productApiTestClient.patchProduct(null, id, updateVO));
		assertEquals(HttpStatus.OK, patchResponse.getStatus(), message);
		assertRoundTrip(patched, patchResponse.body(), "patch response: " + message);
		assertRoundTrip(patched, retrieve(id), "retrieval after patch: " + message);
	}

	private static List<RelatedProductOrderItemVO> items(List<Order> orders, Payload payload, String stage) {
		return IntStream.range(0, orders.size())
				.mapToObj(index -> item(orders.get(index), ITEM_ID_PREFIX + index, payload, stage))
				.toList();
	}

	private static RelatedProductOrderItemVO item(Order order, String orderItemId, Payload payload, String stage) {
		RelatedProductOrderItemVO item = new RelatedProductOrderItemVO()
				.productOrderId(order.id())
				.orderItemId(orderItemId)
				.productOrderHref(order.externalHref());
		if (payload == Payload.MINIMAL) {
			return item;
		}
		return item
				.productOrderHref(order.fullHref())
				.orderItemAction(stage)
				.role(stage)
				.atReferredType(REFERRED_TYPE);
	}

	// the broker does not keep the order of a list's instances
	private void assertRoundTrip(List<RelatedProductOrderItemVO> posted, ProductVO readBack, String message) {
		assertEquals(multiset(objectMapper.valueToTree(posted)),
				multiset(objectMapper.valueToTree(readBack.getProductOrderItem())), message);
	}

	private ProductVO retrieve(String id) throws Exception {
		HttpResponse<ProductVO> retrieved = callAndCatch(() -> productApiTestClient.retrieveProduct(null, id, null));
		assertEquals(HttpStatus.OK, retrieved.getStatus(), "The product should be retrievable.");
		return retrieved.body();
	}

	@Override
	protected String getEntityType() {
		return Product.TYPE_PRODUCT;
	}
}
