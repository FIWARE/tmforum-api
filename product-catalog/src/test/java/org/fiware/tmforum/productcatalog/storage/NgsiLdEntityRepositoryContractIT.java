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
import org.fiware.tmforum.storage.contract.KnownDeviation;
import org.fiware.tmforum.storage.contract.UpdateFixture;

import java.util.EnumSet;
import java.util.Set;

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

	// the Orion-LD defects recorded on the port Javadoc; the broker is chosen by the IT environment
	@Override
	protected Set<KnownDeviation> knownDeviations() {
		if (!environment.getActiveNames().contains(ORION_LD_ENVIRONMENT)) {
			return Set.of();
		}
		Set<KnownDeviation> deviations = EnumSet.of(KnownDeviation.SEVERAL_IDS_WITH_SEVERAL_TYPES_FAIL);
		if (!generalProperties.isReplaceOnUpdate()) {
			deviations.add(KnownDeviation.EMPTIED_LIST_BREAKS_READS);
		}
		return deviations;
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
