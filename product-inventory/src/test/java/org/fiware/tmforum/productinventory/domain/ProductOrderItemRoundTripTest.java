package org.fiware.tmforum.productinventory.domain;

import org.fiware.tmforum.common.test.NgsiLdRoundTrip;
import org.fiware.tmforum.product.Product;
import org.fiware.tmforum.product.RelatedProductOrderItemRef;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins that a stored {@link Product#getProductOrderItem()} reads back as written. {@link RelatedProductOrderItemRef}
 * used to lack a String constructor, so the read failed (ticket 88).
 */
class ProductOrderItemRoundTripTest {

	private static final String PRODUCT_ID = "urn:ngsi-ld:product:p";
	private static final String FIRST_ORDER_ID = "urn:ngsi-ld:product-order:o1";
	private static final String SECOND_ORDER_ID = "urn:ngsi-ld:product-order:o2";

	@ParameterizedTest(name = "{0}")
	@MethodSource("orderItems")
	public void productOrderItemsSurviveTheRoundTrip(String description, List<RelatedProductOrderItemRef> orderItems) {
		Product product = new Product(PRODUCT_ID);
		product.setProductOrderItem(orderItems);

		Product read = NgsiLdRoundTrip.roundTrip(product, Product.class);

		assertEquals(NgsiLdRoundTrip.toTree(orderItems), NgsiLdRoundTrip.toTree(read.getProductOrderItem()),
				description);
	}

	private static Stream<Arguments> orderItems() {
		return Stream.of(
				Arguments.of("id only", List.of(idOnly(FIRST_ORDER_ID))),
				Arguments.of("every field", List.of(full(FIRST_ORDER_ID, "item-1"))),
				Arguments.of("two items", List.of(full(FIRST_ORDER_ID, "item-1"), full(SECOND_ORDER_ID, "item-2"))));
	}

	private static RelatedProductOrderItemRef idOnly(String orderId) {
		return new RelatedProductOrderItemRef(orderId);
	}

	private static RelatedProductOrderItemRef full(String orderId, String orderItemId) {
		RelatedProductOrderItemRef orderItem = new RelatedProductOrderItemRef(orderId);
		orderItem.setProductOrderHref(URI.create("https://external.org/product-order/" + orderItemId));
		orderItem.setOrderItemId(orderItemId);
		orderItem.setOrderItemAction("add");
		orderItem.setRole("buyer");
		orderItem.setAtReferredType("ProductOrderItem");
		return orderItem;
	}
}
