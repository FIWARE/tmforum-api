package org.fiware.tmforum.productcatalog.storage;

import org.fiware.tmforum.product.Category;
import org.fiware.tmforum.storage.contract.EntityFixture;

import java.net.URI;

/**
 * Categories, the second stored type of the port contract.
 */
class CategoryFixture implements EntityFixture<Category> {

	private static final String NAME = "contract-category";

	@Override
	public Class<Category> entityClass() {
		return Category.class;
	}

	@Override
	public String storedType() {
		return Category.TYPE_CATEGORY;
	}

	@Override
	public Category newEntity(URI id) {
		Category category = new Category(id.toString());
		category.setName(NAME);
		return category;
	}

	@Override
	public URI id(Category entity) {
		return entity.getId();
	}
}
