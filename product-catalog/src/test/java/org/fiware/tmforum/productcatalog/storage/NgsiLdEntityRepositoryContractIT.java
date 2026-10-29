package org.fiware.tmforum.productcatalog.storage;

import io.micronaut.context.env.Environment;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.fiware.tmforum.product.Category;
import org.fiware.tmforum.productcatalog.domain.Catalog;
import org.fiware.tmforum.storage.EntityRepository;
import org.fiware.tmforum.storage.contract.AbstractEntityRepositoryContractTest;
import org.fiware.tmforum.storage.contract.EntityFixture;
import org.fiware.tmforum.storage.contract.UpdateFixture;

/**
 * Runs the entity port contract against the NGSI-LD adapter and the broker of the IT profile.
 */
@MicronautTest(packages = {"org.fiware.tmforum.productcatalog"}, contextBuilder = FirstMatchConfigContextBuilder.class)
public class NgsiLdEntityRepositoryContractIT extends AbstractEntityRepositoryContractTest<Catalog, Category> {

	private static final String ORION_LD_ENVIRONMENT = "orion-ld";

	private final EntityRepository repository;
	private final GeneralProperties generalProperties;
	private final Environment environment;

	public NgsiLdEntityRepositoryContractIT(EntityRepository repository, GeneralProperties generalProperties,
			Environment environment) {
		this.repository = repository;
		this.generalProperties = generalProperties;
		this.environment = environment;
	}

	// Orion-LD in append mode keeps the datasetId instances of an emptied list, see updateDomainEntity
	@Override
	protected boolean clearsAnEmptiedList() {
		return generalProperties.isReplaceOnUpdate() || !environment.getActiveNames().contains(ORION_LD_ENVIRONMENT);
	}

	@Override
	protected EntityRepository repository() {
		return repository;
	}

	@Override
	protected UpdateFixture<Catalog> fixture() {
		return new CatalogFixture();
	}

	@Override
	protected EntityFixture<Category> otherFixture() {
		return new CategoryFixture();
	}
}
