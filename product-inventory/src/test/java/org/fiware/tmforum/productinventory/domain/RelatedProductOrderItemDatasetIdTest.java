package org.fiware.tmforum.productinventory.domain;

import org.fiware.tmforum.product.RelatedProductOrderItemRef;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Pins that two items of one product order get distinct datasetIds, which the broker needs to keep both (ticket 88).
 */
class RelatedProductOrderItemDatasetIdTest {

	private static final String ORDER_ID = "urn:ngsi-ld:product-order:o1";

	@ParameterizedTest(name = "{0}")
	@MethodSource("itemIdPairs")
	public void itemsOfOneOrderGetDistinctDatasetIds(String description, String firstItemId, String secondItemId) {
		assertNotEquals(item(firstItemId).getDatasetId(), item(secondItemId).getDatasetId(), description);
	}

	private static Stream<Arguments> itemIdPairs() {
		return Stream.of(
				Arguments.of("different item ids", "item-1", "item-2"),
				Arguments.of("no item id and the former fallback", null, "item"),
				Arguments.of("no item id and the fallback's text", null, "~none"),
				Arguments.of("no item id and an empty item id", null, ""),
				Arguments.of("an item id and its encoded form", "a b", "a+b"));
	}

	private static RelatedProductOrderItemRef item(String orderItemId) {
		RelatedProductOrderItemRef item = new RelatedProductOrderItemRef(ORDER_ID);
		item.setOrderItemId(orderItemId);
		return item;
	}
}
