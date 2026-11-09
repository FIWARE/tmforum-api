package org.fiware.tmforum.productcatalog.storage;

import org.fiware.tmforum.common.domain.RelatedParty;
import org.fiware.tmforum.product.Category;
import org.fiware.tmforum.product.CategoryRef;
import org.fiware.tmforum.productcatalog.domain.Catalog;
import org.fiware.tmforum.storage.contract.ReferenceFixture;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Catalogs referencing absent categories ({@code version} is the {@link CategoryRef} extra) and absent parties
 * ({@code role}); the update replaces only the categories.
 */
class CatalogReferenceFixture implements ReferenceFixture<Catalog> {

	private static final String CATEGORY_ATTRIBUTE = "category";
	private static final String PARTY_ATTRIBUTE = "relatedParty";
	private static final String CATEGORY_ID_FORMAT = "urn:ngsi-ld:" + Category.TYPE_CATEGORY + ":%s";
	private static final String PARTY_ID_FORMAT = "urn:ngsi-ld:individual:%s";
	private static final String HREF_FORMAT = "https://external.example.org/%s";
	private static final String CATEGORY_REFERRED_TYPE = "Category";
	private static final String PARTY_REFERRED_TYPE = "Individual";
	private static final String CATEGORY_NAME = "external-category";
	private static final String REPLACED_CATEGORY_NAME = "replaced-external-category";
	private static final String CATEGORY_VERSION = "1.0";
	private static final String REPLACED_CATEGORY_VERSION = "2.0";
	private static final String PARTY_NAME = "external-party";
	private static final String PARTY_ROLE = "owner";

	@Override
	public Catalog withReferences(URI id) {
		Catalog catalog = new Catalog(id.toString());
		catalog.setCategory(List.of(categoryRef(CATEGORY_NAME, CATEGORY_VERSION),
				categoryRef(CATEGORY_NAME, CATEGORY_VERSION)));
		catalog.setRelatedParty(List.of(relatedParty(), relatedParty()));
		return catalog;
	}

	@Override
	public Catalog withOneReferenceReplaced(URI id) {
		Catalog catalog = new Catalog(id.toString());
		catalog.setCategory(List.of(categoryRef(REPLACED_CATEGORY_NAME, REPLACED_CATEGORY_VERSION)));
		return catalog;
	}

	@Override
	public Map<String, List<?>> references(Catalog entity) {
		Map<String, List<?>> references = new LinkedHashMap<>();
		if (entity.getCategory() != null) {
			references.put(CATEGORY_ATTRIBUTE, entity.getCategory().stream().map(CategoryPayload::of).toList());
		}
		if (entity.getRelatedParty() != null) {
			references.put(PARTY_ATTRIBUTE, entity.getRelatedParty().stream().map(PartyPayload::of).toList());
		}
		return references;
	}

	@Override
	public URI targetId(Catalog entity) {
		return entity.getCategory().getFirst().getId();
	}

	@Override
	public Object target(URI id, String name) {
		Category category = new Category(id.toString());
		category.setName(name);
		return category;
	}

	private static CategoryRef categoryRef(String name, String version) {
		CategoryRef categoryRef = new CategoryRef(String.format(CATEGORY_ID_FORMAT, UUID.randomUUID()));
		categoryRef.setHref(URI.create(String.format(HREF_FORMAT, categoryRef.getId())));
		categoryRef.setName(name);
		categoryRef.setAtReferredType(CATEGORY_REFERRED_TYPE);
		categoryRef.setVersion(version);
		return categoryRef;
	}

	private static RelatedParty relatedParty() {
		RelatedParty relatedParty = new RelatedParty(String.format(PARTY_ID_FORMAT, UUID.randomUUID()));
		relatedParty.setHref(URI.create(String.format(HREF_FORMAT, relatedParty.getId())));
		relatedParty.setName(PARTY_NAME);
		relatedParty.setAtReferredType(PARTY_REFERRED_TYPE);
		relatedParty.setRole(PARTY_ROLE);
		return relatedParty;
	}

	private record CategoryPayload(URI id, URI href, String name, String referredType, String version) {
		static CategoryPayload of(CategoryRef ref) {
			return new CategoryPayload(ref.getId(), ref.getHref(), ref.getName(), ref.getAtReferredType(),
					ref.getVersion());
		}
	}

	private record PartyPayload(URI id, URI href, String name, String referredType, String role) {
		static PartyPayload of(RelatedParty ref) {
			return new PartyPayload(ref.getId(), ref.getHref(), ref.getName(), ref.getAtReferredType(),
					ref.getRole());
		}
	}
}
