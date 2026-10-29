package org.fiware.tmforum.productcatalog.storage;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.fiware.tmforum.common.test.FirstMatchConfigContextBuilder;
import org.fiware.tmforum.productcatalog.domain.Catalog;
import org.fiware.tmforum.storage.EntityWriteRepository;
import org.fiware.tmforum.storage.ReferenceReadRepository;
import org.fiware.tmforum.storage.contract.AbstractReferenceReadRepositoryContractTest;
import org.fiware.tmforum.storage.contract.EntityFixture;

/**
 * Runs the reference port contract against the NGSI-LD adapter and the broker of the IT profile.
 */
@MicronautTest(packages = {"org.fiware.tmforum.productcatalog"}, contextBuilder = FirstMatchConfigContextBuilder.class)
public class NgsiLdReferenceReadRepositoryContractIT extends AbstractReferenceReadRepositoryContractTest<Catalog> {

	private final ReferenceReadRepository repository;
	private final EntityWriteRepository writeRepository;

	public NgsiLdReferenceReadRepositoryContractIT(ReferenceReadRepository repository,
			EntityWriteRepository writeRepository) {
		this.repository = repository;
		this.writeRepository = writeRepository;
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
