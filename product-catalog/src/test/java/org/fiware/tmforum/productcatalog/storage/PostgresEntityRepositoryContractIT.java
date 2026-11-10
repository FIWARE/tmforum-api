package org.fiware.tmforum.productcatalog.storage;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.fiware.tmforum.product.Category;
import org.fiware.tmforum.productcatalog.domain.Catalog;
import org.fiware.tmforum.storage.EntityRepository;
import org.fiware.tmforum.storage.contract.AbstractEntityRepositoryContractTest;
import org.fiware.tmforum.storage.contract.EntityFixture;
import org.fiware.tmforum.storage.contract.ReferenceFixture;
import org.fiware.tmforum.storage.contract.UpdateFixture;
import org.fiware.tmforum.storage.postgresql.PostgresEntityRepository;
import org.fiware.tmforum.storage.postgresql.PostgresStorage;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Runs the entity port contract against the PostgreSQL adapter and the k3s PostgreSQL, without known deviations.
 */
@MicronautTest(packages = {"org.fiware.tmforum.productcatalog"}, environments = PostgresStorage.ENVIRONMENT,
		contextBuilder = FirstMatchConfigContextBuilder.class)
public class PostgresEntityRepositoryContractIT extends AbstractEntityRepositoryContractTest<Catalog, Category> {

	private final EntityRepository repository;

	public PostgresEntityRepositoryContractIT(EntityRepository repository) {
		this.repository = repository;
	}

	// without the environment the NGSI-LD adapter would silently pass as the one under test
	@Test
	void repositoryUnderTestIsThePostgresAdapter() {
		assertInstanceOf(PostgresEntityRepository.class, repository);
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
	protected Optional<ReferenceFixture<?>> referenceFixture() {
		return Optional.of(new ProductOfferingReferenceFixture());
	}
}
