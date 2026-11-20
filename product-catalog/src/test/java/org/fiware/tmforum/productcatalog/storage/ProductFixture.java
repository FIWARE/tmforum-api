package org.fiware.tmforum.productcatalog.storage;

import org.fiware.tmforum.product.Product;
import org.fiware.tmforum.storage.contract.UnversionedFixture;

import java.net.URI;

/**
 * Products for the reference contract: {@code Product} has no {@code version} attribute, so it is not versionable.
 */
class ProductFixture implements UnversionedFixture<Product> {

	private static final String NAME = "contract-product";
	private static final String VERSION_ATTRIBUTE = "version";

	@Override
	public Class<Product> entityClass() {
		return Product.class;
	}

	@Override
	public String storedType() {
		return Product.TYPE_PRODUCT;
	}

	@Override
	public Product newEntity(URI id) {
		Product product = new Product(id.toString());
		product.setName(NAME);
		return product;
	}

	@Override
	public URI id(Product entity) {
		return entity.getId();
	}

	@Override
	public Product withExtensionVersion(URI id, String version) {
		Product product = newEntity(id);
		product.addAdditionalProperties(VERSION_ATTRIBUTE, version);
		return product;
	}
}
