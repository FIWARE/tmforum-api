package org.fiware.tmforum.productcatalog.storage;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.fiware.tmforum.productcatalog.domain.Catalog;
import org.fiware.tmforum.storage.EntityWriteRepository;
import org.fiware.tmforum.storage.ReferenceReadRepository;
import org.fiware.tmforum.storage.contract.AbstractReferenceReadRepositoryContractTest;
import org.fiware.tmforum.storage.contract.EntityFixture;
import org.fiware.tmforum.storage.postgresql.PostgresReferencesRepository;
import org.fiware.tmforum.storage.postgresql.PostgresStorage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Runs the reference port contract against the PostgreSQL adapter and the k3s PostgreSQL.
 */
@MicronautTest(packages = {"org.fiware.tmforum.productcatalog"}, environments = PostgresStorage.ENVIRONMENT,
		contextBuilder = FirstMatchConfigContextBuilder.class)
public class PostgresReferenceReadRepositoryContractIT extends AbstractReferenceReadRepositoryContractTest<Catalog> {

	private final ReferenceReadRepository repository;
	private final EntityWriteRepository writeRepository;

	public PostgresReferenceReadRepositoryContractIT(ReferenceReadRepository repository,
			EntityWriteRepository writeRepository) {
		this.repository = repository;
		this.writeRepository = writeRepository;
	}

	// without the environment the NGSI-LD adapter would silently pass as the one under test
	@Test
	void repositoryUnderTestIsThePostgresAdapter() {
		assertInstanceOf(PostgresReferencesRepository.class, repository);
	}

	@Override
	protected ReferenceReadRepository repository() {
		return repository;
	}

	@Override
	protected EntityWriteRepository writeRepository() {
		return writeRepository;
	}

	@Override
	protected EntityFixture<Catalog> fixture() {
		return new CatalogFixture();
	}
}
