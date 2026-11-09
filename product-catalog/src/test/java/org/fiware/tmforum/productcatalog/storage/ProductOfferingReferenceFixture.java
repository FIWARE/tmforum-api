package org.fiware.tmforum.productcatalog.storage;

import org.fiware.tmforum.product.Category;
import org.fiware.tmforum.product.CategoryRef;
import org.fiware.tmforum.product.ChannelRef;
import org.fiware.tmforum.product.ProductOffering;
import org.fiware.tmforum.storage.contract.ReferenceFixture;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Product offerings referencing absent categories ({@code version} is the {@link CategoryRef} extra) and channels;
 * the update replaces only the categories. Both attributes resolved their targets before ticket 88.
 */
class ProductOfferingReferenceFixture implements ReferenceFixture<ProductOffering> {

	private static final String CATEGORY_ATTRIBUTE = "category";
	private static final String CHANNEL_ATTRIBUTE = "channel";
	private static final String CATEGORY_ID_FORMAT = "urn:ngsi-ld:" + Category.TYPE_CATEGORY + ":%s";
	private static final String CHANNEL_ID_FORMAT = "urn:ngsi-ld:channel:%s";
	private static final String HREF_FORMAT = "https://external.example.org/%s";
	private static final String CATEGORY_REFERRED_TYPE = "Category";
	private static final String CHANNEL_REFERRED_TYPE = "Channel";
	private static final String CATEGORY_NAME = "external-category";
	private static final String REPLACED_CATEGORY_NAME = "replaced-external-category";
	private static final String CATEGORY_VERSION = "1.0";
	private static final String REPLACED_CATEGORY_VERSION = "2.0";
	private static final String CHANNEL_NAME = "external-channel";

	@Override
	public Class<ProductOffering> entityClass() {
		return ProductOffering.class;
	}

	@Override
	public String storedType() {
		return ProductOffering.TYPE_PRODUCT_OFFERING;
	}

	@Override
	public ProductOffering newEntity(URI id) {
		ProductOffering offering = new ProductOffering(id.toString());
		offering.setCategory(List.of(categoryRef(null, CATEGORY_VERSION),
				categoryRef(CATEGORY_NAME, CATEGORY_VERSION)));
		offering.setChannel(List.of(channelRef(), channelRef()));
		return offering;
	}

	@Override
	public URI id(ProductOffering entity) {
		return entity.getId();
	}

	@Override
	public ProductOffering withOneReferenceReplaced(URI id) {
		ProductOffering offering = new ProductOffering(id.toString());
		offering.setCategory(List.of(categoryRef(REPLACED_CATEGORY_NAME, REPLACED_CATEGORY_VERSION)));
		return offering;
	}

	@Override
	public Map<String, List<?>> references(ProductOffering entity) {
		Map<String, List<?>> references = new LinkedHashMap<>();
		if (entity.getCategory() != null) {
			references.put(CATEGORY_ATTRIBUTE, entity.getCategory().stream().map(CategoryPayload::of).toList());
		}
		if (entity.getChannel() != null) {
			references.put(CHANNEL_ATTRIBUTE, entity.getChannel().stream().map(ChannelPayload::of).toList());
		}
		return references;
	}

	@Override
	public URI targetId(ProductOffering entity) {
		return entity.getCategory().getFirst().getId();
	}

	@Override
	public Object target(URI id, String name) {
		Category category = new Category(id.toString());
		category.setName(name);
		return category;
	}

	// a null name leaves the field out, for the reference the no-resolve case points a target at
	private static CategoryRef categoryRef(String name, String version) {
		CategoryRef categoryRef = new CategoryRef(String.format(CATEGORY_ID_FORMAT, UUID.randomUUID()));
		categoryRef.setHref(URI.create(String.format(HREF_FORMAT, categoryRef.getId())));
		categoryRef.setName(name);
		categoryRef.setAtReferredType(CATEGORY_REFERRED_TYPE);
		categoryRef.setVersion(version);
		return categoryRef;
	}

	private static ChannelRef channelRef() {
		ChannelRef channelRef = new ChannelRef(String.format(CHANNEL_ID_FORMAT, UUID.randomUUID()));
		channelRef.setHref(URI.create(String.format(HREF_FORMAT, channelRef.getId())));
		channelRef.setName(CHANNEL_NAME);
		channelRef.setAtReferredType(CHANNEL_REFERRED_TYPE);
		return channelRef;
	}

	private record CategoryPayload(URI id, URI href, String name, String referredType, String version) {
		static CategoryPayload of(CategoryRef ref) {
			return new CategoryPayload(ref.getId(), ref.getHref(), ref.getName(), ref.getAtReferredType(),
					ref.getVersion());
		}
	}

	private record ChannelPayload(URI id, URI href, String name, String referredType) {
		static ChannelPayload of(ChannelRef ref) {
			return new ChannelPayload(ref.getId(), ref.getHref(), ref.getName(), ref.getAtReferredType());
		}
	}
}
