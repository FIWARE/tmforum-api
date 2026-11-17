package org.fiware.tmforum.productcatalog.storage;

import io.micronaut.context.env.Environment;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.exception.QueryException;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.fiware.tmforum.common.test.BrokerOnly;
import org.fiware.tmforum.product.Category;
import org.fiware.tmforum.productcatalog.domain.Catalog;
import org.fiware.tmforum.storage.EntityRepository;
import org.fiware.tmforum.storage.contract.AbstractEntityRepositoryContractTest;
import org.fiware.tmforum.storage.contract.EntityFixture;
import org.fiware.tmforum.storage.contract.KnownDeviation;
import org.fiware.tmforum.storage.contract.QueryFixture;
import org.fiware.tmforum.storage.contract.ReferenceFixture;
import org.fiware.tmforum.storage.contract.UpdateFixture;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Runs the entity port contract against the NGSI-LD adapter and the broker of the IT profile.
 */
@BrokerOnly
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

	// the NGSI-LD defects recorded on the port Javadoc; the broker is chosen by the IT environment
	@Override
	protected Set<KnownDeviation> knownDeviations() {
		Set<KnownDeviation> deviations = EnumSet.of(KnownDeviation.EMPTY_PAGE_COMPLETES_EMPTY,
				KnownDeviation.EXTENSION_OVERRIDES_DECLARED_PROPERTY, KnownDeviation.SORT_IS_IGNORED,
				KnownDeviation.REGEX_HOLDING_QUERY_SYNTAX_IS_REJECTED);
		if (!environment.getActiveNames().contains(ORION_LD_ENVIRONMENT)) {
			deviations.add(KnownDeviation.SEPARATOR_IN_LITERAL_NEVER_MATCHES);
			deviations.add(KnownDeviation.ANCHORED_REGEX_NEVER_MATCHES);
			deviations.add(KnownDeviation.STRING_COMPARISON_IS_LINGUISTIC);
			deviations.add(KnownDeviation.NUMBER_AND_BOOLEAN_FILTER_FAILS);
			return deviations;
		}
		deviations.add(KnownDeviation.SEVERAL_IDS_WITH_SEVERAL_TYPES_FAIL);
		deviations.add(KnownDeviation.MULTI_ELEMENT_LIST_NEVER_MATCHES);
		if (!generalProperties.isReplaceOnUpdate()) {
			deviations.add(KnownDeviation.EMPTIED_LIST_BREAKS_READS);
			deviations.add(KnownDeviation.REFERENCE_LIST_UPDATE_MERGES);
			deviations.add(KnownDeviation.EMPTIED_EXTENSION_LIST_READS_BACK_EXPANDED);
		}
		return deviations;
	}

	@Override
	protected boolean isClientQueryError(Throwable failure) {
		return failure instanceof QueryException;
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

	@Override
	protected List<QueryFixture<?>> queryFixtures() {
		return List.of(new ProductOfferingPriceQueryFixture(), new SubscriptionQueryFixture());
	}

	@Override
	protected Optional<ReferenceFixture<?>> referenceFixture() {
		return Optional.of(new ProductOfferingReferenceFixture());
	}
}
