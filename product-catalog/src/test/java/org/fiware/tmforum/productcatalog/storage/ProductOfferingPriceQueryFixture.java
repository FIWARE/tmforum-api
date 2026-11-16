package org.fiware.tmforum.productcatalog.storage;

import org.fiware.tmforum.common.domain.Money;
import org.fiware.tmforum.common.domain.PlaceRef;
import org.fiware.tmforum.product.ProductOfferingPrice;
import org.fiware.tmforum.storage.contract.QueryFixture;
import org.fiware.tmforum.storage.query.AttributePath;

import java.net.URI;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Product offering prices for the query contract cases, which hold every attribute kind but a plain string list.
 */
class ProductOfferingPriceQueryFixture implements QueryFixture<ProductOfferingPrice> {

	private static final String DESCRIPTION = "contract query price";
	private static final String EXTENSION_NAME = "contractQueryExtension";
	private static final Map<Attribute, AttributePath> PATHS = Map.of(
			Attribute.STRING, declared("name"),
			Attribute.NUMBER, declared("recurringChargePeriodLength"),
			Attribute.BOOLEAN, declared("isBundle"),
			Attribute.DATE_TIME, declared("lastUpdate"),
			Attribute.NESTED_STRING, declared("price", "unit"),
			Attribute.REFERENCE_ID, declared("place", "id"),
			Attribute.EXTENSION, new AttributePath(List.of(EXTENSION_NAME), false));

	@Override
	public Class<ProductOfferingPrice> entityClass() {
		return ProductOfferingPrice.class;
	}

	@Override
	public String storedType() {
		return ProductOfferingPrice.TYPE_PRODUCT_OFFERING_PRICE;
	}

	@Override
	public URI id(ProductOfferingPrice entity) {
		return entity.getId();
	}

	@Override
	public Set<Attribute> attributes() {
		return EnumSet.copyOf(PATHS.keySet());
	}

	@Override
	public AttributePath path(Attribute attribute) {
		return PATHS.get(attribute);
	}

	@Override
	public ProductOfferingPrice newEntity(URI id, Map<Attribute, Object> values) {
		ProductOfferingPrice price = new ProductOfferingPrice(id.toString());
		price.setDescription(DESCRIPTION);
		values.forEach((attribute, value) -> {
			switch (attribute) {
				case STRING -> price.setName((String) value);
				case NUMBER -> price.setRecurringChargePeriodLength((Integer) value);
				case BOOLEAN -> price.setIsBundle((Boolean) value);
				case DATE_TIME -> price.setLastUpdate((Instant) value);
				case NESTED_STRING -> {
					Money money = new Money();
					money.setUnit((String) value);
					price.setPrice(money);
				}
				case REFERENCE_ID -> price.setPlace(((List<?>) value).stream()
						.map(placeId -> new PlaceRef(placeId.toString()))
						.toList());
				case EXTENSION -> price.addAdditionalProperties(EXTENSION_NAME, value);
				default -> throw new IllegalArgumentException("Prices hold no " + attribute);
			}
		});
		return price;
	}

	private static AttributePath declared(String... segments) {
		return new AttributePath(List.of(segments), true);
	}
}
